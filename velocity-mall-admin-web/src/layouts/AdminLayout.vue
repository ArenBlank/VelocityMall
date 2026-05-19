<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <RouterLink class="brand" :to="homePath">
        <span class="brand-main">Velocity<span>Mall</span></span>
        <small>Admin</small>
      </RouterLink>
      <nav>
        <RouterLink v-for="item in visibleNavItems" :key="item.path" :to="item.path">
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
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  BadgePercent,
  Gauge,
  Images,
  MessageSquareText,
  PackageSearch,
  ReceiptText,
  Search,
  ShieldCheck,
  UserRound,
  Zap
} from 'lucide-vue-next';
import { AdminPermissions } from '@/constants/permissions';
import { useAdminAuthStore } from '@/stores/adminAuthStore';

const router = useRouter();
const auth = useAdminAuthStore();

const navItems = [
  { path: '/products', label: '商品运营', icon: PackageSearch, permission: AdminPermissions.PRODUCT_READ },
  { path: '/orders', label: '订单中心', icon: ReceiptText, permission: AdminPermissions.ORDER_READ },
  { path: '/seckill-activities', label: '秒杀活动', icon: Zap, permission: AdminPermissions.SECKILL_READ },
  { path: '/seckill-stress-console', label: '压测观测台', icon: Gauge, permission: AdminPermissions.SECKILL_READ },
  { path: '/coupons', label: '优惠券', icon: BadgePercent, permission: AdminPermissions.COUPON_READ },
  { path: '/reviews', label: '评价管理', icon: MessageSquareText, permission: AdminPermissions.REVIEW_READ },
  { path: '/media', label: '媒体资源', icon: Images, permission: AdminPermissions.PRODUCT_WRITE },
  { path: '/system/search-index', label: '搜索索引', icon: Search, permission: AdminPermissions.SYSTEM_REBUILD },
  { path: '/system/rbac', label: '权限管理', icon: ShieldCheck, permission: AdminPermissions.RBAC_READ }
];

const visibleNavItems = computed(() => navItems.filter((item) => auth.hasPermission(item.permission)));
const homePath = computed(() => visibleNavItems.value[0]?.path || '/403');

function logout() {
  auth.logout();
  void router.push('/login');
}
</script>
