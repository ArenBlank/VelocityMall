import { defineStore } from 'pinia';

import { getSeckillActivityBySkuId, listSeckillActivities } from '@/api/seckillApi';
import type { SeckillActivityVO } from '@/api/types';

interface SeckillActivityState {
  activities: SeckillActivityVO[];
  bySkuId: Record<number, SeckillActivityVO>;
  loading: boolean;
  error: string;
}

export const useSeckillActivityStore = defineStore('seckillActivity', {
  state: (): SeckillActivityState => ({
    activities: [],
    bySkuId: {},
    loading: false,
    error: ''
  }),
  getters: {
    mainActivity: (state) => state.activities[0] || null,
    activityBySkuId: (state) => (skuId: number) =>
      state.bySkuId[skuId] || state.activities.find((activity) => activity.skuId === skuId) || null
  },
  actions: {
    async loadActivities() {
      this.loading = true;
      this.error = '';
      try {
        const activities = await listSeckillActivities();
        this.activities = activities || [];
        this.bySkuId = Object.fromEntries(this.activities.map((activity) => [activity.skuId, activity]));
      } catch (error) {
        this.error = error instanceof Error ? error.message : '秒杀活动加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadActivity(skuId: number) {
      if (this.bySkuId[skuId]) {
        return this.bySkuId[skuId];
      }
      this.loading = true;
      this.error = '';
      try {
        const activity = await getSeckillActivityBySkuId(skuId);
        this.bySkuId = { ...this.bySkuId, [skuId]: activity };
        if (!this.activities.some((item) => item.skuId === skuId)) {
          this.activities = [...this.activities, activity];
        }
        return activity;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '秒杀活动加载失败';
        return null;
      } finally {
        this.loading = false;
      }
    }
  }
});
