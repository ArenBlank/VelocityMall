package com.velocitymall.admin.client;

import com.velocitymall.common.result.Result;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "velocity-mall-seckill")
public interface SeckillFeignClient {

    @PostMapping("/api/v1/seckill/inner/test-execute/{skuId}")
    Result<String> testExecute(
            @PathVariable("skuId") Long skuId,
            @RequestParam("userId") Long userId
    );

    @GetMapping("/api/v1/seckill/inner/stress-metrics/{skuId}")
    Result<Map<String, Object>> getMetrics(@PathVariable("skuId") Long skuId);
}
