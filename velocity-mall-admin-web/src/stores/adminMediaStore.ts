import { defineStore } from 'pinia';
import { uploadAdminSkuCover } from '@/api/adminMediaApi';
import type { FileUploadVO } from '@/api/types';

export const useAdminMediaStore = defineStore('admin-media', {
  state: () => ({
    lastUpload: null as FileUploadVO | null,
    loading: false,
    error: ''
  }),
  actions: {
    async uploadSkuCover(skuId: number, file: File) {
      this.loading = true;
      this.error = '';
      try {
        this.lastUpload = await uploadAdminSkuCover(skuId, file);
        return this.lastUpload;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '上传失败';
        throw error;
      } finally {
        this.loading = false;
      }
    }
  }
});
