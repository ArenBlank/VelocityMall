package com.velocitymall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.OrderItemDTO;
import com.velocitymall.common.model.dto.OrderRefundDTO;
import com.velocitymall.common.model.dto.PaymentSuccessDTO;
import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.common.model.dto.ProductSyncDTO;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.constant.ProductCacheConstant;
import com.velocitymall.product.entity.MqConsumeLog;
import com.velocitymall.product.entity.Sku;
import com.velocitymall.product.entity.StockLockLog;
import com.velocitymall.product.mapper.MqConsumeLogMapper;
import com.velocitymall.product.mapper.SkuMapper;
import com.velocitymall.product.mapper.StockLockLogMapper;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.dto.UnlockStockDTO;
import com.velocitymall.product.model.dto.UpdateSkuDTO;
import com.velocitymall.product.model.vo.SkuVO;
import com.velocitymall.product.service.SkuService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SKU service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";

    private static final String PAYMENT_SUCCESS_CONSUMER_GROUP = "payment-success-consumer-group";

    private static final String ORDER_REFUND_TOPIC = "order-refund-topic";

    private static final String ORDER_REFUND_CONSUMER_GROUP = "order-refund-consumer-group";

    private static final String PRODUCT_SYNC_TOPIC = "product-sync-topic";

    private static final int PRODUCT_STATUS_PUBLISHED = 1;

    private static final int ORDER_TYPE_NORMAL = 0;

    private static final int ORDER_TYPE_SECKILL = 1;

    private static final int STOCK_LOCK_STATUS_LOCKED = 0;

    private final SkuMapper skuMapper;

    private final MqConsumeLogMapper mqConsumeLogMapper;

    private final StockLockLogMapper stockLockLogMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public SkuVO getSkuById(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU不存在或已下架");
        }
        return convertSkuVO(sku);
    }

    @Override
    public ProductSkuSearchDTO getSkuSearchSource(Long skuId) {
        ProductSkuSearchDTO source = skuMapper.selectSearchSourceBySkuId(skuId);
        if (source == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU不存在或已删除");
        }
        return source;
    }

    @Override
    public PageVO<ProductSkuSearchDTO> listPublishedSearchSources(Long page, Long size) {
        validateSourcePage(page, size);
        Long total = skuMapper.countPublishedSearchSources();
        long safeTotal = total == null ? 0L : total;
        long pages = safeTotal == 0 ? 0 : (safeTotal + size - 1) / size;
        long offset = (page - 1) * size;
        List<ProductSkuSearchDTO> records = safeTotal == 0
                ? List.of()
                : skuMapper.selectPublishedSearchSources(offset, size);
        return new PageVO<>(page, size, safeTotal, pages, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuBasicInfo(Long skuId, UpdateSkuDTO dto) {
        validateUpdateSkuRequest(skuId, dto);
        int affectedRows = skuMapper.update(null, Wrappers.<Sku>lambdaUpdate()
                .eq(Sku::getId, skuId)
                .eq(Sku::getIsDeleted, 0)
                .set(StringUtils.hasText(dto.getSkuName()), Sku::getSkuName, dto.getSkuName())
                .set(dto.getPrice() != null, Sku::getPrice, dto.getPrice())
                .set(dto.getCoverImg() != null, Sku::getCoverImg, dto.getCoverImg()));
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU不存在或已删除");
        }
        registerSkuSearchSyncAfterCommit(skuId);
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
                log.warn("Stock unlock affected zero rows, maybe already unlocked or data abnormal. orderSn: {}, skuId: {}",
                        dto.getOrderSn(), dto.getSkuId());
                updateSucceeded = true;
                return;
            }
            updateSucceeded = true;
        } catch (Exception exception) {
            if (!updateSucceeded) {
                try {
                    redisTemplate.delete(unlockKey);
                } catch (RuntimeException deleteException) {
                    log.warn("Delete stock unlock idempotency key failed. unlockKey: {}", unlockKey, deleteException);
                }
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "释放库存失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockPhysicalStock(StockLockDTO dto) {
        validateStockLockRequest(dto);
        List<OrderItemDTO> sortedItems = normalizeAndSortItems(dto.getItems());
        for (OrderItemDTO item : sortedItems) {
            int affectedRows = skuMapper.lockPhysicalStock(item.getSkuId(), item.getQuantity());
            if (affectedRows == 0) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "商品库存不足，锁定失败");
            }
            stockLockLogMapper.insert(StockLockLog.builder()
                    .orderSn(dto.getOrderSn())
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .status(STOCK_LOCK_STATUS_LOCKED)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockPhysicalStock(StockLockDTO dto) {
        validateStockLockRequest(dto);
        List<OrderItemDTO> sortedItems = normalizeAndSortItems(dto.getItems());
        for (OrderItemDTO item : sortedItems) {
            int markedRows = stockLockLogMapper.markReleased(dto.getOrderSn(), item.getSkuId());
            if (markedRows == 0) {
                log.info("Stock lock log is absent or already finished, skip unlock. orderSn: {}, skuId: {}",
                        dto.getOrderSn(), item.getSkuId());
                continue;
            }
            int affectedRows = skuMapper.releasePhysicalLockedStock(item.getSkuId(), item.getQuantity());
            if (affectedRows == 0) {
                log.error("Locked stock release failed after lock log marked. orderSn: {}, skuId: {}, quantity: {}",
                        dto.getOrderSn(), item.getSkuId(), item.getQuantity());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "释放锁定库存失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductPhysicalStock(PaymentSuccessDTO dto) {
        validatePaymentSuccessEvent(dto);
        try {
            mqConsumeLogMapper.insert(MqConsumeLog.builder()
                    .topic(PAYMENT_SUCCESS_TOPIC)
                    .consumerGroup(PAYMENT_SUCCESS_CONSUMER_GROUP)
                    .orderSn(dto.getOrderSn())
                    .build());
        } catch (DuplicateKeyException exception) {
            log.info("Payment success message already consumed. orderSn: {}", dto.getOrderSn());
            return true;
        }

        List<OrderItemDTO> sortedItems = normalizeAndSortItems(dto.getItems());
        if (ORDER_TYPE_NORMAL == dto.getOrderType()) {
            deductNormalOrderStock(dto.getOrderSn(), sortedItems);
            return true;
        }
        if (ORDER_TYPE_SECKILL == dto.getOrderType()) {
            deductSeckillOrderStock(dto.getOrderSn(), sortedItems);
            return true;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "订单类型非法");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundPhysicalStock(OrderRefundDTO dto) {
        validateOrderRefundEvent(dto);
        assertPaymentSuccessConsumed(dto.getOrderSn());
        try {
            mqConsumeLogMapper.insert(MqConsumeLog.builder()
                    .topic(ORDER_REFUND_TOPIC)
                    .consumerGroup(ORDER_REFUND_CONSUMER_GROUP)
                    .orderSn(dto.getOrderSn())
                    .build());
        } catch (DuplicateKeyException exception) {
            log.info("Order refund message already consumed. orderSn: {}", dto.getOrderSn());
            return true;
        }

        List<OrderItemDTO> sortedItems = normalizeAndSortItems(dto.getItems());
        for (OrderItemDTO item : sortedItems) {
            int affectedRows = skuMapper.refundPhysicalStock(item.getSkuId(), item.getQuantity());
            if (affectedRows == 0) {
                log.error("Order refund stock rollback affected zero rows. orderSn: {}, skuId: {}, quantity: {}",
                        dto.getOrderSn(), item.getSkuId(), item.getQuantity());
            }
        }
        return true;
    }

    private void deductNormalOrderStock(String orderSn, List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            int markedRows = stockLockLogMapper.markDeducted(orderSn, item.getSkuId());
            if (markedRows == 0) {
                log.error("普通订单库存锁流水不可扣减. orderSn: {}, skuId: {}", orderSn, item.getSkuId());
                continue;
            }
            int affectedRows = skuMapper.deductNormalPhysicalStock(item.getSkuId(), item.getQuantity());
            if (affectedRows == 0) {
                log.error("订单物理库存扣减失败，库存不足. orderSn: {}, skuId: {}, quantity: {}",
                        orderSn, item.getSkuId(), item.getQuantity());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "订单物理库存扣减失败");
            }
        }
    }

    private void deductSeckillOrderStock(String orderSn, List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            int affectedRows = skuMapper.deductSeckillPhysicalStock(item.getSkuId(), item.getQuantity());
            if (affectedRows == 0) {
                log.error("订单物理库存扣减失败，库存不足. orderSn: {}, skuId: {}, quantity: {}",
                        orderSn, item.getSkuId(), item.getQuantity());
            }
        }
    }

    private void validatePaymentSuccessEvent(PaymentSuccessDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付成功消息不能为空");
        }
        if (!StringUtils.hasText(dto.getOrderSn())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (dto.getUserId() == null || dto.getUserId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户 ID 非法");
        }
        if (dto.getOrderType() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单类型不能为空");
        }
        if (CollectionUtils.isEmpty(dto.getItems())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付成功消息明细不能为空");
        }
    }

    private void validateOrderRefundEvent(OrderRefundDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款消息不能为空");
        }
        if (!StringUtils.hasText(dto.getOrderSn())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (dto.getOrderType() == null || (ORDER_TYPE_NORMAL != dto.getOrderType() && ORDER_TYPE_SECKILL != dto.getOrderType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单类型非法");
        }
        if (CollectionUtils.isEmpty(dto.getItems())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款消息明细不能为空");
        }
    }

    private void assertPaymentSuccessConsumed(String orderSn) {
        Long consumedCount = mqConsumeLogMapper.selectCount(new LambdaQueryWrapper<MqConsumeLog>()
                .eq(MqConsumeLog::getTopic, PAYMENT_SUCCESS_TOPIC)
                .eq(MqConsumeLog::getConsumerGroup, PAYMENT_SUCCESS_CONSUMER_GROUP)
                .eq(MqConsumeLog::getOrderSn, orderSn));
        if (consumedCount == null || consumedCount == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "支付扣库存尚未完成，退款消息稍后重试");
        }
    }

    private void validateStockLockRequest(StockLockDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存锁定请求不能为空");
        }
        if (!StringUtils.hasText(dto.getOrderSn())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (CollectionUtils.isEmpty(dto.getItems())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存锁定明细不能为空");
        }
    }

    private List<OrderItemDTO> normalizeAndSortItems(List<OrderItemDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品明细不能为空");
        }
        Map<Long, Integer> quantityMap = items.stream()
                .peek(this::validateOrderItem)
                .collect(Collectors.toMap(
                        OrderItemDTO::getSkuId,
                        OrderItemDTO::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        return quantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> new OrderItemDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void validateOrderItem(OrderItemDTO item) {
        if (item == null || item.getSkuId() == null || item.getSkuId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU 非法");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品数量非法");
        }
    }

    private void validateSourcePage(Long page, Long size) {
        if (page == null || page <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于0");
        }
        if (size == null || size <= 0 || size > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页数量必须在1到500之间");
        }
    }

    private void validateUpdateSkuRequest(Long skuId, UpdateSkuDTO dto) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU非法");
        }
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU更新参数不能为空");
        }
        boolean hasUpdateField = StringUtils.hasText(dto.getSkuName())
                || dto.getPrice() != null
                || dto.getCoverImg() != null;
        if (!hasUpdateField) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "至少需要传入一个SKU更新字段");
        }
    }

    private void registerSkuSearchSyncAfterCommit(Long skuId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ProductSkuSearchDTO source = skuMapper.selectSearchSourceBySkuId(skuId);
                    int action = source != null && Integer.valueOf(PRODUCT_STATUS_PUBLISHED).equals(source.getStatus())
                            ? ProductSyncDTO.ACTION_UPSERT
                            : ProductSyncDTO.ACTION_DELETE;
                    sendProductSyncMessage(skuId, action);
                } catch (Exception exception) {
                    log.error("Register SKU search sync failed after commit. skuId: {}", skuId, exception);
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
            log.info("Product search sync message sent. skuId: {}, action: {}", skuId, action);
        } catch (Exception exception) {
            log.error("Product search sync message send failed. skuId: {}, action: {}", skuId, action, exception);
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
