package com.velocitymall.seckill.mq;

import com.velocitymall.common.model.dto.SeckillRollbackDTO;
import com.velocitymall.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for Redis seckill stock rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "seckill-rollback-topic", consumerGroup = "seckill-rollback-consumer-group")
public class SeckillStockRollbackConsumer implements RocketMQListener<SeckillRollbackDTO> {

    private static final Long ROLLBACK_SUCCESS = 1L;

    private static final Long ROLLBACK_IDEMPOTENT = 0L;

    private final SeckillService seckillService;

    @Override
    public void onMessage(SeckillRollbackDTO message) {
        log.info("Received seckill stock rollback message. skuId: {}, userId: {}",
                message.getSkuId(), message.getUserId());
        try {
            Long result = seckillService.rollbackStock(message.getSkuId(), message.getUserId());
            if (ROLLBACK_SUCCESS.equals(result)) {
                log.info("Seckill stock rollback succeeded. skuId: {}, userId: {}",
                        message.getSkuId(), message.getUserId());
                return;
            }
            if (ROLLBACK_IDEMPOTENT.equals(result)) {
                log.info("Seckill stock rollback idempotent no-op. skuId: {}, userId: {}",
                        message.getSkuId(), message.getUserId());
                return;
            }
            log.warn("Seckill stock rollback returned unexpected result. skuId: {}, userId: {}, result: {}",
                    message.getSkuId(), message.getUserId(), result);
        } catch (Exception exception) {
            log.error("Seckill stock rollback failed. skuId: {}, userId: {}",
                    message.getSkuId(), message.getUserId(), exception);
            throw new RuntimeException("Seckill stock rollback failed", exception);
        }
    }
}
