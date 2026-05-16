import { http } from './http';
import type { AdminSkuRequest, AdminSkuVO, AdminSpuRequest, AdminSpuVO, FileUploadVO, PageVO } from './types';

export function listAdminSpus(params: { page: number; size: number; keyword?: string; status?: number | null }) {
  return http.get<PageVO<AdminSpuVO>, PageVO<AdminSpuVO>>('/api/v1/admin/products/spus', { params });
}

export function getAdminSpu(spuId: number) {
  return http.get<AdminSpuVO, AdminSpuVO>(`/api/v1/admin/products/spus/${spuId}`);
}

export function createAdminSpu(payload: AdminSpuRequest) {
  return http.post<AdminSpuVO, AdminSpuVO>('/api/v1/admin/products/spus', payload);
}

export function updateAdminSpu(spuId: number, payload: AdminSpuRequest) {
  return http.put<AdminSpuVO, AdminSpuVO>(`/api/v1/admin/products/spus/${spuId}`, payload);
}

export function updateAdminSpuStatus(spuId: number, action: 'publish' | 'unpublish') {
  return http.put<void, void>(`/api/v1/admin/products/spus/${spuId}/status`, null, { params: { action } });
}

export function createAdminSku(payload: AdminSkuRequest) {
  return http.post<AdminSkuVO, AdminSkuVO>('/api/v1/admin/products/skus', payload);
}

export function updateAdminSku(skuId: number, payload: AdminSkuRequest) {
  return http.put<AdminSkuVO, AdminSkuVO>(`/api/v1/admin/products/skus/${skuId}`, payload);
}

export function uploadAdminSkuCover(skuId: number, file: File) {
  const form = new FormData();
  form.append('file', file);
  return http.post<FileUploadVO, FileUploadVO>(`/api/v1/admin/products/skus/${skuId}/cover`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
