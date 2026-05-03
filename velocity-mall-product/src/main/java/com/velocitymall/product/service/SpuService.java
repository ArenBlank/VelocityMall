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
}
