import { request } from './http';
import type { OrderDetailVO, OrderVO, PageVO, SeckillResultVO } from './types';

export function listOrders(params: { page?: number; size?: number; status?: number | null } = {}) {
  const query: Record<string, number> = {
    page: params.page ?? 1,
    size: params.size ?? 10
  };
  if (params.status !== null && params.status !== undefined) {
    query.status = params.status;
  }
  return request<PageVO<OrderDetailVO>>({
    url: '/api/v1/orders',
    method: 'GET',
    params: query
  });
}

export function getOrder(orderSn: string) {
  return request<OrderDetailVO>({
    url: `/api/v1/orders/${orderSn}`,
    method: 'GET'
  });
}

export function getSeckillResult(skuId: number) {
  return request<SeckillResultVO>({
    url: `/api/v1/orders/seckill/result/${skuId}`,
    method: 'GET'
  });
}

export function mockPay(orderSn: string, payType = 1) {
  return request<void>({
    url: '/api/v1/orders/pay/mock',
    method: 'POST',
    params: { orderSn, payType }
  });
}

export function submitOrder(data: { skuIds: number[]; addressId: number; couponHistoryId?: number | null }) {
  return request<OrderVO>({
    url: '/api/v1/orders',
    method: 'POST',
    data
  });
}

export function cancelOrder(orderSn: string) {
  return request<void>({
    url: `/api/v1/orders/${orderSn}/cancel`,
    method: 'PUT'
  });
}

export function mockRefund(orderSn: string) {
  return request<void>({
    url: `/api/v1/orders/${orderSn}/refund/mock`,
    method: 'POST'
  });
}

export function confirmReceipt(orderSn: string) {
  return request<void>({
    url: `/api/v1/orders/${orderSn}/confirm-receipt`,
    method: 'PUT'
  });
}
