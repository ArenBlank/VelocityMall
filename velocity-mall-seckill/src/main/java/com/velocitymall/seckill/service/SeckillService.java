package com.velocitymall.seckill.service;

import java.util.Map;

/**
 * Seckill service.
 */
public interface SeckillService {

    /**
     * Execute a seckill request for one SKU.
     *
     * @param skuId SKU ID
     * @return accepted message
     */
    String execute(Long skuId);

    /**
     * Execute a seckill request with explicit userId (for admin test bridge).
     */
    String execute(Long skuId, Long userId);

    /**
     * Roll back Redis seckill stock for an unpaid timeout order.
     *
     * @param skuId  SKU ID
     * @param userId user ID
     * @return Lua result, 1 means rolled back, 0 means idempotent no-op
     */
    Long rollbackStock(Long skuId, Long userId);

    /**
     * Get real-time stress test metrics for a SKU.
     */
    Map<String, Object> getStressMetrics(Long skuId);

    /**
     * Reset stress test metrics counters for a SKU.
     */
    void resetStressMetrics(Long skuId);
}
