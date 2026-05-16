import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios';

import type { ApiEnvelope } from './types';

export const TOKEN_KEY = 'velocitymall_token';
export const USER_KEY = 'velocitymall_user';

export class ApiError extends Error {
  code: number;
  status?: number;

  constructor(code: number, message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  validateStatus: () => true
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await client.request<ApiEnvelope<T>>(config);
    return unwrap(response);
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (error instanceof AxiosError && error.response) {
      throw toApiError(error.response as AxiosResponse<ApiEnvelope<unknown>>);
    }
    throw new ApiError(0, '网络连接失败，请稍后重试');
  }
}

function unwrap<T>(response: AxiosResponse<ApiEnvelope<T>>) {
  const body = response.data;
  if (response.status === 429 || body?.code === 42900) {
    throw new ApiError(42900, body?.message || '当前抢购人数过多，请稍后再试', response.status);
  }
  if (response.status === 401 || body?.code === 40100) {
    clearAuthStorage();
    throw new ApiError(40100, body?.message || '登录已失效，请重新登录', response.status);
  }
  if (body?.code === 20000) {
    return body.data;
  }
  throw toApiError(response as AxiosResponse<ApiEnvelope<unknown>>);
}

function toApiError(response: AxiosResponse<ApiEnvelope<unknown>>) {
  const body = response.data;
  return new ApiError(body?.code ?? response.status, body?.message || '请求失败', response.status);
}

export function clearAuthStorage() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
