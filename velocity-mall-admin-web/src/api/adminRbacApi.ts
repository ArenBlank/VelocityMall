import { http } from './http';
import type {
  AdminPermissionVO,
  AdminRoleRequest,
  AdminRoleVO,
  AdminUserCreateRequest,
  AdminUserUpdateRequest,
  AdminUserVO,
  PageVO
} from './types';

export function listAdminUsers(params: { page: number; size: number; keyword?: string; status?: number | null }) {
  return http.get<PageVO<AdminUserVO>, PageVO<AdminUserVO>>('/api/v1/admin/rbac/admins', { params });
}

export function createAdminUser(payload: AdminUserCreateRequest) {
  return http.post<AdminUserVO, AdminUserVO>('/api/v1/admin/rbac/admins', payload);
}

export function updateAdminUser(adminId: string, payload: AdminUserUpdateRequest) {
  return http.put<AdminUserVO, AdminUserVO>(`/api/v1/admin/rbac/admins/${adminId}`, payload);
}

export function updateAdminUserStatus(adminId: string, status: number) {
  return http.put<AdminUserVO, AdminUserVO>(`/api/v1/admin/rbac/admins/${adminId}/status`, { status });
}

export function resetAdminUserPassword(adminId: string, password: string) {
  return http.put<void, void>(`/api/v1/admin/rbac/admins/${adminId}/password`, { password });
}

export function listAdminRoles() {
  return http.get<AdminRoleVO[], AdminRoleVO[]>('/api/v1/admin/rbac/roles');
}

export function createAdminRole(payload: AdminRoleRequest) {
  return http.post<AdminRoleVO, AdminRoleVO>('/api/v1/admin/rbac/roles', payload);
}

export function updateAdminRole(roleId: string, payload: AdminRoleRequest) {
  return http.put<AdminRoleVO, AdminRoleVO>(`/api/v1/admin/rbac/roles/${roleId}`, payload);
}

export function updateAdminRoleStatus(roleId: string, status: number) {
  return http.put<AdminRoleVO, AdminRoleVO>(`/api/v1/admin/rbac/roles/${roleId}/status`, { status });
}

export function listAdminPermissions() {
  return http.get<AdminPermissionVO[], AdminPermissionVO[]>('/api/v1/admin/rbac/permissions');
}
