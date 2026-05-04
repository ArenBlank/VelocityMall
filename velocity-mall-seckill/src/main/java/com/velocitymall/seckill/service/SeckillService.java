package com.velocitymall.seckill.service;

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
     * Roll back Redis seckill stock for an unpaid timeout order.
     *
     * @param skuId SKU ID
     * @param userId user ID
     * @return Lua result, 1 means rolled back, 0 means idempotent no-op
     */
    Long rollbackStock(Long skuId, Long userId);
}
