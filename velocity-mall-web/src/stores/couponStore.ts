import { defineStore } from 'pinia';

import { claimCoupon, listAvailableCoupons, listMyCoupons } from '@/api/couponApi';
import type { CouponVO, PageVO, UserCouponVO } from '@/api/types';

interface CouponState {
  availablePage: PageVO<CouponVO> | null;
  minePage: PageVO<UserCouponVO> | null;
  loading: boolean;
  claiming: boolean;
  message: string;
  error: string;
}

export const useCouponStore = defineStore('coupon', {
  state: (): CouponState => ({
    availablePage: null,
    minePage: null,
    loading: false,
    claiming: false,
    message: '',
    error: ''
  }),
  getters: {
    availableCoupons(state) {
      return state.availablePage?.records || [];
    },
    myCoupons(state) {
      return state.minePage?.records || [];
    },
    unusedCoupons(state) {
      return (state.minePage?.records || []).filter((coupon) => coupon.useStatus === 0);
    }
  },
  actions: {
    async loadCoupons() {
      this.loading = true;
      this.error = '';
      try {
        const [availablePage, minePage] = await Promise.all([
          listAvailableCoupons({ page: 1, size: 20 }),
          listMyCoupons({ page: 1, size: 50 })
        ]);
        this.availablePage = availablePage;
        this.minePage = minePage;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '优惠券加载失败';
      } finally {
        this.loading = false;
      }
    },
    async claim(couponId: number) {
      this.claiming = true;
      this.message = '';
      this.error = '';
      try {
        await claimCoupon(couponId);
        this.message = '领取成功，结算时可使用已领取优惠券';
        await this.loadCoupons();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '优惠券领取失败';
        throw error;
      } finally {
        this.claiming = false;
      }
    }
  }
});
