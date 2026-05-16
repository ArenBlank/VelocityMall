import { http } from './http';
import type { AdminOrderVO, PageVO } from './types';

export function listAdminOrders(params: {
  page: number;
  size: number;
  status?: number | null;
  orderSn?: string;
  userId?: number | null;
  orderType?: number | null;
}) {
  return http.get<PageVO<AdminOrderVO>, PageVO<AdminOrderVO>>('/api/v1/admin/orders', { params });
}

export function getAdminOrder(orderSn: string) {
  return http.get<AdminOrderVO, AdminOrderVO>(`/api/v1/admin/orders/${orderSn}`);
}

export function deliverAdminOrder(orderSn: string, deliveryCompany: string, deliverySn: string) {
  return http.post<void, void>(`/api/v1/admin/orders/${orderSn}/deliver`, null, {
    params: { deliveryCompany, deliverySn }
  });
}
