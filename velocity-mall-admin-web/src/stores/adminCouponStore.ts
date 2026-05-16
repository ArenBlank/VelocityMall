import { defineStore } from 'pinia';
import { createAdminCoupon, listAdminCoupons, updateAdminCoupon, updateAdminCouponStatus } from '@/api/adminCouponApi';
import type { AdminCouponRequest, AdminCouponVO, PageVO } from '@/api/types';

export const useAdminCouponStore = defineStore('admin-coupon', {
  state: () => ({
    page: null as PageVO<AdminCouponVO> | null,
    loading: false,
    saving: false,
    error: ''
  }),
  actions: {
    async load(params: { page: number; size: number; status?: number | null }) {
      this.loading = true;
      this.error = '';
      try {
        this.page = await listAdminCoupons(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '优惠券加载失败';
      } finally {
        this.loading = false;
      }
    },
    async save(payload: AdminCouponRequest, id?: number) {
      this.saving = true;
      try {
        return id ? await updateAdminCoupon(id, payload) : await createAdminCoupon(payload);
      } finally {
        this.saving = false;
      }
    },
    async updateStatus(id: number, status: number) {
      return updateAdminCouponStatus(id, status);
    }
  }
});
