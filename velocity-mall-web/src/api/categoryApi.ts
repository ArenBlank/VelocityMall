import { request } from './http';
import type { CategoryTreeVO } from './types';

export function listCategoryTree() {
  return request<CategoryTreeVO[]>({
    url: '/api/v1/categories/tree',
    method: 'GET'
  });
}
