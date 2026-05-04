package com.velocitymall.search.mq;

import com.velocitymall.common.model.dto.ProductSyncDTO;
import com.velocitymall.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Product search synchronization consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "product-sync-topic",
        consumerGroup = "product-sync-consumer-group",
        consumeMode = ConsumeMode.ORDERLY
)
public class ProductSyncConsumer implements RocketMQListener<ProductSyncDTO> {

    private final SearchService searchService;

    @Override
    public void onMessage(ProductSyncDTO message) {
        try {
            searchService.syncProduct(message);
        } catch (Exception exception) {
            log.error("Product search sync failed, will retry. message: {}", message, exception);
            throw new RuntimeException("Product search sync failed", exception);
        }
    }
}
