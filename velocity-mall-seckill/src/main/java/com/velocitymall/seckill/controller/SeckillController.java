package com.velocitymall.seckill.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.seckill.model.vo.SeckillActivityVO;
import com.velocitymall.seckill.service.SeckillActivityService;
import com.velocitymall.seckill.service.SeckillService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seckill API controller.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    private final SeckillActivityService seckillActivityService;

    @GetMapping("/activities")
    public Result<List<SeckillActivityVO>> listActivities() {
        return Result.success(seckillActivityService.listDisplayActivities());
    }

    @GetMapping("/activities/skus/{skuId}")
    public Result<SeckillActivityVO> getActivityBySkuId(@PathVariable("skuId") Long skuId) {
        return Result.success(seckillActivityService.getDisplayActivityBySkuId(skuId));
    }

    @PostMapping("/execute/{skuId}")
    public Result<String> execute(@PathVariable("skuId") Long skuId) {
        return Result.success(seckillService.execute(skuId));
    }

    @PostMapping("/inner/test-execute/{skuId}")
    public Result<String> testExecute(
            @PathVariable("skuId") @Min(1) Long skuId,
            @RequestParam("userId") @Min(1) Long userId
    ) {
        return Result.success(seckillService.execute(skuId, userId));
    }

    @GetMapping("/inner/stress-metrics/{skuId}")
    public Result<Map<String, Object>> getStressMetrics(@PathVariable("skuId") @Min(1) Long skuId) {
        return Result.success(seckillService.getStressMetrics(skuId));
    }
}
