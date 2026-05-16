import { request } from './http';
import type { CartItemVO } from './types';

export interface CartItemPayload {
  skuId: number;
  quantity: number;
}

export function addCartItem(data: CartItemPayload) {
  return request<void>({
    url: '/api/v1/carts/items',
    method: 'POST',
    data
  });
}

export function listCartItems() {
  return request<CartItemVO[]>({
    url: '/api/v1/carts/items',
    method: 'GET'
  });
}

export function deleteCartItem(skuId: number) {
  return request<void>({
    url: `/api/v1/carts/items/${skuId}`,
    method: 'DELETE'
  });
}
