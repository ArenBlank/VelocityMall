import { request } from './http';
import type { SkuVO, SpuDetailVO } from './types';

export function getSku(skuId: number) {
  return request<SkuVO>({
    url: `/api/v1/products/skus/${skuId}`,
    method: 'GET'
  });
}

export function getSpu(spuId: number) {
  return request<SpuDetailVO>({
    url: `/api/v1/products/spus/${spuId}`,
    method: 'GET'
  });
}
