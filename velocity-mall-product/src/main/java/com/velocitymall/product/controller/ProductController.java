package com.velocitymall.product.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.vo.SpuDetailVO;
import com.velocitymall.product.service.SkuService;
import com.velocitymall.product.service.SpuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品展示与库存内部接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final SpuService spuService;

    private final SkuService skuService;

    /**
     * 查询 SPU 详情。
     *
     * @param spuId SPU ID
     * @return SPU 详情
     */
    @GetMapping("/spus/{spu-id}")
    public Result<SpuDetailVO> getSpuDetail(
            @PathVariable("spu-id")
            @NotNull(message = "SPU ID不能为空")
            @Min(value = 1, message = "SPU ID必须大于0") Long spuId) {
        return Result.success(spuService.getSpuDetail(spuId));
    }

    /**
     * 锁定 SKU 库存。
     *
     * @param dto 锁定库存参数
     * @return 处理结果
     */
    @PutMapping("/skus/lock-stock")
    public Result<Void> lockStock(@Valid @RequestBody LockStockDTO dto) {
        skuService.lockStock(dto);
        return Result.success();
    }
}
