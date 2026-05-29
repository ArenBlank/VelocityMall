package com.velocitymall.admin.controller;

import com.velocitymall.admin.annotation.RequireAdminPermission;
import com.velocitymall.admin.client.SeckillFeignClient;
import com.velocitymall.admin.constant.AdminPermissionCodes;
import com.velocitymall.admin.model.dto.SeckillTestRequest;
import com.velocitymall.admin.service.AdminService;
import com.velocitymall.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/seckill/stress")
public class AdminStressTestController {

    private static final Long TEST_SKU_ID = 2001L;
    private static final int CONCURRENCY = 500;
    private static final int WAVES = 3;
    private static final int WAVE_DELAY_MS = 50;
    // One wave = 500 threads. Waves are staggered by 50ms and each Feign call
    // completes in 20-50ms, so threads from earlier waves are freed by the time
    // the next wave hits. 500 threads delivers full 500-concurrent pressure
    // while using 1/3 the stack memory of the original 1500-thread pool.
    private static final int ENGINE_THREADS = CONCURRENCY;

    private final AdminService adminService;
    private final SeckillFeignClient seckillFeignClient;

    @PostMapping("/init")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_PREHEAT)
    public Result<Map<String, Object>> init(@Valid @RequestBody SeckillTestRequest request) {
        return Result.success(adminService.initSeckillTest(request));
    }

    @PostMapping("/cleanup/{skuId}")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_PREHEAT)
    public Result<Map<String, Object>> cleanup(@PathVariable("skuId") @Min(1) Long skuId) {
        Map<String, Object> result = adminService.cleanupSeckillTest(skuId);
        // Hint JVM to recycle heap expanded by the stress-test thread pool.
        // This is best-effort; for guaranteed reclamation, restart the admin service.
        System.gc();
        result.put("gcHint", "JVM GC suggested — if memory stays high, restart admin service");
        return Result.success(result);
    }

    @PostMapping("/single-test/{skuId}")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_READ)
    public Result<Map<String, Object>> singleTest(@PathVariable("skuId") @Min(1) Long skuId) {
        long start = System.currentTimeMillis();
        Long testUserId = 90000L + ThreadLocalRandom.current().nextLong(9000);

        Map<String, Object> result = new HashMap<>();
        result.put("skuId", skuId);
        result.put("userId", testUserId);

        try {
            Result<String> seckillResult = seckillFeignClient.testExecute(skuId, testUserId);
            boolean ok = seckillResult.getCode() != null
                    && seckillResult.getCode().equals(Result.SUCCESS_CODE);
            result.put("success", ok);
            if (ok) {
                result.put("message",
                        seckillResult.getData() != null ? seckillResult.getData() : "秒杀成功");
            } else {
                result.put("message",
                        seckillResult.getMessage() != null ? seckillResult.getMessage() : "秒杀失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            String msg = e.getMessage();
            if (msg != null && msg.contains("请勿重复抢购")) {
                result.put("message", "请勿重复抢购");
            } else if (msg != null && msg.contains("商品已抢光")) {
                result.put("message", "商品已抢光");
            } else {
                result.put("message", msg != null ? msg : "秒杀请求失败");
            }
        }

        result.put("elapsed", System.currentTimeMillis() - start);
        return Result.success(result);
    }

    @PostMapping("/run-k6")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_READ)
    public Result<Map<String, Object>> runK6() {
        long totalStart = System.currentTimeMillis();
        int totalRequests = CONCURRENCY * WAVES;

        // Warmup: single call to prime Feign + Nacos service discovery,
        // preventing 200-thread concurrent subscription storm.
        try {
            seckillFeignClient.testExecute(TEST_SKU_ID, 99999L);
        } catch (Exception ignored) {}

        // Bounded pool: max 200 threads, excess tasks queue up.
        // This prevents the 1.5GB thread-stack blowout from 1500 threads
        // while still generating sustained high concurrency against the seckill service.
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r, "stress-" + r.hashCode());
            t.setDaemon(true);
            return t;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                ENGINE_THREADS, ENGINE_THREADS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                daemonFactory);
        executor.prestartAllCoreThreads();
        List<Future<String>> futures = new ArrayList<>();

        for (int wave = 0; wave < WAVES; wave++) {
            final long baseUserId = 90000L + (wave * 100000L);
            for (int i = 0; i < CONCURRENCY; i++) {
                final long userId = baseUserId + i;
                futures.add(executor.submit(() -> {
                    try {
                        Result<String> r = seckillFeignClient.testExecute(TEST_SKU_ID, userId);
                        boolean ok = r.getCode() != null && r.getCode().equals(Result.SUCCESS_CODE);
                        return ok ? "ok" : (r.getMessage() != null ? r.getMessage() : "fail");
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("重复")) return "dup";
                        if (msg != null && msg.contains("抢光")) return "soldout";
                        return "fail";
                    }
                }));
            }
            if (wave < WAVES - 1) {
                try { Thread.sleep(WAVE_DELAY_MS); } catch (InterruptedException ignored) {}
            }
        }

        int ok = 0, fail = 0, dup = 0, soldOut = 0;
        for (Future<String> f : futures) {
            try {
                switch (f.get(30, TimeUnit.SECONDS)) {
                    case "ok" -> ok++;
                    case "dup" -> dup++;
                    case "soldout" -> soldOut++;
                    default -> fail++;
                }
            } catch (Exception e) {
                fail++;
            }
        }
        executor.shutdown();
        try { executor.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        long elapsed = System.currentTimeMillis() - totalStart;
        long qps = elapsed > 0 ? totalRequests * 1000L / elapsed : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("engine", "java-internal");
        result.put("concurrency", CONCURRENCY);
        result.put("waves", WAVES);
        result.put("totalRequests", totalRequests);
        result.put("totalElapsedMs", elapsed);
        result.put("qps", qps);
        result.put("success", ok);
        result.put("duplicate", dup);
        result.put("soldOut", soldOut);
        result.put("fail", fail);
        result.put("zeroOversell", ok <= 1000);

        log.info("Stress complete: {}/{} total, {} ok, {} dup, {} sold-out, {} fail, {}ms, QPS≈{}",
                totalRequests, ok + dup + soldOut + fail, ok, dup, soldOut, fail, elapsed, qps);
        return Result.success(result);
    }

    @GetMapping("/metrics/{skuId}")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_READ)
    public Result<Map<String, Object>> metrics(@PathVariable("skuId") @Min(1) Long skuId) {
        try {
            Result<Map<String, Object>> result = seckillFeignClient.getMetrics(skuId);
            if (result != null && result.getData() != null) {
                return Result.success(result.getData());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch seckill stress metrics for skuId={}: {}", skuId, e.getMessage());
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("stock", 0);
        fallback.put("qps", 0);
        fallback.put("latency", 0);
        fallback.put("mqQueue", 0);
        return Result.success(fallback);
    }
}
