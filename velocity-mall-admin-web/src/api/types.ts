export interface PageVO<T> {
  current: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

export interface AdminLoginVO {
  token: string;
  adminId: string;
  username: string;
  realName: string;
  roles: string[];
  permissions: string[];
}

export interface AdminProfileVO {
  adminId: string;
  username: string;
  realName: string;
  roles: string[];
  permissions: string[];
}

export interface AdminPermissionVO {
  id: string;
  permissionCode: string;
  permissionName: string;
  resource: string;
  action: string;
  description?: string;
  status: number;
}

export interface AdminRoleVO {
  id: string;
  roleCode: string;
  roleName: string;
  description?: string;
  status: number;
  permissions: AdminPermissionVO[];
  createTime?: string;
  updateTime?: string;
}

export interface AdminUserVO {
  adminId: string;
  username: string;
  realName?: string;
  status: number;
  roles: AdminRoleVO[];
  createTime?: string;
  updateTime?: string;
}

export interface AdminUserCreateRequest {
  username: string;
  password: string;
  realName?: string;
  status: number;
  roleIds: string[];
}

export interface AdminUserUpdateRequest {
  realName?: string;
  status: number;
  roleIds: string[];
}

export interface AdminRoleRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  status: number;
  permissionIds: string[];
}

export interface AdminSkuVO {
  skuId: number;
  spuId: number;
  skuName: string;
  skuCode: string;
  price: number;
  stock: number;
  lockStock: number;
  availableStock: number;
  saleCount: number;
  coverImg: string;
  createTime?: string;
  updateTime?: string;
}

export interface AdminSpuVO {
  spuId: number;
  categoryId: number;
  name: string;
  description: string;
  publishStatus: number;
  createTime?: string;
  updateTime?: string;
  skuList: AdminSkuVO[];
}

export interface AdminSpuRequest {
  categoryId: number;
  name: string;
  description?: string;
  publishStatus: number;
}

export interface AdminSkuRequest {
  spuId: number;
  skuName: string;
  skuCode: string;
  price: number;
  stock: number;
  coverImg?: string;
}

export interface AdminOrderItemVO {
  skuId: number;
  spuId: number;
  skuName: string;
  skuPic: string;
  skuPrice: number;
  quantity: number;
}

export interface AdminOrderVO {
  userId: number;
  orderSn: string;
  totalAmount: number;
  payAmount: number;
  payType: number | null;
  payTime: string | null;
  orderType: number;
  status: number;
  remark: string | null;
  receiverName: string | null;
  receiverPhone: string | null;
  receiverProvince: string | null;
  receiverCity: string | null;
  receiverRegion: string | null;
  receiverDetailAddress: string | null;
  deliveryCompany: string | null;
  deliverySn: string | null;
  deliveryTime: string | null;
  createTime?: string;
  items: AdminOrderItemVO[];
}

export interface AdminSeckillActivityVO {
  id: number;
  skuId: number;
  spuId: number;
  activityName: string;
  seckillPrice: number;
  originalPrice: number;
  seckillStock: number;
  remainingStock: number | null;
  startTime: string;
  endTime: string;
  status: number;
  state: 'NOT_STARTED' | 'ACTIVE' | 'ENDED' | 'DISABLED' | string;
  createTime?: string;
}

export interface AdminSeckillActivityRequest {
  skuId: number;
  spuId: number;
  activityName: string;
  seckillPrice: number;
  originalPrice: number;
  seckillStock: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface SeckillTestRequest {
  skuId: number;
  stock?: number;
}

export interface SeckillTestResult {
  skuId: number;
  activityId?: number;
  activityName?: string;
  mysqlStock?: number;
  redisStock?: number;
  wasDisabled?: boolean;
  deletedOrderItems?: number;
  deletedOrders?: number;
  mysqlStockBefore?: number;
  mysqlStockAfter?: number;
  redisCleared?: boolean;
}

export interface AdminCouponVO {
  id: number;
  name: string;
  amount: number;
  minPoint: number;
  stock: number;
  limitPerUser: number;
  startTime: string;
  endTime: string;
  status: number;
  createTime?: string;
}

export interface AdminCouponRequest {
  name: string;
  amount: number;
  minPoint: number;
  stock: number;
  limitPerUser: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface AdminReviewVO {
  id: number;
  userId: number;
  orderSn: string;
  skuId: number;
  spuId: number;
  rating: number;
  content: string;
  likeCount: number;
  dislikeCount: number;
  createTime?: string;
}

export interface FileUploadVO {
  url: string;
  objectName: string;
  bucket: string;
}

export interface AdminRebuildIndexVO {
  indexedCount: number;
  message: string;
}
