package com.velocitymall.product.mq;

import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.model.dto.PaymentSuccessDTO;
import com.velocitymall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for payment success events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "payment-success-topic", consumerGroup = "payment-success-consumer-group")
public class PaymentSuccessConsumer implements RocketMQListener<PaymentSuccessDTO> {

    private final SkuService skuService;

    @Override
    public void onMessage(PaymentSuccessDTO message) {
        MqTraceContext.runWithTrace(message, () -> {
            try {
                String orderSn = message == null ? null : message.getOrderSn();
                Long userId = message == null ? null : message.getUserId();
                log.info("Received payment success message. orderSn: {}, userId: {}", orderSn, userId);
                skuService.deductPhysicalStock(message);
                log.info("Payment success message consumed. orderSn: {}", orderSn);
            } catch (Exception exception) {
                String orderSn = message == null ? null : message.getOrderSn();
                log.error("Payment success message consume failed. orderSn: {}", orderSn, exception);
                throw new RuntimeException("Payment success message consume failed", exception);
            }
        });
    }
}
