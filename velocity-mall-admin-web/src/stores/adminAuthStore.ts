import { defineStore } from 'pinia';
import { loginAdmin } from '@/api/adminAuthApi';
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

export const useAdminAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    profile: readProfile(),
    loading: false,
    error: ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.profile?.username || state.profile?.realName || '管理员'
  },
  actions: {
    async login(username: string, password: string) {
      this.loading = true;
      this.error = '';
      try {
        const profile = await loginAdmin({ username, password });
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
    logout() {
      this.token = '';
      this.profile = null;
      localStorage.removeItem(ADMIN_TOKEN_KEY);
      localStorage.removeItem(ADMIN_PROFILE_KEY);
    }
  }
});
