package com.velocitymall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.ProductSyncDTO;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.constant.ProductCacheConstant;
import com.velocitymall.product.entity.Sku;
import com.velocitymall.product.entity.Spu;
import com.velocitymall.product.mapper.SkuMapper;
import com.velocitymall.product.mapper.SpuMapper;
import com.velocitymall.product.model.vo.SkuVO;
import com.velocitymall.product.model.vo.SpuDetailVO;
import com.velocitymall.product.service.SpuService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 商品 SPU 服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {

    private static final String PRODUCT_SYNC_TOPIC = "product-sync-topic";

    private static final int PUBLISH_STATUS_OFF = 0;

    private static final int PUBLISH_STATUS_ON = 1;

    private final SpuMapper spuMapper;

    private final SkuMapper skuMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public SpuDetailVO getSpuDetail(Long spuId) {
        String cacheKey = ProductCacheConstant.spuDetailKey(spuId);
        SpuDetailVO cachedSpuDetail = getCachedSpuDetail(cacheKey);
        if (cachedSpuDetail != null) {
            return cachedSpuDetail;
        }

        RLock lock = redissonClient.getLock(ProductCacheConstant.spuDetailLockKey(spuId));
        boolean locked = false;
        try {
            locked = lock.tryLock(
                    ProductCacheConstant.SPU_DETAIL_LOCK_WAIT_SECONDS,
                    ProductCacheConstant.SPU_DETAIL_LOCK_LEASE_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!locked) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "系统繁忙，请稍后重试");
            }

            cachedSpuDetail = getCachedSpuDetail(cacheKey);
            if (cachedSpuDetail != null) {
                return cachedSpuDetail;
            }

            SpuDetailVO spuDetail = querySpuDetailFromDatabase(spuId);
            if (spuDetail == null) {
                cacheEmptySpuDetail(cacheKey);
                throw new BusinessException(ResultCode.BIZ_WARNING, "商品不存在或已下架");
            }

            cacheSpuDetail(cacheKey, spuDetail);
            return spuDetail;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询商品详情被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishSpu(Long spuId) {
        updatePublishStatus(spuId, PUBLISH_STATUS_ON, ProductSyncDTO.ACTION_UPSERT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishSpu(Long spuId) {
        updatePublishStatus(spuId, PUBLISH_STATUS_OFF, ProductSyncDTO.ACTION_DELETE);
    }

    private void updatePublishStatus(Long spuId, Integer publishStatus, Integer syncAction) {
        if (spuId == null || spuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SPU ID非法");
        }
        int affectedRows = spuMapper.updatePublishStatus(spuId, publishStatus);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SPU不存在或已删除");
        }

        redisTemplate.delete(ProductCacheConstant.spuDetailKey(spuId));
        List<Long> skuIds = skuMapper.selectSkuIdsBySpuId(spuId);
        registerSpuSearchSyncAfterCommit(skuIds, syncAction);
    }

    private void registerSpuSearchSyncAfterCommit(List<Long> skuIds, Integer syncAction) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long skuId : skuIds) {
                    sendProductSyncMessage(skuId, syncAction);
                }
            }
        });
    }

    private void sendProductSyncMessage(Long skuId, Integer action) {
        ProductSyncDTO messageDTO = MqTraceContext.prepare(
                new ProductSyncDTO(skuId, action),
                String.valueOf(skuId)
        );
        try {
            rocketMQTemplate.syncSendOrderly(
                    PRODUCT_SYNC_TOPIC,
                    MessageBuilder.withPayload(messageDTO).build(),
                    String.valueOf(skuId)
            );
            log.info("Product SPU publish sync message sent. skuId: {}, action: {}", skuId, action);
        } catch (Exception exception) {
            log.error("Product SPU publish sync message send failed. skuId: {}, action: {}", skuId, action, exception);
        }
    }

    private SpuDetailVO getCachedSpuDetail(String cacheKey) {
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue == null) {
            return null;
        }
        if (cachedValue instanceof SpuDetailVO spuDetailVO) {
            checkDummySpu(spuDetailVO);
            return spuDetailVO;
        }

        log.warn("商品详情缓存类型异常，cacheKey={}，actualType={}", cacheKey, cachedValue.getClass().getName());
        redisTemplate.delete(cacheKey);
        return null;
    }

    private void checkDummySpu(SpuDetailVO spuDetailVO) {
        if (ProductCacheConstant.DUMMY_SPU_ID.equals(spuDetailVO.getSpuId())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "商品不存在或已下架");
        }
    }

    private SpuDetailVO querySpuDetailFromDatabase(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            return null;
        }

        List<SkuVO> skuList = skuMapper.selectList(
                        Wrappers.<Sku>lambdaQuery()
                                .eq(Sku::getSpuId, spuId)
                )
                .stream()
                .map(this::convertSkuVO)
                .toList();

        return SpuDetailVO.builder()
                .spuId(spu.getId())
                .categoryId(spu.getCategoryId())
                .name(spu.getName())
                .description(spu.getDescription())
                .publishStatus(spu.getPublishStatus())
                .skuList(skuList)
                .build();
    }

    private void cacheEmptySpuDetail(String cacheKey) {
        SpuDetailVO dummySpuDetail = SpuDetailVO.builder()
                .spuId(ProductCacheConstant.DUMMY_SPU_ID)
                .skuList(Collections.emptyList())
                .build();
        redisTemplate.opsForValue().set(
                cacheKey,
                dummySpuDetail,
                ProductCacheConstant.EMPTY_CACHE_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void cacheSpuDetail(String cacheKey, SpuDetailVO spuDetail) {
        long ttlMinutes = ProductCacheConstant.SPU_DETAIL_CACHE_BASE_TTL_MINUTES
                + ThreadLocalRandom.current().nextLong(1, ProductCacheConstant.SPU_DETAIL_CACHE_RANDOM_MINUTES + 1);
        redisTemplate.opsForValue().set(cacheKey, spuDetail, ttlMinutes, TimeUnit.MINUTES);
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
