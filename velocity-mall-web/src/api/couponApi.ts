import { request } from './http';
import type { CouponVO, PageVO, UserCouponVO } from './types';

export function listAvailableCoupons(params: { page?: number; size?: number } = {}) {
  return request<PageVO<CouponVO>>({
    url: '/api/v1/coupons/available',
    method: 'GET',
    params: {
      page: params.page ?? 1,
      size: params.size ?? 10
    }
  });
}

export function listMyCoupons(params: { page?: number; size?: number; useStatus?: number | null } = {}) {
  const query: Record<string, number> = {
    page: params.page ?? 1,
    size: params.size ?? 20
  };
  if (params.useStatus !== null && params.useStatus !== undefined) {
    query.useStatus = params.useStatus;
  }
  return request<PageVO<UserCouponVO>>({
    url: '/api/v1/coupons/my',
    method: 'GET',
    params: query
  });
}

export function claimCoupon(couponId: number) {
  return request<void>({
    url: `/api/v1/coupons/${couponId}/claim`,
    method: 'POST'
  });
}
