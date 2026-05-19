import { http } from './http';
import type { AdminSeckillActivityRequest, AdminSeckillActivityVO, PageVO, SeckillTestRequest, SeckillTestResult } from './types';

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

export function initSeckillTest(payload: SeckillTestRequest) {
  return http.post<SeckillTestResult, SeckillTestResult>('/api/v1/admin/seckill/activities/test/init', payload);
}

export function cleanupSeckillTest(skuId: number) {
  return http.post<SeckillTestResult, SeckillTestResult>(`/api/v1/admin/seckill/activities/test/cleanup/${skuId}`);
}

// ---- 压测观测台专用 ----

export interface StressTestResult {
  skuId: number;
  userId?: number;
  success: boolean;
  message: string;
  elapsed: number;
}

export interface StressMetrics {
  stock: number;
  qps: number;
  latency: number;
  mqQueue: number;
}

export function stressInit(payload: SeckillTestRequest) {
  return http.post<SeckillTestResult, SeckillTestResult>('/api/v1/admin/seckill/stress/init', payload);
}

export function stressCleanup(skuId: number) {
  return http.post<SeckillTestResult, SeckillTestResult>(`/api/v1/admin/seckill/stress/cleanup/${skuId}`);
}

export function stressSingleTest(skuId: number) {
  return http.post<StressTestResult, StressTestResult>(`/api/v1/admin/seckill/stress/single-test/${skuId}`);
}

export interface StressEngineResult {
  engine: string;
  concurrency: number;
  waves: number;
  totalRequests: number;
  totalElapsedMs: number;
  qps: number;
  success: number;
  duplicate: number;
  soldOut: number;
  fail: number;
  zeroOversell: boolean;
}

export function stressRunK6() {
  return http.post<StressEngineResult, StressEngineResult>(
    '/api/v1/admin/seckill/stress/run-k6'
  );
}

export function stressMetrics(skuId: number) {
  return http.get<StressMetrics, StressMetrics>(`/api/v1/admin/seckill/stress/metrics/${skuId}`);
}
