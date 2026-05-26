<script setup lang="ts">
import { AlertTriangle, ChevronLeft, ChevronRight, Clock3, LoaderCircle, PackageSearch, ShieldCheck, Zap } from 'lucide-vue-next';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import type { SearchSkuVO, SeckillActivityVO, SeckillActivityState } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { type SearchSort, useProductStore } from '@/stores/productStore';
import { useSeckillActivityStore } from '@/stores/seckillActivityStore';
import { countdownParts, money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();
const seckillActivityStore = useSeckillActivityStore();
const now = ref(Date.now());
const currentHeroIndex = ref(0);
let countdownTimer: number | null = null;
let heroTimer: number | null = null;

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
  const activityMap = new Map(seckillActivityStore.activities.map((activity) => [activity.skuId, activity]));
  if (records.length > 0) {
    return records.map((record) => mergeSearchRecord(record, activityMap.get(record.skuId)));
  }
  return [];
});
const searchRecordMap = computed(() => new Map((productStore.searchPage?.records || []).map((record) => [record.skuId, record])));
const seckillProducts = computed<HomeProduct[]>(() =>
  seckillActivityStore.activities
    .map((activity) => mergeActivity(activity, searchRecordMap.value.get(activity.skuId)))
    .filter((item) => matchesKeyword(item))
);

const heroItems = computed(() => seckillProducts.value);
const activeHero = computed(() => heroItems.value[currentHeroIndex.value] || heroItems.value[0] || null);
const featuredStartAt = computed(() => new Date(activeHero.value?.startTime || now.value).getTime());
const featuredEndAt = computed(() => new Date(activeHero.value?.endTime || now.value).getTime());
const featuredBeforeStart = computed(() => activeHero.value?.state === 'NOT_STARTED' || now.value < featuredStartAt.value);
const featuredEnded = computed(() => activeHero.value?.state === 'ENDED' || now.value > featuredEndAt.value);
const featuredCountdownTarget = computed(() => {
  if (featuredBeforeStart.value) {
    return activeHero.value?.startTime || new Date(now.value).toISOString();
  }
  return activeHero.value?.endTime || new Date(now.value).toISOString();
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
const heroSubtitle = computed(() => {
  if (!activeHero.value) {
    return '秒杀活动加载中';
  }
  if (activeHero.value.state === 'NOT_STARTED') {
    return '活动即将开始，提前查看商品信息';
  }
  if (activeHero.value.state === 'ENDED') {
    return '活动已结束，可查看商品详情';
  }
  return '今日限时秒杀，库存与订单状态会实时刷新';
});
const countdown = computed(() => {
  now.value;
  return countdownParts(featuredCountdownTarget.value);
});
const featuredImage = computed(() => activeHero.value?.displayImage || pickFallbackCover(activeHero.value?.skuId));
const featuredTitle = computed(() => activeHero.value?.displayName || '秒杀活动加载中');
const featuredRegularPrice = computed(() => activeHero.value?.regularPrice || activeHero.value?.originalPrice || 0);
const featuredSeckillPrice = computed(() => activeHero.value?.seckillPrice || activeHero.value?.regularPrice || 0);
const heroTargetSkuId = computed(() => activeHero.value?.skuId || null);
const catalogTitle = computed(() => (keyword.value ? '搜索结果' : '普通购买专区'));
const catalogDescription = computed(() =>
  keyword.value
    ? `关键词：${keyword.value}`
    : '这里展示可加入购物车并提交普通订单的商品，进入详情后走购物车结算。'
);
const emptyTitle = computed(() => (keyword.value ? '没有匹配商品' : '暂无可购买商品'));
const emptyDescription = computed(() =>
  keyword.value ? '换个关键词试试，或清空搜索查看全部商品。' : '稍后刷新页面，或等待后台上架商品。'
);
const seckillEmptyTitle = computed(() => (keyword.value ? '没有匹配秒杀活动' : '暂无秒杀活动'));
const seckillEmptyDescription = computed(() =>
  keyword.value ? '换个关键词试试，或清空搜索查看全部秒杀活动。' : '稍后刷新页面，或等待后台配置秒杀活动。'
);

function matchesKeyword(item: HomeProduct) {
  const normalizedKeyword = keyword.value.toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }
  return [
    item.displayName,
    item.activityName,
    String(item.skuId),
    String(item.spuId)
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(normalizedKeyword);
}

function statusLabel(state?: SeckillActivityState) {
  const labels: Record<SeckillActivityState, string> = {
    ACTIVE: '秒杀中',
    NOT_STARTED: '未开始',
    ENDED: '已结束',
    DISABLED: '已停用'
  };
  return state ? labels[state] : '秒杀活动';
}

function statusClass(state?: SeckillActivityState) {
  return state ? `state-${state.toLowerCase().replace('_', '-')}` : '';
}

function countdownLabel(item: HomeProduct) {
  if (item.state === 'NOT_STARTED') {
    return '距开始';
  }
  if (item.state === 'ENDED') {
    return '已结束';
  }
  return '距结束';
}

function countdownFor(item: HomeProduct) {
  now.value;
  const target = item.state === 'NOT_STARTED' ? item.startTime : item.endTime;
  return countdownParts(target || new Date(now.value).toISOString());
}

function seckillActionText(state?: SeckillActivityState) {
  if (state === 'ACTIVE') {
    return '进入抢购';
  }
  if (state === 'NOT_STARTED') {
    return '查看活动';
  }
  return '查看详情';
}

function nextHero() {
  if (heroItems.value.length <= 1) {
    return;
  }
  currentHeroIndex.value = (currentHeroIndex.value + 1) % heroItems.value.length;
}

function prevHero() {
  if (heroItems.value.length <= 1) {
    return;
  }
  currentHeroIndex.value = (currentHeroIndex.value - 1 + heroItems.value.length) % heroItems.value.length;
}

function selectHero(index: number) {
  if (index < 0 || index >= heroItems.value.length) {
    return;
  }
  currentHeroIndex.value = index;
}

function loadProducts() {
  void Promise.all([
    seckillActivityStore.loadActivities(),
    productStore.loadSearchCatalog(keyword.value, sort.value, pageNo.value)
  ]);
}

function clearSearch() {
  void router.push({ name: 'home' });
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
  heroTimer = window.setInterval(nextHero, 5000);
});
watch([keyword, sort, pageNo], loadProducts);
watch(
  () => heroItems.value.map((item) => item.skuId).join(','),
  () => {
    if (currentHeroIndex.value >= heroItems.value.length) {
      currentHeroIndex.value = 0;
    }
  }
);

onUnmounted(() => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
  }
  if (heroTimer !== null) {
    window.clearInterval(heroTimer);
  }
});
</script>

<template>
  <main class="home-page">
    <section class="home-hero seckill-hero-carousel">
      <div class="hero-copy" :key="heroTargetSkuId || 'empty-hero'">
        <span class="hero-badge" :class="statusClass(activeHero?.state)">
          <Zap :size="22" fill="currentColor" />
          秒杀专区 · {{ statusLabel(activeHero?.state) }}
        </span>
        <h1>{{ featuredTitle }}</h1>
        <p class="hero-subtitle">
          {{ keyword ? `搜索“${keyword}”的商品` : heroSubtitle }}
        </p>

        <div class="hero-price">
          <span>¥</span>
          <strong>{{ featuredSeckillPrice }}</strong>
          <em>秒杀价</em>
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
          <RouterLink v-if="heroTargetSkuId" class="hero-cta" :to="`/seckill/${heroTargetSkuId}`">
            <Zap :size="24" fill="currentColor" />
            {{ seckillActionText(activeHero?.state) }}
          </RouterLink>
          <button v-else class="hero-cta" type="button" disabled>
            <Zap :size="24" fill="currentColor" />
            活动加载中
          </button>
          <button v-if="keyword" class="collect-button" type="button" @click="clearSearch">清空搜索</button>
        </div>
      </div>

      <div class="hero-phone">
        <img :key="featuredImage" :src="featuredImage" :alt="featuredTitle" width="430" height="360" fetchpriority="high" />
      </div>

      <div v-if="heroItems.length > 1" class="hero-carousel-controls" aria-label="秒杀海报轮播">
        <button type="button" aria-label="上一张秒杀海报" @click="prevHero">
          <ChevronLeft :size="20" />
        </button>
        <div class="hero-dots">
          <button
            v-for="(item, index) in heroItems"
            :key="item.skuId"
            type="button"
            :class="{ active: index === currentHeroIndex }"
            :aria-label="`查看 ${item.displayName} 海报`"
            @click="selectHero(index)"
          />
        </div>
        <button type="button" aria-label="下一张秒杀海报" @click="nextHero">
          <ChevronRight :size="20" />
        </button>
      </div>
    </section>

    <section class="home-catalog seckill-overview">
      <div class="catalog-real-head">
        <div>
          <h2>秒杀专区</h2>
          <p>展示全部未结束秒杀活动，未开始的商品也会提前展示。</p>
        </div>
        <div class="catalog-controls">
          <RouterLink class="catalog-link-button" :to="{ name: 'seckill-zone' }">查看全部秒杀</RouterLink>
        </div>
      </div>

      <div v-if="seckillActivityStore.loading || productStore.loading" class="catalog-loading">
        <LoaderCircle :size="24" class="spin" />
        正在加载秒杀活动…
      </div>

      <div v-else-if="seckillProducts.length > 0" class="product-grid seckill-product-grid">
        <article v-for="item in seckillProducts" :key="item.skuId" class="sale-card seckill-sale-card">
          <div class="card-badges">
            <span :class="statusClass(item.state)">{{ statusLabel(item.state) }}</span>
            <em>库存 {{ item.remainingStock ?? 0 }}</em>
          </div>
          <RouterLink class="card-image" :to="`/seckill/${item.skuId}`">
            <img :src="item.displayImage" :alt="item.displayName" loading="lazy" />
          </RouterLink>
          <h2>{{ item.displayName }}</h2>
          <p>SKU {{ item.skuId }}</p>
          <small>销量 {{ item.saleCount.toLocaleString('zh-CN') }} 件</small>
          <div class="card-price">
            <strong>¥ {{ item.seckillPrice ?? item.regularPrice }}</strong>
            <del v-if="item.regularPrice > (item.seckillPrice ?? item.regularPrice)">{{ money(item.regularPrice) }}</del>
          </div>
          <p class="stock-hint" :class="item.state === 'ACTIVE' ? 'safe' : 'warning'">
            <ShieldCheck v-if="item.state === 'ACTIVE'" :size="16" />
            <Clock3 v-else :size="16" />
            {{ item.state === 'ACTIVE' ? `可抢库存 ${item.remainingStock ?? 0}` : `即将开抢，秒杀库存 ${item.remainingStock ?? 0}` }}
          </p>
          <p class="seckill-countdown-line">
            {{ countdownLabel(item) }}
            <strong>{{ countdownFor(item)[0] }}:{{ countdownFor(item)[1] }}:{{ countdownFor(item)[2] }}</strong>
          </p>
          <RouterLink class="detail-button" :to="`/seckill/${item.skuId}`">{{ seckillActionText(item.state) }}</RouterLink>
        </article>
      </div>

      <div v-else class="orders-empty">
        <PackageSearch :size="34" />
        <strong>{{ seckillEmptyTitle }}</strong>
        <span>{{ seckillEmptyDescription }}</span>
        <button v-if="keyword" type="button" @click="clearSearch">查看全部秒杀</button>
      </div>
    </section>

    <section id="normal-products" class="home-catalog">
      <div class="catalog-real-head">
        <div>
          <h2>{{ catalogTitle }}</h2>
          <p>{{ catalogDescription }}</p>
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
        正在加载商品…
      </div>

      <template v-else-if="products.length > 0">
        <div class="product-grid">
        <article v-for="item in products" :key="item.skuId" class="sale-card">
          <div class="card-badges">
            <span>普通商品</span>
            <em v-if="item.saleCount > 0">已售 {{ item.saleCount.toLocaleString('zh-CN') }}</em>
          </div>
          <RouterLink class="card-image" :to="`/products/${item.skuId}`">
            <img :src="item.displayImage" :alt="item.displayName" loading="lazy" />
          </RouterLink>
          <h2>{{ item.displayName }}</h2>
          <p>SKU {{ item.skuId }}</p>
          <small>销量 {{ item.saleCount.toLocaleString('zh-CN') }} 件</small>
          <div class="card-price">
            <strong>¥ {{ item.regularPrice }}</strong>
          </div>
          <p class="stock-hint safe">
            <ShieldCheck :size="16" />
            库存以详情页实时数据为准
          </p>
          <RouterLink class="detail-button" :to="`/products/${item.skuId}`">普通购买</RouterLink>
        </article>
      </div>

      <footer v-if="productStore.searchPage && totalPages > 1" class="catalog-pagination">
        <button type="button" :disabled="pageNo <= 1" @click="goPage(pageNo - 1)">上一页</button>
        <strong>{{ pageNo }} / {{ totalPages }}</strong>
        <button type="button" :disabled="pageNo >= totalPages" @click="goPage(pageNo + 1)">下一页</button>
      </footer>

      </template>

      <div v-else-if="productStore.error || seckillActivityStore.error" class="orders-error">
        <AlertTriangle :size="28" />
        <strong>{{ productStore.error || seckillActivityStore.error }}</strong>
        <button type="button" @click="loadProducts">重试</button>
      </div>

      <div v-else class="orders-empty">
        <PackageSearch :size="34" />
        <strong>{{ emptyTitle }}</strong>
        <span>{{ emptyDescription }}</span>
        <button v-if="keyword" type="button" @click="clearSearch">查看全部商品</button>
      </div>
    </section>
  </main>
</template>
