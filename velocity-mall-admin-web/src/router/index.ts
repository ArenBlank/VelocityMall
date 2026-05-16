import { createRouter, createWebHistory } from 'vue-router';
import { ADMIN_TOKEN_KEY } from '@/api/http';
import AdminLayout from '@/layouts/AdminLayout.vue';
import LoginPage from '@/pages/LoginPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/products',
      children: [
        { path: 'products', name: 'products', component: () => import('@/pages/ProductsPage.vue') },
        { path: 'products/:spuId', name: 'product-detail', component: () => import('@/pages/ProductDetailPage.vue') },
        { path: 'orders', name: 'orders', component: () => import('@/pages/OrdersPage.vue') },
        { path: 'orders/:orderSn', name: 'order-detail', component: () => import('@/pages/OrderDetailPage.vue') },
        { path: 'seckill-activities', name: 'seckill-activities', component: () => import('@/pages/SeckillActivitiesPage.vue') },
        { path: 'coupons', name: 'coupons', component: () => import('@/pages/CouponsPage.vue') },
        { path: 'reviews', name: 'reviews', component: () => import('@/pages/ReviewsPage.vue') },
        { path: 'media', name: 'media', component: () => import('@/pages/MediaPage.vue') },
        { path: 'system/search-index', name: 'search-index', component: () => import('@/pages/SearchIndexPage.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/products' }
  ]
});

router.beforeEach((to) => {
  const token = localStorage.getItem(ADMIN_TOKEN_KEY);
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && token) {
    return { path: '/products' };
  }
  return true;
});

export default router;
