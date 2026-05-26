<script setup lang="ts">
import { AlertTriangle, Clock3, LoaderCircle, PackageSearch, ShieldCheck, Zap } from 'lucide-vue-next';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import type { SearchSkuVO, SeckillActivityState, SeckillActivityVO } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { useProductStore } from '@/stores/productStore';
import { useSeckillActivityStore } from '@/stores/seckillActivityStore';
import { countdownParts, money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

interface SeckillListItem {
  skuId: number;
  displayName: string;
  displayImage: string;
  seckillPrice: number;
  originalPrice: number;
  remainingStock: number;
  seckillStock: number;
  saleCount: number;
  startTime: string;
  endTime: string;
  state: SeckillActivityState;
}

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();
const seckillActivityStore = useSeckillActivityStore();
const now = ref(Date.now());
let countdownTimer: number | null = null;

const keyword = computed(() => String(route.query.keyword || '').trim());
const recordMap = computed(() => new Map((productStore.searchPage?.records || []).map((record) => [record.skuId, record])));
const seckillItems = computed<SeckillListItem[]>(() => {
  const normalizedKeyword = keyword.value.toLowerCase();
  return seckillActivityStore.activities
    .map((activity) => toSeckillItem(activity, recordMap.value.get(activity.skuId)))
    .filter((item) => {
      if (!normalizedKeyword) {
        return true;
      }
      return `${item.displayName} ${item.skuId}`.toLowerCase().includes(normalizedKeyword);
    });
});
const loading = computed(() => seckillActivityStore.loading || productStore.loading);
const error = computed(() => seckillActivityStore.error || productStore.error);

function toSeckillItem(activity: SeckillActivityVO, record?: SearchSkuVO): SeckillListItem {
  return {
    skuId: activity.skuId,
    displayName: record?.skuName || activity.activityName,
    displayImage: normalizeProductImage(record?.skuPic, pickFallbackCover(activity.skuId)),
    seckillPrice: Number(activity.seckillPrice ?? 0),
    originalPrice: Number(record?.price ?? activity.originalPrice ?? 0),
    remainingStock: Number(activity.remainingStock ?? 0),
    seckillStock: Number(activity.seckillStock ?? 0),
    saleCount: Number(record?.saleCount ?? 0),
    startTime: activity.startTime,
    endTime: activity.endTime,
    state: activity.state
  };
}

function statusLabel(state: SeckillActivityState) {
  const labels: Record<SeckillActivityState, string> = {
    ACTIVE: '秒杀中',
    NOT_STARTED: '未开始',
    ENDED: '已结束',
    DISABLED: '已停用'
  };
  return labels[state] || '秒杀活动';
}

function statusClass(state: SeckillActivityState) {
  return `state-${state.toLowerCase().replace('_', '-')}`;
}

function countdownLabel(item: SeckillListItem) {
  if (item.state === 'NOT_STARTED') {
    return '距开始';
  }
  if (item.state === 'ENDED') {
    return '已结束';
  }
  return '距结束';
}

function countdownFor(item: SeckillListItem) {
  now.value;
  const target = item.state === 'NOT_STARTED' ? item.startTime : item.endTime;
  return countdownParts(target || new Date(now.value).toISOString());
}

function stockText(item: SeckillListItem) {
  if (item.state === 'NOT_STARTED') {
    return `即将开抢，秒杀库存 ${item.remainingStock}`;
  }
  if (item.state === 'ACTIVE') {
    return `可抢库存 ${item.remainingStock}`;
  }
  return `秒杀库存 ${item.remainingStock}`;
}

function actionText(state: SeckillActivityState) {
  if (state === 'ACTIVE') {
    return '进入抢购';
  }
  if (state === 'NOT_STARTED') {
    return '查看活动';
  }
  return '查看详情';
}

function clearSearch() {
  void router.push({ name: 'seckill-zone' });
}

function loadData() {
  void Promise.all([
    seckillActivityStore.loadActivities(),
    productStore.loadSearchCatalog('', 'sale_desc', 1, 50)
  ]);
}

onMounted(() => {
  loadData();
  countdownTimer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});
watch(keyword, () => {
  if (seckillActivityStore.activities.length === 0) {
    loadData();
  }
});
onUnmounted(() => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
  }
});
</script>

<template>
  <main class="home-page catalog-page seckill-zone-page">
    <section class="catalog-page-banner seckill-zone-banner">
      <div>
        <span class="page-kicker"><Zap :size="18" fill="currentColor" /> 秒杀专区</span>
        <h1>限时秒杀</h1>
        <p>展示全部未结束秒杀活动，未开始的商品也会出现在这里。</p>
      </div>
      <div class="banner-deco seckill-deco" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
    </section>

    <section class="home-catalog">
      <div class="catalog-real-head">
        <div>
          <h2>{{ keyword ? '秒杀搜索结果' : '秒杀商品' }}</h2>
          <p>{{ keyword ? `关键词：${keyword}` : '活动未开始时可先查看详情，开始后再提交抢购。' }}</p>
        </div>
        <div class="catalog-controls">
          <button v-if="keyword" type="button" @click="clearSearch">查看全部秒杀</button>
        </div>
      </div>

      <div v-if="loading" class="catalog-loading">
        <LoaderCircle :size="24" class="spin" />
        正在加载秒杀活动…
      </div>

      <div v-else-if="seckillItems.length > 0" class="product-grid seckill-product-grid">
        <article v-for="item in seckillItems" :key="item.skuId" class="sale-card seckill-sale-card">
          <div class="card-badges">
            <span :class="statusClass(item.state)">{{ statusLabel(item.state) }}</span>
            <em>库存 {{ item.remainingStock }}</em>
          </div>
          <RouterLink class="card-image" :to="`/seckill/${item.skuId}`">
            <img :src="item.displayImage" :alt="item.displayName" loading="lazy" />
          </RouterLink>
          <h2>{{ item.displayName }}</h2>
          <p>SKU {{ item.skuId }}</p>
          <small>销量 {{ item.saleCount.toLocaleString('zh-CN') }} 件</small>
          <div class="card-price">
            <strong>¥ {{ item.seckillPrice }}</strong>
            <del v-if="item.originalPrice > item.seckillPrice">{{ money(item.originalPrice) }}</del>
          </div>
          <p class="stock-hint" :class="item.state === 'ACTIVE' ? 'safe' : 'warning'">
            <ShieldCheck v-if="item.state === 'ACTIVE'" :size="16" />
            <Clock3 v-else :size="16" />
            {{ stockText(item) }}
          </p>
          <p class="seckill-countdown-line">
            {{ countdownLabel(item) }}
            <strong>{{ countdownFor(item)[0] }}:{{ countdownFor(item)[1] }}:{{ countdownFor(item)[2] }}</strong>
          </p>
          <RouterLink class="detail-button" :to="`/seckill/${item.skuId}`">{{ actionText(item.state) }}</RouterLink>
        </article>
      </div>

      <div v-else-if="error" class="orders-error">
        <AlertTriangle :size="28" />
        <strong>{{ error }}</strong>
        <button type="button" @click="loadData">重试</button>
      </div>

      <div v-else class="orders-empty">
        <PackageSearch :size="34" />
        <strong>{{ keyword ? '没有匹配秒杀活动' : '暂无秒杀活动' }}</strong>
        <span>{{ keyword ? '换个关键词试试，或清空搜索查看全部秒杀。' : '稍后刷新页面，或等待后台配置秒杀活动。' }}</span>
        <button v-if="keyword" type="button" @click="clearSearch">查看全部秒杀</button>
      </div>
    </section>
  </main>
</template>
