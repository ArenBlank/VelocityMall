import { defineStore } from 'pinia';

import { ApiError } from '@/api/http';
import { getSeckillResult } from '@/api/orderApi';
import { executeSeckill } from '@/api/seckillApi';

export type SeckillStatus =
  | 'READY'
  | 'SUBMITTING'
  | 'QUEUED'
  | 'SUCCESS'
  | 'SOLD_OUT'
  | 'DUPLICATE'
  | 'LIMITED'
  | 'FAILED';

interface SeckillState {
  status: SeckillStatus;
  message: string;
  orderSn: string;
  timer: number | null;
  submittedAt: number | null;
  queuedAt: number | null;
  orderReadyAt: number | null;
}

export const useSeckillStore = defineStore('seckill', {
  state: (): SeckillState => ({
    status: 'READY',
    message: '立即抢购',
    orderSn: '',
    timer: null,
    submittedAt: null,
    queuedAt: null,
    orderReadyAt: null
  }),
  getters: {
    locked: (state) => ['SUBMITTING', 'QUEUED', 'SUCCESS'].includes(state.status)
  },
  actions: {
    async submit(skuId: number, onOrderFound?: (orderSn: string) => void) {
      if (this.locked) {
        return;
      }
      this.status = 'SUBMITTING';
      this.message = '正在提交';
      this.submittedAt = Date.now();
      this.queuedAt = null;
      this.orderReadyAt = null;
      try {
        await executeSeckill(skuId);
        this.status = 'QUEUED';
        this.message = '排队生成订单中';
        this.queuedAt = Date.now();
        this.startOrderPolling(skuId, onOrderFound);
      } catch (error) {
        const classified = classifyFailure(error);
        this.status = classified.status;
        this.message = classified.message;
      }
    },
    startOrderPolling(skuId: number, onOrderFound?: (orderSn: string) => void) {
      this.clearTimer();
      const startedAt = Date.now();
      const poll = async () => {
        if (Date.now() - startedAt > 30000) {
          this.clearTimer();
          this.status = 'FAILED';
          this.message = '排队超时，请到我的订单确认';
          return;
        }
        try {
          const result = await getSeckillResult(skuId);
          if (result.state === 'SUCCESS' && result.orderSn) {
            this.clearTimer();
            this.status = 'SUCCESS';
            this.orderSn = result.orderSn;
            this.message = result.message || '订单已生成，去支付';
            this.orderReadyAt = Date.now();
            onOrderFound?.(result.orderSn);
            return;
          }
          if (result.state === 'FAILED') {
            this.clearTimer();
            this.status = 'FAILED';
            this.message = result.message || '订单生成失败，请稍后重试';
          }
        } catch {
          // Keep queue feedback visible; the user can still check orders manually.
        }
      };
      void poll();
      this.timer = window.setInterval(poll, 1000);
    },
    clearTimer() {
      if (this.timer !== null) {
        window.clearInterval(this.timer);
        this.timer = null;
      }
    },
    reset() {
      this.clearTimer();
      this.status = 'READY';
      this.message = '立即抢购';
      this.orderSn = '';
      this.submittedAt = null;
      this.queuedAt = null;
      this.orderReadyAt = null;
    }
  }
});

function classifyFailure(error: unknown): { status: SeckillStatus; message: string } {
  const message = error instanceof Error ? error.message : '请求失败，请稍后重试';
  if (error instanceof ApiError && (error.status === 429 || error.code === 42900)) {
    return { status: 'LIMITED', message: '活动火爆，请稍后重试' };
  }
  if (error instanceof ApiError && error.code === 40100) {
    return { status: 'FAILED', message: '请先登录再参与秒杀' };
  }
  if (error instanceof ApiError && error.code === 50001) {
    if (/重复|已抢过|请勿/.test(message)) {
      return { status: 'DUPLICATE', message: '请勿重复抢购' };
    }
    if (/抢光|库存不足|售罄/.test(message)) {
      return { status: 'SOLD_OUT', message: '商品已抢光' };
    }
  }
  return { status: 'FAILED', message };
}
