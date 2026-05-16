package com.velocitymall.seckill.task;

import com.velocitymall.seckill.entity.SeckillActivity;
import com.velocitymall.seckill.service.SeckillActivityService;
import java.util.List;
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

    private final StringRedisTemplate stringRedisTemplate;

    private final SeckillActivityService seckillActivityService;

    /**
     * Local preheat runs every minute. Activity stock comes from sms_seckill_activity.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void preheatSeckillStock() {
        List<SeckillActivity> activities = seckillActivityService.listPreheatActivities();
        if (activities.isEmpty()) {
            log.info("No active seckill activity needs preheating.");
            return;
        }

        for (SeckillActivity activity : activities) {
            String stockKey = SECKILL_STOCK_PREFIX + activity.getSkuId();
            Boolean initialized = stringRedisTemplate.opsForValue()
                    .setIfAbsent(stockKey, String.valueOf(activity.getSeckillStock()));
            if (Boolean.TRUE.equals(initialized)) {
                log.info("Seckill stock preheated. skuId: {}, stock: {}",
                        activity.getSkuId(), activity.getSeckillStock());
                continue;
            }
            String currentStock = stringRedisTemplate.opsForValue().get(stockKey);
            log.info("Seckill stock already exists, skip duplicate preheat. skuId: {}, currentStock: {}",
                    activity.getSkuId(), currentStock);
        }
    }
}
