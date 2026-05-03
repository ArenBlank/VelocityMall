package com.velocitymall.product.service.impl;

import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.constant.ProductCacheConstant;
import com.velocitymall.product.entity.Sku;
import com.velocitymall.product.mapper.SkuMapper;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.dto.UnlockStockDTO;
import com.velocitymall.product.model.vo.SkuVO;
import com.velocitymall.product.service.SkuService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品 SKU 服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final SkuMapper skuMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public SkuVO getSkuById(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU不存在或已下架");
        }
        return convertSkuVO(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(LockStockDTO dto) {
        int affectedRows = skuMapper.lockStock(dto.getSkuId(), dto.getQuantity());
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "商品库存不足，锁定库存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockStock(UnlockStockDTO dto) {
        String unlockKey = ProductCacheConstant.unlockStockKey(dto.getOrderSn(), dto.getSkuId());
        Boolean firstUnlock = redisTemplate.opsForValue().setIfAbsent(
                unlockKey,
                ProductCacheConstant.UNLOCK_STOCK_VALUE,
                ProductCacheConstant.UNLOCK_STOCK_TTL_DAYS,
                TimeUnit.DAYS
        );
        if (!Boolean.TRUE.equals(firstUnlock)) {
            return;
        }

        boolean updateSucceeded = false;
        try {
            int affectedRows = skuMapper.unlockStock(dto.getSkuId(), dto.getQuantity());
            if (affectedRows == 0) {
                log.warn("库存释放影响行数为 0，可能已释放或数据异常，orderSn: {}, skuId: {}", dto.getOrderSn(), dto.getSkuId());
                updateSucceeded = true;
                return;
            }
            updateSucceeded = true;
        } catch (Exception exception) {
            if (!updateSucceeded) {
                try {
                    redisTemplate.delete(unlockKey);
                } catch (RuntimeException deleteException) {
                    log.warn("删除库存释放幂等 Key 失败，unlockKey: {}", unlockKey, deleteException);
                }
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "释放库存失败，请稍后重试");
        }
    }

    private SkuVO convertSkuVO(Sku sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int lockStock = sku.getLockStock() == null ? 0 : sku.getLockStock();
        return SkuVO.builder()
                .skuId(sku.getId())
                .spuId(sku.getSpuId())
                .skuName(sku.getSkuName())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .availableStock(Math.max(stock - lockStock, 0))
                .coverImg(sku.getCoverImg())
                .build();
    }
}
