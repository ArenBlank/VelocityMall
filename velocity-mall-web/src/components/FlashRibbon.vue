<script setup lang="ts">
import { Clock3, PackageCheck, ShieldCheck, Zap } from 'lucide-vue-next';
import { computed, onMounted, onUnmounted, ref } from 'vue';

import { countdownParts } from '@/utils/format';

const props = defineProps<{
  saleCount?: number;
  availableStock?: number;
  loading?: boolean;
  startTime?: string;
  endTime?: string;
}>();

const now = ref(Date.now());
let timer: number | null = null;

const startAt = computed(() => new Date(props.startTime || now.value).getTime());
const endAt = computed(() => new Date(props.endTime || now.value).getTime());
const beforeStart = computed(() => now.value < startAt.value);
const ended = computed(() => now.value > endAt.value);
const targetTime = computed(() => {
  if (beforeStart.value) {
    return props.startTime || new Date(now.value).toISOString();
  }
  return props.endTime || new Date(now.value).toISOString();
});
const countdownLabel = computed(() => {
  if (beforeStart.value) {
    return '距离活动开始';
  }
  if (ended.value) {
    return '本场已结束';
  }
  return '本场距结束';
});
const parts = computed(() => {
  now.value;
  return countdownParts(targetTime.value);
});

const soldPercent = computed(() => {
  const sold = props.saleCount ?? 0;
  const stock = props.availableStock ?? 0;
  const total = sold + stock;
  if (total <= 0) {
    return null;
  }
  return Math.min(99, Math.round((sold / total) * 100));
});

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onUnmounted(() => {
  if (timer !== null) {
    window.clearInterval(timer);
  }
});
</script>

<template>
  <section class="flash-ribbon">
    <div class="ribbon-title">
      <Zap :size="28" fill="currentColor" />
      限时秒杀
    </div>
    <div class="ribbon-countdown">
      <span>{{ countdownLabel }}</span>
      <strong>{{ parts[0] }}</strong>
      <i>:</i>
      <strong>{{ parts[1] }}</strong>
      <i>:</i>
      <strong>{{ parts[2] }}</strong>
    </div>
    <div class="ribbon-fact">
      <ShieldCheck :size="20" />
      下单后锁定资格
    </div>
    <div class="ribbon-fact">
      <Clock3 :size="20" />
      订单结果自动刷新
    </div>
    <div class="ribbon-stock">
      <PackageCheck :size="20" />
      {{ loading ? '库存同步中' : '库存已同步' }}
    </div>
    <div class="sold-progress">
      <span v-if="soldPercent !== null">已售 {{ soldPercent }}%</span>
      <span v-else>销量同步中</span>
      <b><i :style="{ width: soldPercent !== null ? `${soldPercent}%` : '0%' }" /></b>
    </div>
  </section>
</template>
