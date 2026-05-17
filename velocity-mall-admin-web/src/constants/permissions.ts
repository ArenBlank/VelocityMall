export const AdminPermissions = {
  PRODUCT_READ: 'product:read',
  PRODUCT_WRITE: 'product:write',
  ORDER_READ: 'order:read',
  ORDER_DELIVER: 'order:deliver',
  SECKILL_READ: 'seckill:read',
  SECKILL_WRITE: 'seckill:write',
  SECKILL_PREHEAT: 'seckill:preheat',
  COUPON_READ: 'coupon:read',
  COUPON_WRITE: 'coupon:write',
  REVIEW_READ: 'review:read',
  REVIEW_DELETE: 'review:delete',
  SYSTEM_REBUILD: 'system:rebuild',
  RBAC_READ: 'rbac:read',
  RBAC_WRITE: 'rbac:write'
} as const;
