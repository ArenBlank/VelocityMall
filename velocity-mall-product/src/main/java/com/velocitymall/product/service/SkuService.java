package com.velocitymall.product.service;

import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.dto.UnlockStockDTO;
import com.velocitymall.product.model.vo.SkuVO;

/**
 * 商品 SKU 服务。
 */
public interface SkuService {

    /**
     * 查询 SKU 详情。
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    SkuVO getSkuById(Long skuId);

    /**
     * 锁定库存。
     *
     * @param dto 锁定库存参数
     */
    void lockStock(LockStockDTO dto);

    /**
     * 释放锁定库存。
     *
     * @param dto 释放锁定库存参数
     */
    void unlockStock(UnlockStockDTO dto);
}
