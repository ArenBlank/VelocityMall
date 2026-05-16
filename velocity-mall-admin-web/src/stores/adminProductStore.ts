import { defineStore } from 'pinia';
import {
  createAdminSku,
  createAdminSpu,
  getAdminSpu,
  listAdminSpus,
  updateAdminSku,
  updateAdminSpu,
  updateAdminSpuStatus,
  uploadAdminSkuCover
} from '@/api/adminProductApi';
import type { AdminSkuRequest, AdminSpuRequest, AdminSpuVO, PageVO } from '@/api/types';

export const useAdminProductStore = defineStore('admin-product', {
  state: () => ({
    page: null as PageVO<AdminSpuVO> | null,
    current: null as AdminSpuVO | null,
    loading: false,
    saving: false,
    error: ''
  }),
  actions: {
    async load(params: { page: number; size: number; keyword?: string; status?: number | null }) {
      this.loading = true;
      this.error = '';
      try {
        this.page = await listAdminSpus(params);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '商品加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadDetail(spuId: number) {
      this.loading = true;
      this.current = null;
      try {
        this.current = await getAdminSpu(spuId);
      } finally {
        this.loading = false;
      }
    },
    async saveSpu(payload: AdminSpuRequest, spuId?: number) {
      this.saving = true;
      try {
        const saved = spuId ? await updateAdminSpu(spuId, payload) : await createAdminSpu(payload);
        this.current = saved;
        return saved;
      } finally {
        this.saving = false;
      }
    },
    async setSpuStatus(spuId: number, publish: boolean) {
      await updateAdminSpuStatus(spuId, publish ? 'publish' : 'unpublish');
    },
    async saveSku(payload: AdminSkuRequest, skuId?: number) {
      this.saving = true;
      try {
        return skuId ? await updateAdminSku(skuId, payload) : await createAdminSku(payload);
      } finally {
        this.saving = false;
      }
    },
    async uploadCover(skuId: number, file: File) {
      return uploadAdminSkuCover(skuId, file);
    }
  }
});
