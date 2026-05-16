import { defineStore } from 'pinia';
import {
  createAdminSeckillActivity,
  listAdminSeckillActivities,
  preheatAdminSeckillActivity,
  updateAdminSeckillActivity,
  updateAdminSeckillActivityStatus
} from '@/api/adminSeckillApi';
import type { AdminSeckillActivityRequest, AdminSeckillActivityVO, PageVO } from '@/api/types';

export const useAdminSeckillStore = defineStore('admin-seckill', {
  state: () => ({
    page: null as PageVO<AdminSeckillActivityVO> | null,
    loading: false,
    saving: false,
    error: ''
  }),
  actions: {
    async load(params: { page: number; size: number; state?: string; skuId?: number | null }) {
      this.loading = true;
      this.error = '';
      try {
        this.page = await listAdminSeckillActivities(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '秒杀活动加载失败';
      } finally {
        this.loading = false;
      }
    },
    async save(payload: AdminSeckillActivityRequest, id?: number) {
      this.saving = true;
      try {
        return id ? await updateAdminSeckillActivity(id, payload) : await createAdminSeckillActivity(payload);
      } finally {
        this.saving = false;
      }
    },
    async updateStatus(id: number, status: number) {
      return updateAdminSeckillActivityStatus(id, status);
    },
    async preheat(id: number) {
      return preheatAdminSeckillActivity(id);
    }
  }
});
