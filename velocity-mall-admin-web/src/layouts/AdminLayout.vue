<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/products">
        <span class="brand-main">Velocity<span>Mall</span></span>
        <small>Admin</small>
      </RouterLink>
      <nav>
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          <component :is="item.icon" :size="18" />
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>
    <main class="workspace">
      <header class="topbar">
        <div>
          <strong>VelocityMall Admin</strong>
          <span>真实后端运营工作台</span>
        </div>
        <div class="admin-user">
          <UserRound :size="18" />
          <span>{{ auth.displayName }}</span>
          <button type="button" class="ghost-button compact" @click="logout">退出</button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  BadgePercent,
  Images,
  MessageSquareText,
  PackageSearch,
  ReceiptText,
  Search,
  UserRound,
  Zap
} from 'lucide-vue-next';
import { useAdminAuthStore } from '@/stores/adminAuthStore';

const router = useRouter();
const auth = useAdminAuthStore();

const navItems = [
  { path: '/products', label: '商品运营', icon: PackageSearch },
  { path: '/orders', label: '订单中心', icon: ReceiptText },
  { path: '/seckill-activities', label: '秒杀活动', icon: Zap },
  { path: '/coupons', label: '优惠券', icon: BadgePercent },
  { path: '/reviews', label: '评价管理', icon: MessageSquareText },
  { path: '/media', label: '媒体资源', icon: Images },
  { path: '/system/search-index', label: '搜索索引', icon: Search }
];

function logout() {
  auth.logout();
  void router.push('/login');
}
</script>
