<script setup lang="ts">
import { LoaderCircle, TicketCheck, TicketPercent } from 'lucide-vue-next';
import { computed, onMounted } from 'vue';

import { useCouponStore } from '@/stores/couponStore';
import { money } from '@/utils/format';

const couponStore = useCouponStore();

const claimedIds = computed(() => new Set(couponStore.myCoupons.map((coupon) => coupon.couponId)));

function statusText(useStatus: number) {
  if (useStatus === 1) {
    return '已使用';
  }
  return '未使用';
}

function formatTime(value?: string | null) {
  if (!value) {
    return '-';
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(parsed);
}

async function claim(couponId: number) {
  await couponStore.claim(couponId);
}

onMounted(() => {
  void couponStore.loadCoupons();
});
</script>

<template>
  <main class="buyer-page coupon-page">
    <section class="buyer-page-head">
      <div>
        <span class="eyebrow"><TicketCheck :size="18" /> 优惠券</span>
        <h1>领取与管理优惠券</h1>
        <p>优惠券数据来自后端营销服务，购物车结算时会按已领取且满足门槛的券进行抵扣。</p>
      </div>
      <button type="button" class="outline-action" :disabled="couponStore.loading" @click="couponStore.loadCoupons">
        <LoaderCircle v-if="couponStore.loading" :size="18" class="spin" />
        刷新
      </button>
    </section>

    <p v-if="couponStore.message" class="form-message success">{{ couponStore.message }}</p>
    <p v-if="couponStore.error" class="form-message error">{{ couponStore.error }}</p>

    <section class="coupon-grid-layout">
      <article class="coupon-panel">
        <header>
          <h2>可领取优惠券</h2>
          <span>{{ couponStore.availableCoupons.length }} 张</span>
        </header>
        <div v-if="couponStore.loading" class="inline-loading">
          <LoaderCircle :size="20" class="spin" />
          正在同步优惠券...
        </div>
        <div v-else-if="couponStore.availableCoupons.length === 0" class="empty-inline">
          当前暂无可领取优惠券。
        </div>
        <article v-for="coupon in couponStore.availableCoupons" v-else :key="coupon.couponId" class="coupon-card">
          <div>
            <TicketPercent :size="22" />
            <strong>{{ coupon.name }}</strong>
            <span>满 {{ money(coupon.minPoint) }} 可用</span>
          </div>
          <b>{{ money(coupon.amount) }}</b>
          <small>剩余 {{ coupon.stock }} 张 · 每人限领 {{ coupon.limitPerUser }} 张</small>
          <button
            type="button"
            class="primary-action"
            :disabled="couponStore.claiming || claimedIds.has(coupon.couponId)"
            @click="claim(coupon.couponId)"
          >
            <LoaderCircle v-if="couponStore.claiming" :size="18" class="spin" />
            {{ claimedIds.has(coupon.couponId) ? '已领取' : '立即领取' }}
          </button>
        </article>
      </article>

      <article class="coupon-panel">
        <header>
          <h2>我的优惠券</h2>
          <span>{{ couponStore.myCoupons.length }} 张</span>
        </header>
        <div v-if="couponStore.myCoupons.length === 0" class="empty-inline">
          暂无已领取优惠券，可以先从左侧领取。
        </div>
        <article v-for="coupon in couponStore.myCoupons" v-else :key="coupon.historyId" class="my-coupon-row">
          <div>
            <strong>{{ coupon.name }}</strong>
            <span>满 {{ money(coupon.minPoint) }} 减 {{ money(coupon.amount) }}</span>
            <small>有效期至 {{ formatTime(coupon.endTime) }}</small>
          </div>
          <em :class="{ used: coupon.useStatus !== 0 }">{{ statusText(coupon.useStatus) }}</em>
        </article>
      </article>
    </section>
  </main>
</template>
