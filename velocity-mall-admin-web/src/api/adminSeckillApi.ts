import { http } from './http';
import type { AdminSeckillActivityRequest, AdminSeckillActivityVO, PageVO } from './types';

export function listAdminSeckillActivities(params: {
  page: number;
  size: number;
  state?: string;
  skuId?: number | null;
}) {
  return http.get<PageVO<AdminSeckillActivityVO>, PageVO<AdminSeckillActivityVO>>(
    '/api/v1/admin/seckill/activities',
    { params }
  );
}

export function createAdminSeckillActivity(payload: AdminSeckillActivityRequest) {
  return http.post<AdminSeckillActivityVO, AdminSeckillActivityVO>('/api/v1/admin/seckill/activities', payload);
}

export function updateAdminSeckillActivity(id: number, payload: AdminSeckillActivityRequest) {
  return http.put<AdminSeckillActivityVO, AdminSeckillActivityVO>(`/api/v1/admin/seckill/activities/${id}`, payload);
}

export function updateAdminSeckillActivityStatus(id: number, status: number) {
  return http.put<AdminSeckillActivityVO, AdminSeckillActivityVO>(`/api/v1/admin/seckill/activities/${id}/status`, { status });
}

export function preheatAdminSeckillActivity(id: number) {
  return http.post<AdminSeckillActivityVO, AdminSeckillActivityVO>(`/api/v1/admin/seckill/activities/${id}/preheat`);
}
