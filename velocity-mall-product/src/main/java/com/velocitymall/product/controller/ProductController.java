package com.velocitymall.product.controller;

import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.dto.UnlockStockDTO;
import com.velocitymall.product.model.dto.UpdateSkuDTO;
import com.velocitymall.product.model.vo.SkuVO;
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
import org.springframework.web.bind.annotation.RequestParam;
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
     * 查询 SKU 详情。
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    @GetMapping("/skus/{sku-id}")
    public Result<SkuVO> getSkuById(
            @PathVariable("sku-id")
            @NotNull(message = "SKU ID不能为空")
            @Min(value = 1, message = "SKU ID必须大于0") Long skuId) {
        return Result.success(skuService.getSkuById(skuId));
    }

    /**
     * Internal search source query by SKU ID.
     *
     * @param skuId SKU ID
     * @return SKU search source data
     */
    @GetMapping("/inner/skus/{sku-id}")
    public Result<ProductSkuSearchDTO> getSkuSearchSource(
            @PathVariable("sku-id")
            @NotNull(message = "SKU ID不能为空")
            @Min(value = 1, message = "SKU ID必须大于0") Long skuId) {
        return Result.success(skuService.getSkuSearchSource(skuId));
    }

    /**
     * Internal published SKU search source page query for index rebuild.
     *
     * @param page page number
     * @param size page size
     * @return paged SKU search source data
     */
    @GetMapping("/inner/skus/search-source")
    public Result<PageVO<ProductSkuSearchDTO>> listPublishedSearchSources(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Long page,
            @RequestParam(defaultValue = "500")
            @Min(value = 1, message = "每页数量必须大于0") Long size) {
        return Result.success(skuService.listPublishedSearchSources(page, size));
    }

    /**
     * Internal SKU basic information update.
     *
     * @param skuId SKU ID
     * @param dto update request
     * @return processing result
     */
    @PutMapping("/inner/skus/{sku-id}")
    public Result<Void> updateSkuBasicInfo(
            @PathVariable("sku-id")
            @NotNull(message = "SKU ID不能为空")
            @Min(value = 1, message = "SKU ID必须大于0") Long skuId,
            @Valid @RequestBody UpdateSkuDTO dto) {
        skuService.updateSkuBasicInfo(skuId, dto);
        return Result.success();
    }

    /**
     * Internal SPU publish operation.
     *
     * @param spuId SPU ID
     * @return processing result
     */
    @PutMapping("/inner/spus/{spu-id}/publish")
    public Result<Void> publishSpu(
            @PathVariable("spu-id")
            @NotNull(message = "SPU ID不能为空")
            @Min(value = 1, message = "SPU ID必须大于0") Long spuId) {
        spuService.publishSpu(spuId);
        return Result.success();
    }

    /**
     * Internal SPU unpublish operation.
     *
     * @param spuId SPU ID
     * @return processing result
     */
    @PutMapping("/inner/spus/{spu-id}/unpublish")
    public Result<Void> unpublishSpu(
            @PathVariable("spu-id")
            @NotNull(message = "SPU ID不能为空")
            @Min(value = 1, message = "SPU ID必须大于0") Long spuId) {
        spuService.unpublishSpu(spuId);
        return Result.success();
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

    /**
     * 释放 SKU 锁定库存。
     *
     * @param dto 释放锁定库存参数
     * @return 处理结果
     */
    @PutMapping("/skus/unlock-stock")
    public Result<Void> unlockStock(@Valid @RequestBody UnlockStockDTO dto) {
        skuService.unlockStock(dto);
        return Result.success();
    }

    /**
     * Internal API for batch physical stock locking.
     *
     * @param dto batch stock lock request
     * @return processing result
     */
    @PutMapping("/inner/skus/lock-batch")
    public Result<Void> lockPhysicalStock(@RequestBody StockLockDTO dto) {
        skuService.lockPhysicalStock(dto);
        return Result.success();
    }

    /**
     * Internal API for batch physical stock unlocking.
     *
     * @param dto batch stock unlock request
     * @return processing result
     */
    @PutMapping("/inner/skus/unlock-batch")
    public Result<Void> unlockPhysicalStock(@RequestBody StockLockDTO dto) {
        skuService.unlockPhysicalStock(dto);
        return Result.success();
    }
}
