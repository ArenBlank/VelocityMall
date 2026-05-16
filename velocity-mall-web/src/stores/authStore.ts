import { defineStore } from 'pinia';

import { getCurrentUser, login as loginApi, register as registerApi } from '@/api/authApi';
import { clearAuthStorage, TOKEN_KEY, USER_KEY } from '@/api/http';
import type { UserInfoVO } from '@/api/types';

interface AuthState {
  token: string;
  user: UserInfoVO | null;
  loading: boolean;
}

function readUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserInfoVO;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    loading: false
  }),
  actions: {
    async login(username: string, password: string) {
      this.loading = true;
      try {
        const result = await loginApi({ username, password });
        const user = result.user || { id: 0, username };
        this.token = result.token;
        this.user = user;
        localStorage.setItem(TOKEN_KEY, result.token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      } finally {
        this.loading = false;
      }
    },
    async register(username: string, password: string) {
      this.loading = true;
      try {
        await registerApi({ username, password });
      } finally {
        this.loading = false;
      }
    },
    async refreshMe() {
      if (!this.token) {
        return;
      }
      this.loading = true;
      try {
        const user = await getCurrentUser();
        this.user = user;
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      } catch (error) {
        this.logout();
        throw error;
      } finally {
        this.loading = false;
      }
    },
    logout() {
      this.token = '';
      this.user = null;
      clearAuthStorage();
    }
  }
});
