package com.velocitymall.admin.constant;

/**
 * Permission codes used by the admin RBAC model.
 */
public final class AdminPermissionCodes {

    public static final String PRODUCT_READ = "product:read";
    public static final String PRODUCT_WRITE = "product:write";
    public static final String ORDER_READ = "order:read";
    public static final String ORDER_DELIVER = "order:deliver";
    public static final String SECKILL_READ = "seckill:read";
    public static final String SECKILL_WRITE = "seckill:write";
    public static final String SECKILL_PREHEAT = "seckill:preheat";
    public static final String COUPON_READ = "coupon:read";
    public static final String COUPON_WRITE = "coupon:write";
    public static final String REVIEW_READ = "review:read";
    public static final String REVIEW_DELETE = "review:delete";
    public static final String SYSTEM_REBUILD = "system:rebuild";
    public static final String RBAC_READ = "rbac:read";
    public static final String RBAC_WRITE = "rbac:write";

    private AdminPermissionCodes() {
    }
}
