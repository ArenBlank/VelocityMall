package com.velocitymall.product.service.impl;

import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.mapper.SkuMapper;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品 SKU 服务实现。
 */
@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final SkuMapper skuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(LockStockDTO dto) {
        int affectedRows = skuMapper.lockStock(dto.getSkuId(), dto.getQuantity());
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "商品库存不足，锁定库存失败");
        }
    }
}
