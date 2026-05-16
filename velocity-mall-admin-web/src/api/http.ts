import axios, { AxiosError } from 'axios';

export const ADMIN_TOKEN_KEY = 'velocitymall_admin_token';
export const ADMIN_PROFILE_KEY = 'velocitymall_admin_profile';

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiBusinessError extends Error {
  code: number;
  status?: number;

  constructor(message: string, code: number, status?: number) {
    super(message);
    this.name = 'ApiBusinessError';
    this.code = code;
    this.status = status;
  }
}

export const http = axios.create({
  baseURL: '',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(ADMIN_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function logoutToLogin() {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
  localStorage.removeItem(ADMIN_PROFILE_KEY);
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
}

http.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>;
    if (envelope && typeof envelope.code === 'number') {
      if (envelope.code === 20000) {
        return envelope.data as never;
      }
      if (envelope.code === 40100) {
        logoutToLogin();
      }
      throw new ApiBusinessError(envelope.message || '请求失败', envelope.code, response.status);
    }
    return response.data;
  },
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const code = error.response?.data?.code ?? error.response?.status ?? 50000;
    const message = error.response?.data?.message || error.message || '网络请求失败';
    if (error.response?.status === 401 || code === 40100) {
      logoutToLogin();
    }
    return Promise.reject(new ApiBusinessError(message, code, error.response?.status));
  }
);
