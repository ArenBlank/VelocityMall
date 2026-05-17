import { defineStore } from 'pinia';
import {
  createAdminRole,
  createAdminUser,
  listAdminPermissions,
  listAdminRoles,
  listAdminUsers,
  resetAdminUserPassword,
  updateAdminRole,
  updateAdminRoleStatus,
  updateAdminUser,
  updateAdminUserStatus
} from '@/api/adminRbacApi';
import type {
  AdminPermissionVO,
  AdminRoleRequest,
  AdminRoleVO,
  AdminUserCreateRequest,
  AdminUserUpdateRequest,
  AdminUserVO,
  PageVO
} from '@/api/types';

export const useAdminRbacStore = defineStore('admin-rbac', {
  state: () => ({
    adminPage: null as PageVO<AdminUserVO> | null,
    roles: [] as AdminRoleVO[],
    permissions: [] as AdminPermissionVO[],
    loading: false,
    saving: false,
    error: ''
  }),
  actions: {
    async loadAdmins(params: { page: number; size: number; keyword?: string; status?: number | null }) {
      this.loading = true;
      this.error = '';
      try {
        this.adminPage = await listAdminUsers(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '管理员加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadRoles() {
      this.roles = await listAdminRoles();
    },
    async loadPermissions() {
      this.permissions = await listAdminPermissions();
    },
    async saveAdmin(payload: AdminUserCreateRequest | AdminUserUpdateRequest, adminId?: string) {
      this.saving = true;
      try {
        return adminId
          ? await updateAdminUser(adminId, payload as AdminUserUpdateRequest)
          : await createAdminUser(payload as AdminUserCreateRequest);
      } finally {
        this.saving = false;
      }
    },
    async updateAdminStatus(adminId: string, status: number) {
      return updateAdminUserStatus(adminId, status);
    },
    async resetPassword(adminId: string, password: string) {
      return resetAdminUserPassword(adminId, password);
    },
    async saveRole(payload: AdminRoleRequest, roleId?: string) {
      this.saving = true;
      try {
        return roleId ? await updateAdminRole(roleId, payload) : await createAdminRole(payload);
      } finally {
        this.saving = false;
      }
    },
    async updateRoleStatus(roleId: string, status: number) {
      return updateAdminRoleStatus(roleId, status);
    }
  }
});
