import { http } from './http';
import type { AdminCouponRequest, AdminCouponVO, PageVO } from './types';

export function listAdminCoupons(params: { page: number; size: number; status?: number | null }) {
  return http.get<PageVO<AdminCouponVO>, PageVO<AdminCouponVO>>('/api/v1/admin/coupons', { params });
}

export function createAdminCoupon(payload: AdminCouponRequest) {
  return http.post<AdminCouponVO, AdminCouponVO>('/api/v1/admin/coupons', payload);
}

export function updateAdminCoupon(id: number, payload: AdminCouponRequest) {
  return http.put<AdminCouponVO, AdminCouponVO>(`/api/v1/admin/coupons/${id}`, payload);
}

export function updateAdminCouponStatus(id: number, status: number) {
  return http.put<AdminCouponVO, AdminCouponVO>(`/api/v1/admin/coupons/${id}/status`, { status });
}
