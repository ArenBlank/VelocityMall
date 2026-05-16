import { defineStore } from 'pinia';

import { cancelOrder, confirmReceipt, getOrder, listOrders, mockPay, mockRefund, submitOrder } from '@/api/orderApi';
import type { OrderDetailVO, OrderVO, PageVO } from '@/api/types';

interface OrderState {
  page: PageVO<OrderDetailVO> | null;
  current: OrderDetailVO | null;
  loading: boolean;
  error: string;
  status: number | null;
  pageNo: number;
  pageSize: number;
}

export const useOrderStore = defineStore('order', {
  state: (): OrderState => ({
    page: null,
    current: null,
    loading: false,
    error: '',
    status: null,
    pageNo: 1,
    pageSize: 10
  }),
  actions: {
    async loadOrders(status?: number | null, page?: number, size?: number) {
      this.loading = true;
      this.error = '';
      const nextStatus = status === undefined ? this.status : status;
      const nextPage = page ?? this.pageNo;
      const nextSize = size ?? this.pageSize;
      this.status = nextStatus;
      this.pageNo = nextPage;
      this.pageSize = nextSize;
      try {
        this.page = await listOrders({ page: nextPage, size: nextSize, status: nextStatus });
      } catch (error) {
        this.error = error instanceof Error ? error.message : '订单列表加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadOrder(orderSn: string) {
      this.loading = true;
      this.error = '';
      this.current = null;
      try {
        this.current = await getOrder(orderSn);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '订单详情加载失败';
      } finally {
        this.loading = false;
      }
    },
    async pay(orderSn: string) {
      await mockPay(orderSn, 1);
      await this.loadOrder(orderSn);
    },
    async submitNormalOrder(skuIds: number[], addressId: number, couponHistoryId?: number | null): Promise<OrderVO> {
      return submitOrder({ skuIds, addressId, couponHistoryId });
    },
    async cancel(orderSn: string) {
      await cancelOrder(orderSn);
      await this.loadOrders(this.status, this.pageNo, this.pageSize);
    },
    async refund(orderSn: string) {
      await mockRefund(orderSn);
      await this.loadOrders(this.status, this.pageNo, this.pageSize);
    },
    async confirm(orderSn: string) {
      await confirmReceipt(orderSn);
      await this.loadOrders(this.status, this.pageNo, this.pageSize);
    }
  }
});
