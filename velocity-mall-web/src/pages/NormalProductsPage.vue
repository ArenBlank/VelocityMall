<script setup lang="ts">
import { AlertTriangle, LoaderCircle, PackageSearch, ShieldCheck, ShoppingCart } from 'lucide-vue-next';
import { computed, onMounted, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import type { SearchSkuVO } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { type SearchSort, useProductStore } from '@/stores/productStore';
import { normalizeProductImage } from '@/utils/images';

interface NormalProduct {
  skuId: number;
  displayName: string;
  displayImage: string;
  regularPrice: number;
  saleCount: number;
}

const route = useRoute();
const router = useRouter();
const productStore = useProductStore();

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

const products = computed<NormalProduct[]>(() => (productStore.searchPage?.records || []).map(toNormalProduct));
const emptyTitle = computed(() => (keyword.value ? '没有匹配商品' : '暂无可购买商品'));
const emptyDescription = computed(() =>
  keyword.value ? '换个关键词试试，或清空搜索查看全部商品。' : '稍后刷新页面，或等待后台上架商品。'
);

function toNormalProduct(record: SearchSkuVO): NormalProduct {
  return {
    skuId: record.skuId,
    displayName: record.skuName,
    displayImage: normalizeProductImage(record.skuPic, pickFallbackCover(record.skuId)),
    regularPrice: Number(record.price ?? 0),
    saleCount: Number(record.saleCount ?? 0)
  };
}

function loadProducts() {
  void productStore.loadSearchCatalog(keyword.value, sort.value, pageNo.value);
}

function clearSearch() {
  void router.push({ name: 'normal-products' });
}

function selectSort(value: SearchSort) {
  void router.push({
    name: 'normal-products',
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
    name: 'normal-products',
    query: {
      ...(keyword.value ? { keyword: keyword.value } : {}),
      sort: sort.value,
      page: nextPage
    }
  });
}

onMounted(loadProducts);
watch([keyword, sort, pageNo], loadProducts);
</script>

<template>
  <main class="home-page catalog-page normal-zone-page">
    <section class="catalog-page-banner normal-zone-banner">
      <div>
        <span class="page-kicker"><ShoppingCart :size="18" /> 普通购买</span>
        <h1>普通购买专区</h1>
        <p>这里展示可加入购物车并提交普通订单的商品，详情页只保留购物车结算链路。</p>
      </div>
      <div class="banner-deco normal-deco" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
    </section>

    <section class="home-catalog">
      <div class="catalog-real-head">
        <div>
          <h2>{{ keyword ? '搜索结果' : '普通商品' }}</h2>
          <p>{{ keyword ? `关键词：${keyword}` : '加入购物车后可选择地址、优惠券并进入模拟支付。' }}</p>
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

      <div v-if="productStore.loading" class="catalog-loading">
        <LoaderCircle :size="24" class="spin" />
        正在加载商品...
      </div>

      <template v-else-if="products.length > 0">
        <div class="product-grid">
          <article v-for="item in products" :key="item.skuId" class="sale-card">
            <div class="card-badges">
              <span>普通商品</span>
              <em v-if="item.saleCount > 0">已售 {{ item.saleCount.toLocaleString('zh-CN') }}</em>
            </div>
            <RouterLink class="card-image" :to="`/products/${item.skuId}`">
              <img :src="item.displayImage" :alt="item.displayName" />
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

      <div v-else-if="productStore.error" class="orders-error">
        <AlertTriangle :size="28" />
        <strong>{{ productStore.error }}</strong>
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
