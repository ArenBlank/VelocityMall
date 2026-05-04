package com.velocitymall.order.mq;

import com.velocitymall.common.model.dto.SeckillOrderDTO;
import com.velocitymall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Consumer for asynchronous seckill order creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "seckill-order-topic", consumerGroup = "seckill-order-consumer-group")
public class SeckillOrderConsumer implements RocketMQListener<SeckillOrderDTO> {

    private static final String SECKILL_DELAY_TOPIC = "seckill-delay-topic";

    private static final long SEND_TIMEOUT_MILLIS = 3000L;

    /**
     * RocketMQ 4.x default delay level 9 means 5 minutes.
     */
    private static final int DELAY_LEVEL_FIVE_MINUTES = 9;

    private final OrderService orderService;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(SeckillOrderDTO message) {
        log.info("Received seckill order message. orderSn: {}, skuId: {}, userId: {}",
                message.getOrderSn(), message.getSkuId(), message.getUserId());
        try {
            orderService.createSeckillOrder(message);
            rocketMQTemplate.syncSend(
                    SECKILL_DELAY_TOPIC,
                    MessageBuilder.withPayload(message).build(),
                    SEND_TIMEOUT_MILLIS,
                    DELAY_LEVEL_FIVE_MINUTES
            );
            log.info("Seckill delay close message sent. orderSn: {}", message.getOrderSn());
        } catch (Exception exception) {
            log.error("Seckill order persistence or delay close message failed. orderSn: {}",
                    message.getOrderSn(), exception);
            throw new RuntimeException("Seckill order persistence or delay close message failed", exception);
        }
    }
}
