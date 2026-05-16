import { http } from './http';
import type { AdminLoginVO } from './types';

export function loginAdmin(payload: { username: string; password: string }) {
  return http.post<AdminLoginVO, AdminLoginVO>('/api/v1/admin/login', payload);
}
