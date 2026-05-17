import { http } from './http';
import type { AdminLoginVO, AdminProfileVO } from './types';

export function loginAdmin(payload: { username: string; password: string }) {
  return http.post<AdminLoginVO, AdminLoginVO>('/api/v1/admin/login', payload);
}

export function getAdminMe() {
  return http.get<AdminProfileVO, AdminProfileVO>('/api/v1/admin/me');
}
