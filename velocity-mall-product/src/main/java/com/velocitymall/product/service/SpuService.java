package com.velocitymall.product.service;

import com.velocitymall.product.model.vo.SpuDetailVO;

/**
 * 商品 SPU 服务。
 */
public interface SpuService {

    /**
     * 查询 SPU 详情。
     *
     * @param spuId SPU ID
     * @return SPU 详情
     */
    SpuDetailVO getSpuDetail(Long spuId);

    /**
     * Publish a SPU and synchronize all related SKU documents after commit.
     *
     * @param spuId SPU ID
     */
    void publishSpu(Long spuId);

    /**
     * Unpublish a SPU and remove all related SKU documents after commit.
     *
     * @param spuId SPU ID
     */
    void unpublishSpu(Long spuId);
}
