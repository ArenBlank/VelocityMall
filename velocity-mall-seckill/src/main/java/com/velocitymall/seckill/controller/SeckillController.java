package com.velocitymall.seckill.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.seckill.model.vo.SeckillActivityVO;
import com.velocitymall.seckill.service.SeckillActivityService;
import com.velocitymall.seckill.service.SeckillService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
