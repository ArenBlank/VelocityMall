package com.velocitymall.product.mq;

import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.model.dto.OrderRefundDTO;
import com.velocitymall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for order refund stock rollback events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "order-refund-topic", consumerGroup = "order-refund-consumer-group")
public class OrderRefundConsumer implements RocketMQListener<OrderRefundDTO> {

    private final SkuService skuService;

    @Override
    public void onMessage(OrderRefundDTO message) {
        MqTraceContext.runWithTrace(message, () -> {
            String orderSn = message == null ? null : message.getOrderSn();
            try {
                log.info("Received order refund message. orderSn: {}", orderSn);
                skuService.refundPhysicalStock(message);
                log.info("Order refund message consumed. orderSn: {}", orderSn);
            } catch (Exception exception) {
                log.error("Order refund message consume failed. orderSn: {}", orderSn, exception);
                throw new RuntimeException("Order refund message consume failed", exception);
            }
        });
    }
}
