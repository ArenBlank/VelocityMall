import { defineStore } from 'pinia';
import { rebuildAdminSkuIndex } from '@/api/adminSystemApi';
import type { AdminRebuildIndexVO } from '@/api/types';

export const useAdminSystemStore = defineStore('admin-system', {
  state: () => ({
    result: null as AdminRebuildIndexVO | null,
    loading: false,
    error: ''
  }),
  actions: {
    async rebuildSkuIndex() {
      this.loading = true;
      this.error = '';
      try {
        this.result = await rebuildAdminSkuIndex();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '重建索引失败';
        throw error;
      } finally {
        this.loading = false;
      }
    }
  }
});
