package com.velocitymall.order.client;

import com.velocitymall.common.result.Result;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.order.model.dto.LockStockDTO;
import com.velocitymall.order.model.dto.UnlockStockDTO;
import com.velocitymall.order.model.vo.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 商品服务远程调用客户端。
 */
@FeignClient(name = "velocity-mall-product")
public interface ProductFeignClient {

    /**
     * 查询 SKU 详情。
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    @GetMapping("/api/v1/products/skus/{sku-id}")
    Result<SkuVO> getSkuById(@PathVariable("sku-id") Long skuId);

    /**
     * 调用商品服务锁定库存。
     *
     * @param dto 锁定库存参数
     * @return 调用结果
     */
    @PutMapping("/api/v1/products/skus/lock-stock")
    Result<Void> lockStock(@RequestBody LockStockDTO dto);

    /**
     * 调用商品服务释放库存。
     *
     * @param dto 释放库存参数
     * @return 调用结果
     */
    @PutMapping("/api/v1/products/skus/unlock-stock")
    Result<Void> unlockStock(@RequestBody UnlockStockDTO dto);

    /**
     * Batch lock physical stock through product internal API.
     *
     * @param dto batch lock request
     * @return call result
     */
    @PutMapping("/api/v1/products/inner/skus/lock-batch")
    Result<Void> lockPhysicalStock(@RequestBody StockLockDTO dto);

    /**
     * Batch unlock physical stock through product internal API.
     *
     * @param dto batch unlock request
     * @return call result
     */
    @PutMapping("/api/v1/products/inner/skus/unlock-batch")
    Result<Void> unlockPhysicalStock(@RequestBody StockLockDTO dto);
}
