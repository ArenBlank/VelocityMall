package com.velocitymall.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.model.dto.SeckillOrderDTO;
import com.velocitymall.common.model.dto.SeckillRollbackDTO;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer for delayed seckill order closing and Redis stock rollback notification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "seckill-delay-topic", consumerGroup = "seckill-delay-consumer-group")
public class SeckillDelayConsumer implements RocketMQListener<SeckillOrderDTO> {

    private static final Integer ORDER_STATUS_WAIT_PAY = 0;

    private static final Integer ORDER_STATUS_CLOSED = 4;

    private static final String SECKILL_ROLLBACK_TOPIC = "seckill-rollback-topic";

    private static final long SEND_TIMEOUT_MILLIS = 3000L;

    private final OrderMapper orderMapper;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(SeckillOrderDTO message) {
        MqTraceContext.runWithTrace(message, () -> handleMessage(message));
    }

    private void handleMessage(SeckillOrderDTO message) {
        log.info("Received seckill delay close message. orderSn: {}", message.getOrderSn());
        try {
            Order order = selectByOrderSn(message.getOrderSn());
            if (order == null) {
                log.warn("Seckill order not found, discard delay message. orderSn: {}", message.getOrderSn());
                return;
            }

            Integer status = order.getStatus();
            if (ORDER_STATUS_WAIT_PAY.equals(status)) {
                int updatedRows = orderMapper.closeOrder(message.getOrderSn());
                if (updatedRows == 0) {
                    handleConcurrentClose(message);
                    return;
                }
                sendRollbackMessage(message);
                log.info("Seckill order closed and rollback message sent. orderSn: {}", message.getOrderSn());
                return;
            }

            if (ORDER_STATUS_CLOSED.equals(status)) {
                sendRollbackMessage(message);
                log.info("Seckill order already closed, rollback message sent again for compensation. orderSn: {}",
                        message.getOrderSn());
                return;
            }

            log.info("Seckill order status is not wait-pay or closed, discard delay message. orderSn: {}, status: {}",
                    message.getOrderSn(), status);
        } catch (Exception exception) {
            log.error("Seckill delay close processing failed. orderSn: {}", message.getOrderSn(), exception);
            throw new RuntimeException("Seckill delay close processing failed", exception);
        }
    }

    private void handleConcurrentClose(SeckillOrderDTO message) {
        Order latestOrder = selectByOrderSn(message.getOrderSn());
        if (latestOrder == null) {
            log.warn("Seckill order disappeared during close retry. orderSn: {}", message.getOrderSn());
            return;
        }
        if (ORDER_STATUS_CLOSED.equals(latestOrder.getStatus())) {
            sendRollbackMessage(message);
            log.info("Seckill order was closed by another consumer, rollback message sent. orderSn: {}",
                    message.getOrderSn());
            return;
        }
        if (ORDER_STATUS_WAIT_PAY.equals(latestOrder.getStatus())) {
            throw new RuntimeException("Seckill order close affected zero rows while status is still wait-pay");
        }
        log.info("Seckill order status changed by concurrent flow, discard rollback. orderSn: {}, status: {}",
                message.getOrderSn(), latestOrder.getStatus());
    }

    private Order selectByOrderSn(String orderSn) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, orderSn)
                .last("LIMIT 1"));
    }

    private void sendRollbackMessage(SeckillOrderDTO message) {
        SeckillRollbackDTO rollbackDTO = new SeckillRollbackDTO(message.getSkuId(), message.getUserId());
        MqTraceContext.prepare(
                rollbackDTO,
                message.getBusinessId() == null ? message.getOrderSn() : message.getBusinessId()
        );
        rocketMQTemplate.syncSend(
                SECKILL_ROLLBACK_TOPIC,
                MessageBuilder.withPayload(rollbackDTO).build(),
                SEND_TIMEOUT_MILLIS
        );
    }
}
