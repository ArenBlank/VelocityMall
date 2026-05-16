import { request } from './http';
import type { AddressVO } from './types';

export interface AddressPayload {
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  region: string;
  detailAddress: string;
  isDefault: boolean | number;
}

export function listAddresses() {
  return request<AddressVO[]>({
    url: '/api/v1/users/addresses',
    method: 'GET'
  });
}

export function createAddress(data: AddressPayload) {
  return request<void>({
    url: '/api/v1/users/addresses',
    method: 'POST',
    data
  });
}

export function updateAddress(id: number, data: AddressPayload) {
  return request<void>({
    url: `/api/v1/users/addresses/${id}`,
    method: 'PUT',
    data
  });
}

export function deleteAddress(id: number) {
  return request<void>({
    url: `/api/v1/users/addresses/${id}`,
    method: 'DELETE'
  });
}
