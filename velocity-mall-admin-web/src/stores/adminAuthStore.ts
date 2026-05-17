import { defineStore } from 'pinia';
import { getAdminMe, loginAdmin } from '@/api/adminAuthApi';
import { ADMIN_PROFILE_KEY, ADMIN_TOKEN_KEY } from '@/api/http';
import type { AdminLoginVO } from '@/api/types';

function readProfile(): AdminLoginVO | null {
  const raw = localStorage.getItem(ADMIN_PROFILE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AdminLoginVO;
  } catch {
    localStorage.removeItem(ADMIN_PROFILE_KEY);
    return null;
  }
}

function normalizeProfile(profile: AdminLoginVO): AdminLoginVO {
  return {
    ...profile,
    roles: profile.roles || [],
    permissions: profile.permissions || []
  };
}

export const useAdminAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    profile: readProfile(),
    loading: false,
    error: ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.profile?.realName || state.profile?.username || '管理员',
    roles: (state) => state.profile?.roles || [],
    permissions: (state) => state.profile?.permissions || [],
    hasPermission: (state) => (code: string) => Boolean(state.profile?.permissions?.includes(code)),
    hasAnyPermission: (state) => (codes: string[]) => codes.some((code) => state.profile?.permissions?.includes(code))
  },
  actions: {
    async login(username: string, password: string) {
      this.loading = true;
      this.error = '';
      try {
        const profile = normalizeProfile(await loginAdmin({ username, password }));
        this.token = profile.token;
        this.profile = profile;
        localStorage.setItem(ADMIN_TOKEN_KEY, profile.token);
        localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile));
      } catch (error) {
        this.error = error instanceof Error ? error.message : '登录失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async refreshProfile() {
      if (!this.token) {
        return null;
      }
      const profile = normalizeProfile({
        ...(await getAdminMe()),
        token: this.token
      });
      this.profile = profile;
      localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile));
      return profile;
    },
    logout() {
      this.token = '';
      this.profile = null;
      localStorage.removeItem(ADMIN_TOKEN_KEY);
      localStorage.removeItem(ADMIN_PROFILE_KEY);
    }
  }
});
