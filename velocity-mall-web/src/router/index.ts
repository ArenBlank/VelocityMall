import { createRouter, createWebHistory } from 'vue-router';

import { TOKEN_KEY } from '@/api/http';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/pages/HomePage.vue')
    },
    {
      path: '/products',
      name: 'normal-products',
      component: () => import('@/pages/NormalProductsPage.vue')
    },
    {
      path: '/products/:skuId',
      name: 'product-detail',
      component: () => import('@/pages/FlashSalePage.vue')
    },
    {
      path: '/seckill',
      name: 'seckill-zone',
      component: () => import('@/pages/SeckillZonePage.vue')
    },
    {
      path: '/seckill/:skuId',
      name: 'seckill-detail',
      component: () => import('@/pages/FlashSalePage.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      props: { initialMode: 'login' },
      meta: { publicOnly: true, authLayout: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/LoginPage.vue'),
      props: { initialMode: 'register' },
      meta: { publicOnly: true, authLayout: true }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/pages/OrdersPage.vue')
    },
    {
      path: '/orders/:orderSn',
      name: 'order-detail',
      component: () => import('@/pages/OrderDetailPage.vue'),
      props: true
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/pages/CartPage.vue')
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/pages/AddressPage.vue')
    },
    {
      path: '/coupons',
      name: 'coupons',
      component: () => import('@/pages/CouponPage.vue')
    },
    {
      path: '/pay/:orderSn',
      name: 'pay',
      component: () => import('@/pages/PayPage.vue'),
      props: true
    }
  ],
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, top: 16 };
    }
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!to.meta.publicOnly && !token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    };
  }
  if (to.meta.publicOnly && token) {
    return { path: '/home' };
  }
  return true;
});

export default router;
