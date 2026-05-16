import { request } from './http';
import type { LoginVO, UserInfoVO } from './types';

export interface AuthPayload {
  username: string;
  password: string;
}

export function login(payload: AuthPayload) {
  return request<LoginVO>({
    url: '/api/v1/users/login',
    method: 'POST',
    data: payload
  });
}

export function register(payload: AuthPayload) {
  return request<void>({
    url: '/api/v1/users/register',
    method: 'POST',
    data: payload
  });
}

export function getCurrentUser() {
  return request<UserInfoVO>({
    url: '/api/v1/users/me',
    method: 'GET'
  });
}
