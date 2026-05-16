import { http } from './http';
import type { AdminReviewVO, PageVO } from './types';

export function listAdminReviews(params: { page: number; size: number; spuId?: number | null; keyword?: string }) {
  return http.get<PageVO<AdminReviewVO>, PageVO<AdminReviewVO>>('/api/v1/admin/reviews', { params });
}

export function deleteAdminReview(id: number) {
  return http.delete<void, void>(`/api/v1/admin/reviews/${id}`);
}
