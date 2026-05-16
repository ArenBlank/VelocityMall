import { defineStore } from 'pinia';

import { addCartItem, deleteCartItem, listCartItems } from '@/api/cartApi';
import { getSku } from '@/api/productApi';
import { submitOrder } from '@/api/orderApi';
import type { CartItemVO, OrderVO } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { normalizeProductImage } from '@/utils/images';

interface CartState {
  items: CartItemVO[];
  skuImages: Record<number, string>;
  loading: boolean;
  submitting: boolean;
  error: string;
}

export const useCartStore = defineStore('cart', {
  state: (): CartState => ({
    items: [],
    skuImages: {},
    loading: false,
    submitting: false,
    error: ''
  }),
  getters: {
    totalQuantity(state) {
      return state.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
    },
    totalAmount(state) {
      return state.items.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0);
    }
  },
  actions: {
    imageFor(skuId: number) {
      return this.skuImages[skuId] || pickFallbackCover(skuId);
    },
    async loadItems() {
      this.loading = true;
      this.error = '';
      try {
        this.items = await listCartItems();
        await Promise.all(
          this.items.map(async (item) => {
            if (this.skuImages[item.skuId]) {
              return;
            }
            try {
              const sku = await getSku(item.skuId);
              this.skuImages[item.skuId] = normalizeProductImage(sku.coverImg, pickFallbackCover(item.skuId));
            } catch {
              this.skuImages[item.skuId] = pickFallbackCover(item.skuId);
            }
          })
        );
      } catch (error) {
        this.error = error instanceof Error ? error.message : '购物车加载失败';
      } finally {
        this.loading = false;
      }
    },
    async addItem(skuId: number, quantity = 1) {
      this.error = '';
      await addCartItem({ skuId, quantity });
      await this.loadItems();
    },
    async removeItem(skuId: number) {
      this.error = '';
      await deleteCartItem(skuId);
      await this.loadItems();
    },
    async submitSelected(skuIds: number[], addressId: number, couponHistoryId?: number | null): Promise<OrderVO> {
      this.submitting = true;
      this.error = '';
      try {
        const order = await submitOrder({ skuIds, addressId, couponHistoryId });
        await this.loadItems();
        return order;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '订单提交失败';
        throw error;
      } finally {
        this.submitting = false;
      }
    }
  }
});
