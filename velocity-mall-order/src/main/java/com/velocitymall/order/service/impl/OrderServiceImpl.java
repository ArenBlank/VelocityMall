package com.velocitymall.order.service.impl;

import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.entity.OrderItem;
import com.velocitymall.order.mapper.OrderItemMapper;
import com.velocitymall.order.mapper.OrderMapper;
import com.velocitymall.order.model.dto.LockStockDTO;
import com.velocitymall.order.model.dto.OrderMessageDTO;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.dto.UnlockStockDTO;
import com.velocitymall.order.model.vo.OrderVO;
import com.velocitymall.order.model.vo.SkuVO;
import com.velocitymall.order.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 订单服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_STATUS_WAIT_PAY = 0;

    private static final String ORDER_DELAY_TOPIC = "velocity-mall-order-delay-topic";

    private static final long SEND_TIMEOUT_MILLIS = 3000L;

    private static final int DELAY_LEVEL_TEN_SECONDS = 3;

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;

    private static final DateTimeFormatter ORDER_SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ProductFeignClient productFeignClient;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitOrder(SubmitOrderDTO dto) {
        // TODO: 接入 Redis 实现 SET NX 短幂等键防重提交
        String orderSn = generateOrderSn();
        SkuVO skuSnapshot = getSkuSnapshot(dto.getSkuId());
        BigDecimal skuPrice = skuSnapshot.getPrice() == null ? DEFAULT_AMOUNT : skuSnapshot.getPrice();
        BigDecimal totalAmount = skuPrice.multiply(BigDecimal.valueOf(dto.getQuantity()));
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户上下文不存在");
        }

        Result<Void> lockStockResult = productFeignClient.lockStock(new LockStockDTO(dto.getSkuId(), dto.getQuantity()));
        if (lockStockResult == null || !ResultCode.SUCCESS.getCode().equals(lockStockResult.getCode())) {
            String message = lockStockResult == null || !StringUtils.hasText(lockStockResult.getMessage())
                    ? "锁定库存失败"
                    : lockStockResult.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, message);
        }

        try {
            Order order = Order.builder()
                    .orderSn(orderSn)
                    .userId(currentUserId)
                    .totalAmount(totalAmount)
                    .payAmount(totalAmount)
                    .status(ORDER_STATUS_WAIT_PAY)
                    .build();
            orderMapper.insert(order);

            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .orderSn(orderSn)
                    .spuId(skuSnapshot.getSpuId())
                    .skuId(dto.getSkuId())
                    .skuName(skuSnapshot.getSkuName())
                    .skuPrice(skuPrice)
                    .skuQuantity(dto.getQuantity())
                    .build();
            orderItemMapper.insert(orderItem);

            registerOrderDelayMessage(orderSn, dto);

            return OrderVO.builder()
                    .orderId(order.getId())
                    .orderSn(orderSn)
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .build();
        } catch (Exception exception) {
            compensateUnlockStock(orderSn, dto, exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "下单失败，请稍后重试");
        }
    }

    private void compensateUnlockStock(String orderSn, SubmitOrderDTO dto, Exception cause) {
        log.error("本地订单落库失败，尝试同步回滚已锁定的库存. orderSn: {}", orderSn, cause);
        try {
            UnlockStockDTO unlockDTO = new UnlockStockDTO(orderSn, dto.getSkuId(), dto.getQuantity());
            Result<Void> unlockResult = productFeignClient.unlockStock(unlockDTO);
            if (unlockResult == null || !ResultCode.SUCCESS.getCode().equals(unlockResult.getCode())) {
                String message = unlockResult == null || !StringUtils.hasText(unlockResult.getMessage())
                        ? "商品服务无响应"
                        : unlockResult.getMessage();
                log.error("同步解锁库存返回失败，产生死锁库存风险. orderSn: {}, message: {}", orderSn, message);
                return;
            }
            log.info("本地落库失败，同步解锁库存成功. orderSn: {}", orderSn);
        } catch (Exception unlockException) {
            log.error("同步解锁库存遭遇异常，产生死锁库存风险! orderSn: {}", orderSn, unlockException);
        }
    }

    private String generateOrderSn() {
        int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "VM" + LocalDateTime.now().format(ORDER_SN_TIME_FORMATTER) + randomSuffix;
    }

    private void registerOrderDelayMessage(String orderSn, SubmitOrderDTO dto) {
        // 注册事务同步钩子，确保本地事务提交后再发送 MQ 消息
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                OrderMessageDTO messageDTO = new OrderMessageDTO();
                messageDTO.setOrderSn(orderSn);
                messageDTO.setSkuId(dto.getSkuId());
                messageDTO.setQuantity(dto.getQuantity());

                try {
                    rocketMQTemplate.syncSend(
                            ORDER_DELAY_TOPIC,
                            MessageBuilder.withPayload(messageDTO).build(),
                            SEND_TIMEOUT_MILLIS,
                            DELAY_LEVEL_TEN_SECONDS
                    );
                    log.info("订单延时关单消息发送成功, orderSn: {}", orderSn);
                } catch (Exception exception) {
                    log.error("订单延时关单消息发送失败, orderSn: {}", orderSn, exception);
                }
            }
        });
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
}
