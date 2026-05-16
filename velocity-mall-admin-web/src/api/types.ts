export interface PageVO<T> {
  current: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

export interface AdminLoginVO {
  token: string;
  adminId: number;
  username: string;
  realName: string;
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
