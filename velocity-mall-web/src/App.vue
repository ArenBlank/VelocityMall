<script setup lang="ts">
import { LogOut, MapPin, Search, ShoppingBag, ShoppingCart, Ticket, UserRound, Zap } from 'lucide-vue-next';
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/authStore';
import { useCartStore } from '@/stores/cartStore';
import { useSeckillActivityStore } from '@/stores/seckillActivityStore';

const authStore = useAuthStore();
const cartStore = useCartStore();
const seckillActivityStore = useSeckillActivityStore();
const route = useRoute();
const router = useRouter();
const isAuthLayout = computed(() => route.meta.authLayout === true);
const accountLabel = computed(() => authStore.user?.username || '登录/注册');
const cartCount = computed(() => cartStore.totalQuantity);
const searchKeyword = ref(String(route.query.keyword || ''));

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = String(keyword || '');
  }
);

function submitSearch() {
  const keyword = searchKeyword.value.trim();
  void router.push({
    name: 'home',
    query: keyword ? { keyword } : {}
  });
}

function goAccount() {
  if (!authStore.token) {
    void router.push('/login');
    return;
  }
  void router.push('/orders');
}

function logout() {
  authStore.logout();
  void router.push('/login');
}

function ensureBuyerDataLoaded() {
  if (authStore.token && (!authStore.user || authStore.user.id === 0)) {
    void authStore.refreshMe().catch(() => router.push('/login'));
  }
  if (authStore.token && seckillActivityStore.activities.length === 0) {
    void seckillActivityStore.loadActivities();
  }
  if (authStore.token && cartStore.items.length === 0) {
    void cartStore.loadItems();
  }
}

onMounted(ensureBuyerDataLoaded);
watch(() => authStore.token, ensureBuyerDataLoaded);
</script>

<template>
  <div v-if="isAuthLayout" class="auth-shell">
    <header class="auth-brand-header">
      <RouterLink class="brand" to="/login">
        <span>Velocity</span><strong>Mall</strong>
      </RouterLink>
      <small>秒杀 · 订单 · 支付</small>
    </header>
    <RouterView />
  </div>

  <div v-else class="app-shell">
    <header class="site-header clean-header">
      <div class="brand-zone">
        <RouterLink class="brand" to="/">
          <span>Velocity</span><strong>Mall</strong>
        </RouterLink>
        <small>买家前台</small>
      </div>

      <form class="search-box" role="search" @submit.prevent="submitSearch">
        <input v-model.trim="searchKeyword" placeholder="搜索商品名称或 SKU…" aria-label="搜索商品" autocomplete="off" />
        <button type="submit">
          <Search :size="21" />
          搜索
        </button>
      </form>

      <div class="header-actions clean-actions">
        <button class="plain-action" type="button" @click="goAccount">
          <UserRound :size="24" />
          {{ accountLabel }}
        </button>
        <button v-if="authStore.token" class="plain-action muted-action" type="button" @click="logout">
          <LogOut :size="21" />
          退出
        </button>
      </div>
    </header>

    <nav class="category-nav clean-nav" aria-label="主导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink :to="{ name: 'normal-products' }">
        <ShoppingBag :size="18" />
        普通购买
      </RouterLink>
      <RouterLink :to="{ name: 'seckill-zone' }">
        <Zap :size="18" fill="currentColor" />
        秒杀专区
      </RouterLink>
      <RouterLink to="/orders">我的订单</RouterLink>
      <RouterLink to="/cart">
        <ShoppingCart :size="18" />
        购物车
        <span v-if="cartCount > 0" class="nav-count">{{ cartCount }}</span>
      </RouterLink>
      <RouterLink to="/addresses">
        <MapPin :size="18" />
        地址
      </RouterLink>
      <RouterLink to="/coupons">
        <Ticket :size="18" />
        优惠券
      </RouterLink>
    </nav>

    <RouterView />
  </div>
</template>
