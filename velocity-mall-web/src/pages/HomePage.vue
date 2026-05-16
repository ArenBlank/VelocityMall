<script setup lang="ts">
import { AlertTriangle, LoaderCircle, PackageSearch, ShieldCheck, Zap } from 'lucide-vue-next';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import type { CategoryTreeVO, SearchSkuVO, SeckillActivityVO, SeckillActivityState } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { useCategoryStore } from '@/stores/categoryStore';
import { type SearchSort, useProductStore } from '@/stores/productStore';
import { useSeckillActivityStore } from '@/stores/seckillActivityStore';
import { countdownParts, money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();
const categoryStore = useCategoryStore();
const seckillActivityStore = useSeckillActivityStore();
const now = ref(Date.now());
let countdownTimer: number | null = null;

interface HomeProduct {
  skuId: number;
  spuId: number;
  activityId?: number;
  activityName?: string;
  displayName: string;
  displayImage: string;
  regularPrice: number;
  originalPrice?: number;
  seckillPrice?: number;
  remainingStock?: number;
  startTime?: string;
  endTime?: string;
  state?: SeckillActivityState;
  isSeckill: boolean;
  saleCount: number;
}

const sortOptions: Array<{ label: string; value: SearchSort }> = [
  { label: '销量优先', value: 'sale_desc' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' }
];

const keyword = computed(() => String(route.query.keyword || '').trim());
const sort = computed<SearchSort>(() => {
  const value = String(route.query.sort || 'sale_desc');
  return sortOptions.some((item) => item.value === value) ? (value as SearchSort) : 'sale_desc';
});
const pageNo = computed(() => {
  const rawPage = Number(route.query.page || 1);
  return Number.isFinite(rawPage) && rawPage > 0 ? Math.floor(rawPage) : 1;
});
const categoryChips = computed<CategoryTreeVO[]>(() => categoryStore.flatCategories.slice(0, 12));
const totalPages = computed(() => Math.max(productStore.searchPage?.pages || 1, 1));

function mergeSearchRecord(record: SearchSkuVO, activity?: SeckillActivityVO): HomeProduct {
  return {
    skuId: record.skuId,
    spuId: activity?.spuId || 0,
    activityId: activity?.activityId,
    activityName: activity?.activityName,
    displayName: record.skuName,
    displayImage: normalizeProductImage(record.skuPic, pickFallbackCover(record.skuId)),
    regularPrice: Number(record.price ?? activity?.originalPrice ?? 0),
    originalPrice: activity?.originalPrice,
    seckillPrice: activity?.seckillPrice,
    remainingStock: activity?.remainingStock,
    startTime: activity?.startTime,
    endTime: activity?.endTime,
    state: activity?.state,
    isSeckill: Boolean(activity),
    saleCount: Number(record.saleCount ?? 0)
  };
}

function mergeActivity(activity: SeckillActivityVO, record?: SearchSkuVO): HomeProduct {
  return {
    skuId: activity.skuId,
    spuId: activity.spuId,
    activityId: activity.activityId,
    activityName: activity.activityName,
    displayName: record?.skuName || activity.activityName,
    displayImage: normalizeProductImage(record?.skuPic, pickFallbackCover(activity.skuId)),
    regularPrice: Number(record?.price ?? activity.originalPrice ?? 0),
    originalPrice: activity.originalPrice,
    seckillPrice: activity.seckillPrice,
    remainingStock: activity.remainingStock,
    startTime: activity.startTime,
    endTime: activity.endTime,
    state: activity.state,
    isSeckill: true,
    saleCount: Number(record?.saleCount ?? 0)
  };
}

const products = computed<HomeProduct[]>(() => {
  const records = productStore.searchPage?.records || [];
  const recordMap = new Map(records.map((record) => [record.skuId, record]));
  const activityMap = new Map(seckillActivityStore.activities.map((activity) => [activity.skuId, activity]));
  if (records.length > 0) {
    return records.map((record) => mergeSearchRecord(record, activityMap.get(record.skuId)));
  }
  if (keyword.value) {
    return [];
  }
  return seckillActivityStore.activities.map((activity) => mergeActivity(activity, recordMap.get(activity.skuId)));
});

const featuredProduct = computed(() => products.value.find((item) => item.isSeckill) || products.value[0] || null);
const fallbackActivity = computed(() => featuredProduct.value || seckillActivityStore.mainActivity);
const featuredStartAt = computed(() => new Date(fallbackActivity.value?.startTime || now.value).getTime());
const featuredEndAt = computed(() => new Date(fallbackActivity.value?.endTime || now.value).getTime());
const featuredBeforeStart = computed(() => now.value < featuredStartAt.value);
const featuredEnded = computed(() => now.value > featuredEndAt.value);
const featuredCountdownTarget = computed(() => {
  if (featuredBeforeStart.value) {
    return fallbackActivity.value?.startTime || new Date(now.value).toISOString();
  }
  return fallbackActivity.value?.endTime || new Date(now.value).toISOString();
});
const featuredCountdownLabel = computed(() => {
  if (featuredBeforeStart.value) {
    return '距离活动开始：';
  }
  if (featuredEnded.value) {
    return '本场已结束：';
  }
  return '本场距结束：';
});
const countdown = computed(() => {
  now.value;
  return countdownParts(featuredCountdownTarget.value);
});
const featuredImage = computed(() => featuredProduct.value?.displayImage || pickFallbackCover(fallbackActivity.value?.skuId));
const featuredTitle = computed(() => featuredProduct.value?.displayName || fallbackActivity.value?.activityName || '秒杀活动加载中');
const featuredRegularPrice = computed(() => featuredProduct.value?.regularPrice || fallbackActivity.value?.originalPrice || 0);
const featuredSeckillPrice = computed(() => featuredProduct.value?.seckillPrice || featuredProduct.value?.regularPrice || fallbackActivity.value?.seckillPrice || 0);
const heroTargetSkuId = computed(() => featuredProduct.value?.skuId || fallbackActivity.value?.skuId || null);

function loadProducts() {
  void Promise.all([
    seckillActivityStore.loadActivities(),
    productStore.loadSearchCatalog(keyword.value, sort.value, pageNo.value),
    categoryStore.loadCategories()
  ]);
}

function clearSearch() {
  void router.push({ name: 'home' });
}

function searchCategory(name: string) {
  void router.push({ name: 'home', query: { keyword: name, sort: sort.value, page: 1 } });
}

function selectSort(value: SearchSort) {
  void router.push({
    name: 'home',
    query: {
      ...(keyword.value ? { keyword: keyword.value } : {}),
      sort: value,
      page: 1
    }
  });
}

function goPage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value) {
    return;
  }
  void router.push({
    name: 'home',
    query: {
      ...(keyword.value ? { keyword: keyword.value } : {}),
      sort: sort.value,
      page: nextPage
    }
  });
}

onMounted(() => {
  loadProducts();
  countdownTimer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});
watch([keyword, sort, pageNo], loadProducts);

onUnmounted(() => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
  }
});
</script>

<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="hero-copy">
        <span class="hero-badge"><Zap :size="22" fill="currentColor" /> 秒杀专区</span>
        <h1>{{ featuredTitle }}</h1>
        <p class="hero-subtitle">
          {{ keyword ? `搜索“${keyword}”的秒杀商品` : '今日限时秒杀，库存与订单状态会实时刷新' }}
        </p>

        <div class="hero-price">
          <span>¥</span>
          <strong>{{ featuredSeckillPrice }}</strong>
          <em>{{ featuredProduct?.isSeckill ? '秒杀价' : '到手价' }}</em>
          <del>{{ money(featuredRegularPrice) }}</del>
        </div>

        <div class="hero-countdown">
          <b>{{ featuredCountdownLabel }}</b>
          <strong>{{ countdown[0] }}</strong>
          <i>:</i>
          <strong>{{ countdown[1] }}</strong>
          <i>:</i>
          <strong>{{ countdown[2] }}</strong>
        </div>

        <div class="hero-actions">
          <RouterLink v-if="heroTargetSkuId" class="hero-cta" :to="`/products/${heroTargetSkuId}`">
            <Zap :size="24" fill="currentColor" />
            进入秒杀
          </RouterLink>
          <button v-else class="hero-cta" type="button" disabled>
            <Zap :size="24" fill="currentColor" />
            活动加载中
          </button>
          <button v-if="keyword" class="collect-button" type="button" @click="clearSearch">清空搜索</button>
        </div>
      </div>

      <div class="hero-phone">
        <img :src="featuredImage" :alt="featuredTitle" />
      </div>
    </section>

    <section v-if="categoryChips.length > 0" class="real-category-strip">
      <div>
        <strong>分类搜索</strong>
        <span>来自商品分类树</span>
      </div>
      <button
        v-for="category in categoryChips"
        :key="category.id"
        type="button"
        :class="{ active: keyword === category.name }"
        @click="searchCategory(category.name)"
      >
        {{ category.name }}
      </button>
    </section>

    <section class="home-catalog">
      <div class="catalog-real-head">
        <div>
          <h2>{{ keyword ? '搜索结果' : '秒杀商品' }}</h2>
          <p>{{ keyword ? `关键词：${keyword}` : '商品搜索结果会叠加当前秒杀活动信息' }}</p>
        </div>
        <div class="catalog-controls">
          <div class="sort-buttons" aria-label="商品排序">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              type="button"
              :class="{ active: sort === option.value }"
              @click="selectSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
          <button v-if="keyword" type="button" @click="clearSearch">查看全部商品</button>
        </div>
      </div>

      <div v-if="productStore.loading || seckillActivityStore.loading" class="catalog-loading">
        <LoaderCircle :size="24" class="spin" />
        正在加载商品...
      </div>

      <div v-else-if="products.length > 0" class="product-grid">
        <article v-for="item in products" :key="item.skuId" class="sale-card">
          <div class="card-badges">
            <span>{{ item.isSeckill ? '秒杀中' : '在售' }}</span>
            <em v-if="item.saleCount > 0">已售 {{ item.saleCount.toLocaleString('zh-CN') }}</em>
          </div>
          <RouterLink class="card-image" :to="`/products/${item.skuId}`">
            <img :src="item.displayImage" :alt="item.displayName" />
          </RouterLink>
          <h2>{{ item.displayName }}</h2>
          <p>SKU {{ item.skuId }}</p>
          <small>销量 {{ item.saleCount.toLocaleString('zh-CN') }} 件</small>
          <div class="card-price">
            <strong>¥ {{ item.seckillPrice ?? item.regularPrice }}</strong>
            <del>{{ money(item.regularPrice) }}</del>
          </div>
          <p class="stock-hint safe">
            <ShieldCheck :size="16" />
            {{ item.isSeckill && item.remainingStock !== undefined ? `秒杀库存 ${item.remainingStock}` : '库存以详情页实时数据为准' }}
          </p>
          <RouterLink class="detail-button" :to="`/products/${item.skuId}`">查看详情</RouterLink>
        </article>
      </div>

      <footer v-if="productStore.searchPage && totalPages > 1" class="catalog-pagination">
        <button type="button" :disabled="pageNo <= 1" @click="goPage(pageNo - 1)">上一页</button>
        <strong>{{ pageNo }} / {{ totalPages }}</strong>
        <button type="button" :disabled="pageNo >= totalPages" @click="goPage(pageNo + 1)">下一页</button>
      </footer>

      <div v-else-if="productStore.error || seckillActivityStore.error" class="orders-error">
        <AlertTriangle :size="28" />
        <strong>{{ productStore.error || seckillActivityStore.error }}</strong>
        <button type="button" @click="loadProducts">重试</button>
      </div>

      <div v-else class="orders-empty">
        <PackageSearch :size="34" />
        <strong>{{ keyword ? '没有匹配秒杀商品' : '暂无秒杀商品' }}</strong>
        <span>换个关键词试试，或稍后刷新页面。</span>
        <button v-if="keyword" type="button" @click="clearSearch">查看全部商品</button>
      </div>
    </section>
  </main>
</template>
