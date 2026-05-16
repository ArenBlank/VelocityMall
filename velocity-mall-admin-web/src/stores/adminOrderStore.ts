import { defineStore } from 'pinia';
import { deliverAdminOrder, getAdminOrder, listAdminOrders } from '@/api/adminOrderApi';
import type { AdminOrderVO, PageVO } from '@/api/types';

export const useAdminOrderStore = defineStore('admin-order', {
  state: () => ({
    page: null as PageVO<AdminOrderVO> | null,
    current: null as AdminOrderVO | null,
    loading: false,
    error: ''
  }),
  actions: {
    async load(params: { page: number; size: number; status?: number | null; orderSn?: string; userId?: number | null; orderType?: number | null }) {
      this.loading = true;
      this.error = '';
      try {
        this.page = await listAdminOrders(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '订单加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadDetail(orderSn: string) {
      this.loading = true;
      this.current = null;
      try {
        this.current = await getAdminOrder(orderSn);
      } finally {
        this.loading = false;
      }
    },
    async deliver(orderSn: string, deliveryCompany: string, deliverySn: string) {
      await deliverAdminOrder(orderSn, deliveryCompany, deliverySn);
    }
  }
});
