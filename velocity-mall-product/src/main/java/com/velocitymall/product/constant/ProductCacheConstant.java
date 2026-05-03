package com.velocitymall.product.constant;

/**
 * 商品缓存 Key 与 TTL 常量。
 */
public final class ProductCacheConstant {

    public static final String SPU_DETAIL_KEY_PREFIX = "velocitymall:product:spu:";

    public static final String SPU_DETAIL_LOCK_PREFIX = "velocitymall:product:lock:";

    public static final Long DUMMY_SPU_ID = -1L;

    public static final long EMPTY_CACHE_TTL_SECONDS = 60L;

    public static final long SPU_DETAIL_CACHE_BASE_TTL_MINUTES = 30L;

    public static final long SPU_DETAIL_CACHE_RANDOM_MINUTES = 5L;

    public static final long SPU_DETAIL_LOCK_WAIT_SECONDS = 1L;

    public static final long SPU_DETAIL_LOCK_LEASE_SECONDS = 10L;

    public static final String UNLOCK_STOCK_KEY_PREFIX = "velocitymall:product:unlock:";

    public static final String UNLOCK_STOCK_VALUE = "UNLOCKED";

    public static final long UNLOCK_STOCK_TTL_DAYS = 7L;

    private ProductCacheConstant() {
    }

    public static String spuDetailKey(Long spuId) {
        return SPU_DETAIL_KEY_PREFIX + spuId;
    }

    public static String spuDetailLockKey(Long spuId) {
        return SPU_DETAIL_LOCK_PREFIX + spuId;
    }

    public static String unlockStockKey(String orderSn, Long skuId) {
        return UNLOCK_STOCK_KEY_PREFIX + orderSn + ":" + skuId;
    }
}
