package com.velocitymall.seckill.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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

    @PostMapping("/execute/{skuId}")
    public Result<String> execute(@PathVariable("skuId") Long skuId) {
        return Result.success(seckillService.execute(skuId));
    }
}
