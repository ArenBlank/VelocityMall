import { request } from './http';
import type { PageVO, SearchSkuVO } from './types';

export function searchSkus(params: { keyword?: string; sort?: string; page?: number; size?: number } = {}) {
  return request<PageVO<SearchSkuVO>>({
    url: '/api/v1/search/skus',
    method: 'GET',
    params: {
      sort: 'sale_desc',
      page: 1,
      size: 10,
      ...params
    }
  });
}
