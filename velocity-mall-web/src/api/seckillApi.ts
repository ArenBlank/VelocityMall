import { request } from './http';
import type { SeckillActivityVO } from './types';

export function listSeckillActivities() {
  return request<SeckillActivityVO[]>({
    url: '/api/v1/seckill/activities',
    method: 'GET'
  });
}

export function getSeckillActivityBySkuId(skuId: number) {
  return request<SeckillActivityVO>({
    url: `/api/v1/seckill/activities/skus/${skuId}`,
    method: 'GET'
  });
}

export function executeSeckill(skuId: number) {
  return request<string>({
    url: `/api/v1/seckill/execute/${skuId}`,
    method: 'POST'
  });
}
