package com.velocitymall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.NormalOrderDelayDTO;
import com.velocitymall.common.model.dto.OrderItemDTO;
import com.velocitymall.common.model.dto.OrderRefundDTO;
import com.velocitymall.common.model.dto.PaymentSuccessDTO;
import com.velocitymall.common.model.dto.SeckillOrderDTO;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.common.model.vo.OrderDetailVO;
import com.velocitymall.common.model.vo.OrderItemVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.client.UserFeignClient;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.entity.OrderItem;
import com.velocitymall.order.mapper.OrderItemMapper;
import com.velocitymall.order.mapper.OrderMapper;
import com.velocitymall.order.model.dto.OrderMessageDTO;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.dto.UnlockStockDTO;
import com.velocitymall.order.model.vo.OrderVO;
import com.velocitymall.order.model.vo.SkuVO;
import com.velocitymall.order.model.vo.UserAddressVO;
import com.velocitymall.order.service.CartService;
import com.velocitymall.order.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Order service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_STATUS_WAIT_PAY = 0;

    private static final int ORDER_STATUS_PAID = 1;

    private static final int ORDER_STATUS_DELIVERED = 2;

    private static final int ORDER_STATUS_COMPLETED = 3;

    private static final int ORDER_STATUS_CLOSED = 4;

    private static final int ORDER_STATUS_REFUNDED = 5;

    private static final int ORDER_TYPE_NORMAL = 0;

    private static final int ORDER_TYPE_SECKILL = 1;

    private static final String ORDER_DELAY_TOPIC = "velocity-mall-order-delay-topic";

    private static final String NORMAL_ORDER_DELAY_TOPIC = "normal-order-delay-topic";

    private static final String SECKILL_DELAY_TOPIC = "seckill-delay-topic";

    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";

    private static final String ORDER_REFUND_TOPIC = "order-refund-topic";

    private static final long SEND_TIMEOUT_MILLIS = 3000L;

    private static final int DELAY_LEVEL_TEN_SECONDS = 3;

    private static final int DELAY_LEVEL_THIRTY_MINUTES = 16;

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;

    private static final DateTimeFormatter ORDER_SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ProductFeignClient productFeignClient;

    private final UserFeignClient userFeignClient;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final RocketMQTemplate rocketMQTemplate;

    private final CartService cartService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitOrder(SubmitOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        List<Long> checkedSkuIds = normalizeSkuIds(dto.getSkuIds());
        List<OrderItemDTO> checkedItems = cartService.getCheckedItems(currentUserId, checkedSkuIds);
        Map<Long, SkuVO> skuSnapshotMap = getSkuSnapshotMap(checkedItems);
        BigDecimal totalAmount = calculateTotalAmount(checkedItems, skuSnapshotMap);
        String orderSn = generateOrderSn();

        UserAddressVO addressSnapshot = fetchAddressSnapshot(dto.getAddressId(), currentUserId);

        lockPhysicalStockOrCompensate(orderSn, checkedItems);
        try {
            Order order = Order.builder()
                    .orderSn(orderSn)
                    .userId(currentUserId)
                    .totalAmount(totalAmount)
                    .payAmount(totalAmount)
                    .orderType(ORDER_TYPE_NORMAL)
                    .status(ORDER_STATUS_WAIT_PAY)
                    .receiverName(addressSnapshot.getReceiverName())
                    .receiverPhone(addressSnapshot.getReceiverPhone())
                    .receiverProvince(addressSnapshot.getProvince())
                    .receiverCity(addressSnapshot.getCity())
                    .receiverRegion(addressSnapshot.getRegion())
                    .receiverDetailAddress(addressSnapshot.getDetailAddress())
                    .build();
            orderMapper.insert(order);

            for (OrderItemDTO item : checkedItems) {
                SkuVO skuSnapshot = skuSnapshotMap.get(item.getSkuId());
                BigDecimal skuPrice = skuSnapshot.getPrice() == null ? DEFAULT_AMOUNT : skuSnapshot.getPrice();
                OrderItem orderItem = OrderItem.builder()
                        .orderId(order.getId())
                        .orderSn(orderSn)
                        .spuId(skuSnapshot.getSpuId())
                        .skuId(item.getSkuId())
                        .skuName(skuSnapshot.getSkuName())
                        .skuPic(skuSnapshot.getCoverImg())
                        .skuPrice(skuPrice)
                        .skuQuantity(item.getQuantity())
                        .build();
                orderItemMapper.insert(orderItem);
            }

            registerNormalOrderAfterCommit(orderSn, checkedItems, currentUserId, checkedSkuIds);

            return OrderVO.builder()
                    .orderId(order.getId())
                    .orderSn(orderSn)
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .build();
        } catch (Exception exception) {
            compensateUnlockPhysicalStock(orderSn, checkedItems, exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "下单失败，请稍后重试");
        }
    }

    @Override
    public PageVO<OrderDetailVO> listMyOrders(Long page, Long size, Integer status) {
        Long currentUserId = getCurrentUserId();
        Page<Order> orderPage = orderMapper.selectPage(
                Page.of(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, currentUserId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getCreateTime)
        );

        List<Order> orders = orderPage.getRecords();
        if (CollectionUtils.isEmpty(orders)) {
            return new PageVO<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal(),
                    orderPage.getPages(), List.of());
        }

        Map<String, List<OrderItem>> itemMap = listOrderItemsByOrderSn(
                orders.stream().map(Order::getOrderSn).toList()
        );
        List<OrderDetailVO> records = orders.stream()
                .map(order -> toOrderDetailVO(order, itemMap.getOrDefault(order.getOrderSn(), List.of())))
                .toList();
        return new PageVO<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal(),
                orderPage.getPages(), records);
    }

    @Override
    public OrderDetailVO getOrderDetail(String orderSn) {
        Order order = selectCurrentUserOrder(orderSn);
        if (order == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单不存在");
        }
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderSn, orderSn));
        return toOrderDetailVO(order, orderItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderSn) {
        Order order = selectCurrentUserOrder(orderSn);
        if (order == null || !Integer.valueOf(ORDER_STATUS_WAIT_PAY).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单状态异常，无法取消");
        }
        List<OrderItem> orderItems = listOrderItems(orderSn);
        int affectedRows = orderMapper.cancelOrder(orderSn, order.getUserId());
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单状态异常，取消失败");
        }
        registerCancelCompensationMessage(order, orderItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockRefund(String orderSn) {
        Order order = selectCurrentUserOrder(orderSn);
        if (order == null || !Integer.valueOf(ORDER_STATUS_PAID).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单状态异常，无法退款");
        }
        List<OrderItem> orderItems = listOrderItems(orderSn);
        int affectedRows = orderMapper.markRefunded(orderSn, order.getUserId());
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "退款失败，订单状态异常");
        }
        OrderRefundDTO refundDTO = new OrderRefundDTO(
                orderSn,
                order.getOrderType() == null ? ORDER_TYPE_NORMAL : order.getOrderType(),
                toOrderItemDTOList(orderItems)
        );
        MqTraceContext.prepare(refundDTO, orderSn);
        registerRefundMessage(orderSn, refundDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSeckillOrder(SeckillOrderDTO dto) {
        validateSeckillOrderMessage(dto);

        Order existingOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, dto.getOrderSn())
                .last("LIMIT 1"));
        if (existingOrder != null) {
            log.info("Seckill order already exists, skip duplicate message. orderSn: {}", dto.getOrderSn());
            return;
        }

        SkuVO skuSnapshot = getSkuSnapshot(dto.getSkuId());
        BigDecimal skuPrice = skuSnapshot.getPrice() == null ? DEFAULT_AMOUNT : skuSnapshot.getPrice();
        BigDecimal totalAmount = skuPrice.multiply(BigDecimal.valueOf(dto.getQuantity()));

        Order order = Order.builder()
                .orderSn(dto.getOrderSn())
                .userId(dto.getUserId())
                .totalAmount(totalAmount)
                .payAmount(totalAmount)
                .orderType(ORDER_TYPE_SECKILL)
                .status(ORDER_STATUS_WAIT_PAY)
                .remark("Seckill order")
                .build();

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            log.info("Seckill order unique key hit, skip duplicate message. orderSn: {}", dto.getOrderSn());
            return;
        }

        OrderItem orderItem = OrderItem.builder()
                .orderId(order.getId())
                .orderSn(dto.getOrderSn())
                .spuId(skuSnapshot.getSpuId())
                .skuId(dto.getSkuId())
                .skuName(skuSnapshot.getSkuName())
                .skuPic(skuSnapshot.getCoverImg())
                .skuPrice(skuPrice)
                .skuQuantity(dto.getQuantity())
                .build();
        orderItemMapper.insert(orderItem);
        log.info("Seckill order persisted. orderSn: {}, userId: {}", dto.getOrderSn(), dto.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(String orderSn, Integer payType) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (payType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付方式不能为空");
        }

        Long currentUserId = getCurrentUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, orderSn)
                .last("LIMIT 1"));
        if (order == null || !Integer.valueOf(ORDER_STATUS_WAIT_PAY).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单状态异常，无法支付");
        }
        if (!currentUserId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权支付该订单");
        }

        int affectedRows = orderMapper.markPaid(orderSn, payType);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "支付失败，订单状态异常");
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderSn, orderSn));
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "订单明细不存在");
        }

        List<OrderItemDTO> itemDTOList = orderItems.stream()
                .map(item -> new OrderItemDTO(item.getSkuId(), item.getSkuQuantity()))
                .toList();
        int orderType = order.getOrderType() == null ? ORDER_TYPE_NORMAL : order.getOrderType();
        PaymentSuccessDTO paymentSuccessDTO = new PaymentSuccessDTO(orderSn, currentUserId, orderType, itemDTOList);
        MqTraceContext.prepare(paymentSuccessDTO, orderSn);
        registerPaymentSuccessMessage(orderSn, paymentSuccessDTO);
    }

    @Override
    public Boolean checkPurchase(Long userId, String orderSn, Long skuId) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(orderSn) || skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购买校验参数非法");
        }
        Long count = orderMapper.countCompletedSkuOrders(userId, orderSn, skuId);
        return count != null && count > 0;
    }

    private Order selectCurrentUserOrder(String orderSn) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, orderSn)
                .eq(Order::getUserId, getCurrentUserId())
                .last("LIMIT 1"));
    }

    private List<OrderItem> listOrderItems(String orderSn) {
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderSn, orderSn));
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "订单明细不存在");
        }
        return orderItems;
    }

    private Map<String, List<OrderItem>> listOrderItemsByOrderSn(List<String> orderSns) {
        if (CollectionUtils.isEmpty(orderSns)) {
            return Map.of();
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderSn, orderSns))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderSn, LinkedHashMap::new, Collectors.toList()));
    }

    private OrderDetailVO toOrderDetailVO(Order order, List<OrderItem> orderItems) {
        List<OrderItemVO> itemVOList = orderItems.stream()
                .map(item -> new OrderItemVO(
                        item.getSkuId(),
                        item.getSkuName(),
                        item.getSkuPic(),
                        item.getSkuPrice(),
                        item.getSkuQuantity()
                ))
                .toList();
        return new OrderDetailVO(
                order.getOrderSn(),
                order.getTotalAmount(),
                order.getPayType(),
                order.getStatus(),
                order.getOrderType(),
                order.getCreateTime(),
                itemVOList
        );
    }

    private List<OrderItemDTO> toOrderItemDTOList(List<OrderItem> orderItems) {
        return orderItems.stream()
                .filter(Objects::nonNull)
                .map(item -> new OrderItemDTO(item.getSkuId(), item.getSkuQuantity()))
                .toList();
    }

    private void registerCancelCompensationMessage(Order order, List<OrderItem> orderItems) {
        int orderType = order.getOrderType() == null ? ORDER_TYPE_NORMAL : order.getOrderType();
        List<OrderItemDTO> itemDTOList = toOrderItemDTOList(orderItems);
        if (ORDER_TYPE_NORMAL == orderType) {
            registerImmediateNormalUnlockMessage(order.getOrderSn(), itemDTOList);
            return;
        }
        if (ORDER_TYPE_SECKILL == orderType) {
            registerImmediateSeckillRollbackMessage(order, orderItems);
            return;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "订单类型非法");
    }

    private void registerImmediateNormalUnlockMessage(String orderSn, List<OrderItemDTO> itemDTOList) {
        NormalOrderDelayDTO messageDTO = MqTraceContext.prepare(
                new NormalOrderDelayDTO(orderSn, itemDTOList),
                orderSn
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.syncSend(
                            NORMAL_ORDER_DELAY_TOPIC,
                            MessageBuilder.withPayload(messageDTO).build(),
                            SEND_TIMEOUT_MILLIS
                    );
                    log.info("Manual cancel normal order unlock message sent. orderSn: {}", orderSn);
                } catch (Exception exception) {
                    log.error("Manual cancel normal order unlock message send failed. orderSn: {}", orderSn, exception);
                }
            }
        });
    }

    private void registerImmediateSeckillRollbackMessage(Order order, List<OrderItem> orderItems) {
        OrderItem firstItem = orderItems.get(0);
        SeckillOrderDTO messageDTO = new SeckillOrderDTO(
                order.getUserId(),
                firstItem.getSkuId(),
                firstItem.getSkuQuantity(),
                order.getOrderSn()
        );
        MqTraceContext.prepare(messageDTO, order.getOrderSn());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.syncSend(
                            SECKILL_DELAY_TOPIC,
                            MessageBuilder.withPayload(messageDTO).build(),
                            SEND_TIMEOUT_MILLIS
                    );
                    log.info("Manual cancel seckill rollback message sent. orderSn: {}", order.getOrderSn());
                } catch (Exception exception) {
                    log.error("Manual cancel seckill rollback message send failed. orderSn: {}",
                            order.getOrderSn(), exception);
                }
            }
        });
    }

    private void registerRefundMessage(String orderSn, OrderRefundDTO refundDTO) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.asyncSend(ORDER_REFUND_TOPIC, refundDTO, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("Order refund message sent. orderSn: {}, sendResult: {}", orderSn, sendResult);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("Order refund message async send failed. orderSn: {}", orderSn, throwable);
                        }
                    });
                } catch (Exception exception) {
                    log.error("Order refund message submit failed. orderSn: {}", orderSn, exception);
                }
            }
        });
    }

    private void lockPhysicalStockOrCompensate(String orderSn, List<OrderItemDTO> checkedItems) {
        try {
            Result<Void> lockResult = productFeignClient.lockPhysicalStock(new StockLockDTO(orderSn, checkedItems));
            if (lockResult == null || !ResultCode.SUCCESS.getCode().equals(lockResult.getCode())) {
                String message = lockResult == null || !StringUtils.hasText(lockResult.getMessage())
                        ? "商品服务锁库失败"
                        : lockResult.getMessage();
                throw new BusinessException(ResultCode.BIZ_WARNING, message);
            }
        } catch (Exception exception) {
            compensateUnlockPhysicalStock(orderSn, checkedItems, exception);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ResultCode.BIZ_WARNING, "锁定库存失败，请稍后重试");
        }
    }

    private void validateSeckillOrderMessage(SeckillOrderDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀订单消息不能为空");
        }
        if (!StringUtils.hasText(dto.getOrderSn())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀订单号不能为空");
        }
        if (dto.getUserId() == null || dto.getUserId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀用户 ID 非法");
        }
        if (dto.getSkuId() == null || dto.getSkuId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀 SKU ID 非法");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀数量非法");
        }
    }

    private void compensateUnlockPhysicalStock(String orderSn, List<OrderItemDTO> items, Exception cause) {
        log.error("Try to compensate batch unlock stock. orderSn: {}", orderSn, cause);
        try {
            Result<Void> unlockResult = productFeignClient.unlockPhysicalStock(new StockLockDTO(orderSn, items));
            if (unlockResult == null || !ResultCode.SUCCESS.getCode().equals(unlockResult.getCode())) {
                String message = unlockResult == null || !StringUtils.hasText(unlockResult.getMessage())
                        ? "Product service no response"
                        : unlockResult.getMessage();
                log.error("Batch unlock stock compensation returned failure. orderSn: {}, message: {}", orderSn, message);
            }
        } catch (Exception unlockException) {
            log.error("Batch unlock stock compensation exception. orderSn: {}", orderSn, unlockException);
        }
    }

    private void compensateUnlockStock(String orderSn, OrderItemDTO item, Exception cause) {
        log.error("Legacy single stock compensation. orderSn: {}", orderSn, cause);
        try {
            UnlockStockDTO unlockDTO = new UnlockStockDTO(orderSn, item.getSkuId(), item.getQuantity());
            Result<Void> unlockResult = productFeignClient.unlockStock(unlockDTO);
            if (unlockResult == null || !ResultCode.SUCCESS.getCode().equals(unlockResult.getCode())) {
                String message = unlockResult == null || !StringUtils.hasText(unlockResult.getMessage())
                        ? "Product service no response"
                        : unlockResult.getMessage();
                log.error("Legacy single unlock stock returned failure. orderSn: {}, message: {}", orderSn, message);
            }
        } catch (Exception unlockException) {
            log.error("Legacy single unlock stock exception. orderSn: {}", orderSn, unlockException);
        }
    }

    private String generateOrderSn() {
        int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "VM" + LocalDateTime.now().format(ORDER_SN_TIME_FORMATTER) + randomSuffix;
    }

    private void registerNormalOrderAfterCommit(
            String orderSn,
            List<OrderItemDTO> items,
            Long userId,
            List<Long> checkedSkuIds
    ) {
        NormalOrderDelayDTO messageDTO = MqTraceContext.prepare(
                new NormalOrderDelayDTO(orderSn, items),
                orderSn
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.syncSend(
                            NORMAL_ORDER_DELAY_TOPIC,
                            MessageBuilder.withPayload(messageDTO).build(),
                            SEND_TIMEOUT_MILLIS,
                            DELAY_LEVEL_THIRTY_MINUTES
                    );
                    log.info("Normal order delay close message sent. orderSn: {}", orderSn);
                } catch (Exception exception) {
                    log.error("Normal order delay close message send failed. orderSn: {}", orderSn, exception);
                }

                try {
                    cartService.removeItems(userId, checkedSkuIds);
                } catch (Exception exception) {
                    log.error("Cart cleanup after order commit failed. orderSn: {}, userId: {}", orderSn, userId, exception);
                }
            }
        });
    }

    private void registerOrderDelayMessage(String orderSn, OrderItemDTO item) {
        OrderMessageDTO messageDTO = new OrderMessageDTO();
        messageDTO.setOrderSn(orderSn);
        messageDTO.setSkuId(item.getSkuId());
        messageDTO.setQuantity(item.getQuantity());
        MqTraceContext.prepare(messageDTO, orderSn);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.syncSend(
                            ORDER_DELAY_TOPIC,
                            MessageBuilder.withPayload(messageDTO).build(),
                            SEND_TIMEOUT_MILLIS,
                            DELAY_LEVEL_TEN_SECONDS
                    );
                    log.info("Order delay close message sent. orderSn: {}", orderSn);
                } catch (Exception exception) {
                    log.error("Order delay close message send failed. orderSn: {}", orderSn, exception);
                }
            }
        });
    }

    private void registerPaymentSuccessMessage(String orderSn, PaymentSuccessDTO paymentSuccessDTO) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.asyncSend(PAYMENT_SUCCESS_TOPIC, paymentSuccessDTO, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("Payment success message sent. orderSn: {}, sendResult: {}",
                                    orderSn, sendResult);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("Payment success message async send failed. orderSn: {}", orderSn, throwable);
                        }
                    });
                } catch (Exception exception) {
                    log.error("Payment success message submit failed. orderSn: {}", orderSn, exception);
                }
            }
        });
    }

    private List<Long> normalizeSkuIds(List<Long> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结算 SKU 列表不能为空");
        }
        List<Long> normalizedSkuIds = skuIds.stream()
                .filter(skuId -> skuId != null && skuId > 0)
                .distinct()
                .toList();
        if (normalizedSkuIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结算 SKU 列表不能为空");
        }
        return normalizedSkuIds;
    }

    private Map<Long, SkuVO> getSkuSnapshotMap(List<OrderItemDTO> items) {
        return items.stream()
                .map(OrderItemDTO::getSkuId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getSkuSnapshot,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private BigDecimal calculateTotalAmount(List<OrderItemDTO> items, Map<Long, SkuVO> skuSnapshotMap) {
        return items.stream()
                .map(item -> {
                    SkuVO skuSnapshot = skuSnapshotMap.get(item.getSkuId());
                    BigDecimal price = skuSnapshot.getPrice() == null ? DEFAULT_AMOUNT : skuSnapshot.getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(DEFAULT_AMOUNT, BigDecimal::add);
    }

    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户上下文不存在");
        }
        return currentUserId;
    }

    private SkuVO getSkuSnapshot(Long skuId) {
        Result<SkuVO> skuResult = productFeignClient.getSkuById(skuId);
        if (skuResult == null || !ResultCode.SUCCESS.getCode().equals(skuResult.getCode()) || skuResult.getData() == null) {
            String message = skuResult == null || !StringUtils.hasText(skuResult.getMessage())
                    ? "获取商品快照失败"
                    : skuResult.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, message);
        }
        return skuResult.getData();
    }

    private UserAddressVO fetchAddressSnapshot(Long addressId, Long userId) {
        Result<UserAddressVO> result = userFeignClient.getAddressById(addressId, userId);
        if (result == null || !ResultCode.SUCCESS.getCode().equals(result.getCode()) || result.getData() == null) {
            String message = result == null || !StringUtils.hasText(result.getMessage())
                    ? "获取收货地址失败"
                    : result.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, message);
        }
        return result.getData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(String orderSn, String deliveryCompany, String deliverySn) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (!StringUtils.hasText(deliveryCompany)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "物流公司不能为空");
        }
        if (!StringUtils.hasText(deliverySn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "物流单号不能为空");
        }

        int rows = orderMapper.markDelivered(orderSn, deliveryCompany, deliverySn);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "发货失败，订单状态异常或订单不存在");
        }
        log.info("订单发货成功, orderSn: {}, deliveryCompany: {}", orderSn, deliveryCompany);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(String orderSn) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号不能为空");
        }

        Long userId = getCurrentUserId();
        int rows = orderMapper.markReceived(orderSn, userId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "确认收货失败，订单状态异常或订单不属于你");
        }
        log.info("用户确认收货成功, orderSn: {}, userId: {}", orderSn, userId);
    }
}
