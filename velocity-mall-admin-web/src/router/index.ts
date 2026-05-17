import { createRouter, createWebHistory } from 'vue-router';
import { AdminPermissions } from '@/constants/permissions';
import AdminLayout from '@/layouts/AdminLayout.vue';
import LoginPage from '@/pages/LoginPage.vue';
import { useAdminAuthStore } from '@/stores/adminAuthStore';

const DEFAULT_ADMIN_PATH = '/products';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/products',
      children: [
        {
          path: 'products',
          name: 'products',
          component: () => import('@/pages/ProductsPage.vue'),
          meta: { permission: AdminPermissions.PRODUCT_READ }
        },
        {
          path: 'products/:spuId',
          name: 'product-detail',
          component: () => import('@/pages/ProductDetailPage.vue'),
          meta: { permission: AdminPermissions.PRODUCT_READ }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/pages/OrdersPage.vue'),
          meta: { permission: AdminPermissions.ORDER_READ }
        },
        {
          path: 'orders/:orderSn',
          name: 'order-detail',
          component: () => import('@/pages/OrderDetailPage.vue'),
          meta: { permission: AdminPermissions.ORDER_READ }
        },
        {
          path: 'seckill-activities',
          name: 'seckill-activities',
          component: () => import('@/pages/SeckillActivitiesPage.vue'),
          meta: { permission: AdminPermissions.SECKILL_READ }
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('@/pages/CouponsPage.vue'),
          meta: { permission: AdminPermissions.COUPON_READ }
        },
        {
          path: 'reviews',
          name: 'reviews',
          component: () => import('@/pages/ReviewsPage.vue'),
          meta: { permission: AdminPermissions.REVIEW_READ }
        },
        {
          path: 'media',
          name: 'media',
          component: () => import('@/pages/MediaPage.vue'),
          meta: { permission: AdminPermissions.PRODUCT_WRITE }
        },
        {
          path: 'system/search-index',
          name: 'search-index',
          component: () => import('@/pages/SearchIndexPage.vue'),
          meta: { permission: AdminPermissions.SYSTEM_REBUILD }
        },
        {
          path: 'system/rbac',
          name: 'rbac-management',
          component: () => import('@/pages/RbacManagementPage.vue'),
          meta: { permission: AdminPermissions.RBAC_READ }
        },
        { path: '403', name: 'forbidden', component: () => import('@/pages/ForbiddenPage.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/products' }
  ]
});

router.beforeEach(async (to) => {
  const auth = useAdminAuthStore();
  if (!to.meta.public && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && auth.token) {
    return { path: DEFAULT_ADMIN_PATH };
  }
  if (!to.meta.public && auth.token && (!auth.profile || auth.permissions.length === 0)) {
    try {
      await auth.refreshProfile();
    } catch {
      auth.logout();
      return { path: '/login', query: { redirect: to.fullPath } };
    }
  }
  const requiredPermission = to.meta.permission as string | undefined;
  if (requiredPermission && !auth.hasPermission(requiredPermission)) {
    return { path: '/403' };
  }
  return true;
});

export default router;
