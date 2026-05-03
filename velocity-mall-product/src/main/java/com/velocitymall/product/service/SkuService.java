package com.velocitymall.product.service;

import com.velocitymall.product.model.dto.LockStockDTO;

/**
 * 商品 SKU 服务。
 */
public interface SkuService {

    /**
     * 锁定库存。
     *
     * @param dto 锁定库存参数
     */
    void lockStock(LockStockDTO dto);
}
