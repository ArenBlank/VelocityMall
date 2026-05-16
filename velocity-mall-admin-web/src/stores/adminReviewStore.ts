import { defineStore } from 'pinia';
import { deleteAdminReview, listAdminReviews } from '@/api/adminReviewApi';
import type { AdminReviewVO, PageVO } from '@/api/types';

export const useAdminReviewStore = defineStore('admin-review', {
  state: () => ({
    page: null as PageVO<AdminReviewVO> | null,
    loading: false,
    error: ''
  }),
  actions: {
    async load(params: { page: number; size: number; spuId?: number | null; keyword?: string }) {
      this.loading = true;
      this.error = '';
      try {
        this.page = await listAdminReviews(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '评价加载失败';
      } finally {
        this.loading = false;
      }
    },
    async remove(id: number) {
      await deleteAdminReview(id);
    }
  }
});
