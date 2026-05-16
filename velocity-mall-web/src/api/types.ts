export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export interface UserInfoVO {
  id: number;
  username: string;
  nickname?: string;
  phone?: string;
}

export interface LoginVO {
  token: string;
  user?: UserInfoVO;
}

export interface PageVO<T> {
  page: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

export interface SearchSkuVO {
  skuId: number;
  skuName: string;
  skuPic: string;
  price: number;
  saleCount: number;
}

export type SeckillActivityState = 'NOT_STARTED' | 'ACTIVE' | 'ENDED' | 'DISABLED';

export interface SeckillActivityVO {
  activityId: number;
  skuId: number;
  spuId: number;
  activityName: string;
  seckillPrice: number;
  originalPrice: number;
  seckillStock: number;
  remainingStock: number;
  startTime: string;
  endTime: string;
  status: number;
  state: SeckillActivityState;
}

export type SeckillResultState = 'PROCESSING' | 'SUCCESS' | 'FAILED';

export interface SeckillResultVO {
  state: SeckillResultState;
  orderSn: string | null;
  orderStatus: number | null;
  message: string;
}

export interface SkuVO {
  skuId: number;
  spuId: number;
  skuName: string;
  skuCode: string;
  price: number;
  availableStock: number;
  coverImg: string;
}

export interface SpuDetailVO {
  spuId: number;
  categoryId: number;
  name: string;
  description: string;
  publishStatus: number;
  skuList: SkuVO[];
}

export interface OrderItemVO {
  skuId: number;
  skuName: string;
  skuPic: string;
  skuPrice: number;
  quantity: number;
}

export interface OrderDetailVO {
  orderSn: string;
  totalAmount: number;
  payAmount: number;
  payType: number | null;
  status: number;
  orderType: number;
  createTime: string;
  items: OrderItemVO[];
}

export interface AddressVO {
  id: number;
  userId: number;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  region: string;
  detailAddress: string;
  isDefault: number | boolean;
}

export interface CartItemVO {
  skuId: number;
  skuName: string;
  price: number;
  quantity: number;
  availableStock: number;
  totalAmount: number;
}

export interface CategoryTreeVO {
  id: number;
  parentId: number;
  name: string;
  level: number;
  sort: number;
  icon?: string;
  children?: CategoryTreeVO[];
}

export interface OrderVO {
  orderSn: string;
  totalAmount: number;
  payAmount: number;
  payType: number | null;
  status: number;
  orderType: number;
  createTime: string;
}

export interface CouponVO {
  couponId: number;
  name: string;
  amount: number;
  minPoint: number;
  stock: number;
  limitPerUser: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface UserCouponVO {
  historyId: number;
  couponId: number;
  name: string;
  amount: number;
  minPoint: number;
  useStatus: number;
  claimTime: string;
  useTime: string | null;
  orderSn: string | null;
  startTime: string;
  endTime: string;
  couponStatus: number;
  available: boolean;
}

export interface ReviewVO {
  id: number;
  skuId: number;
  spuId: number;
  rating: number;
  content: string;
  hasPictures: number | boolean;
  likeCount: number;
  dislikeCount: number;
  replyCount: number;
  currentInteractionType: number | null;
  mine?: boolean;
  createTime: string;
}

export interface ReviewStatsVO {
  spuId: number;
  totalCount: number;
  goodCount: number;
  goodRate: number;
}
