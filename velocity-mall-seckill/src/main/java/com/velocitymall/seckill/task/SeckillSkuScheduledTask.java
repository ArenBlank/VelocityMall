package com.velocitymall.seckill.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Seckill stock preheat task.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillSkuScheduledTask {

    public static final String SECKILL_STOCK_PREFIX = "velocitymall:seckill:stock:";

    private static final Long TEST_SKU_ID = 2001L;

    private static final int TEST_STOCK = 50;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Local test preheat runs every minute. Production should preheat before the activity starts.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void preheatSeckillStock() {
        String stockKey = SECKILL_STOCK_PREFIX + TEST_SKU_ID;
        Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(TEST_STOCK));
        if (Boolean.TRUE.equals(initialized)) {
            log.info("预热秒杀库存成功. skuId: {}, stock: {}", TEST_SKU_ID, TEST_STOCK);
            return;
        }
        String currentStock = stringRedisTemplate.opsForValue().get(stockKey);
        log.info("秒杀库存已存在，跳过重复预热. skuId: {}, currentStock: {}", TEST_SKU_ID, currentStock);
    }
}
