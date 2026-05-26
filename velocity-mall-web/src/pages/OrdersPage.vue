<script setup lang="ts">
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  MessageSquareText,
  PackageOpen,
  RotateCcw,
  Search,
  ShoppingBag
} from 'lucide-vue-next';
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { getSku } from '@/api/productApi';
import { createReview } from '@/api/reviewApi';
import type { OrderDetailVO, OrderItemVO } from '@/api/types';
import StatusBadge from '@/components/StatusBadge.vue';
import { fallbackCoverImages } from '@/config/media';
import { useOrderStore } from '@/stores/orderStore';
import { money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

const orderStore = useOrderStore();
const expandedOrder = ref('');
const orderKeyword = ref('');
const actionLoading = ref('');
const actionMessage = ref('');
const actionError = ref('');
const reviewSubmitting = ref(false);
const reviewDraft = reactive({
  orderSn: '',
  skuId: 0,
  skuName: '',
  rating: 5,
  content: ''
});

const filters: Array<{ label: string; status: number | null }> = [
  { label: '全部', status: null },
  { label: '待支付', status: 0 },
  { label: '已支付', status: 1 },
  { label: '已发货', status: 2 },
  { label: '已完成', status: 3 },
  { label: '已关闭', status: 4 },
  { label: '已退款', status: 5 }
];

const orders = computed(() => orderStore.page?.records || []);
const filteredOrders = computed(() => {
  const keyword = orderKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return orders.value;
  }
  return orders.value.filter((order) => {
    const orderSnMatched = order.orderSn.toLowerCase().includes(keyword);
    const itemMatched = order.items?.some((item) => item.skuName?.toLowerCase().includes(keyword));
    return orderSnMatched || itemMatched;
  });
});
const total = computed(() => orderStore.page?.total || 0);
const pageNo = computed(() => orderStore.page?.page || orderStore.pageNo);
const totalPages = computed(() => Math.max(orderStore.page?.pages || 1, 1));
const reviewOpen = computed(() => Boolean(reviewDraft.orderSn && reviewDraft.skuId));

function load(status = orderStore.status, page = 1) {
  expandedOrder.value = '';
  actionMessage.value = '';
  actionError.value = '';
  void orderStore.loadOrders(status, page, orderStore.pageSize);
}

function toggleDetails(orderSn: string) {
  expandedOrder.value = expandedOrder.value === orderSn ? '' : orderSn;
}

function primaryItem(order: OrderDetailVO): OrderItemVO | null {
  return order.items?.[0] || null;
}

function normalizeImage(src?: string) {
  return normalizeProductImage(src);
}

function productImage(order: OrderDetailVO) {
  return normalizeImage(primaryItem(order)?.skuPic);
}

function handleImageError(event: Event) {
  const target = event.target as HTMLImageElement;
  if (target.src !== fallbackCoverImages[0]) {
    target.src = fallbackCoverImages[0];
  }
}

function skuName(order: OrderDetailVO) {
  return primaryItem(order)?.skuName || '订单商品';
}

function quantity(order: OrderDetailVO) {
  return order.items?.reduce((sum, item) => sum + Number(item.quantity || 0), 0) || 0;
}

function orderPayAmount(order: OrderDetailVO) {
  return order.payAmount ?? order.totalAmount;
}

function orderTypeLabel(orderType: number) {
  return orderType === 1 ? '秒杀订单' : '普通订单';
}

function orderTypeTone(orderType: number) {
  return orderType === 1 ? 'seckill' : 'normal';
}

function formatTime(value: string) {
  if (!value) {
    return '-';
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(parsed);
}

async function runOrderAction(orderSn: string, label: string, action: () => Promise<void>) {
  actionLoading.value = `${label}-${orderSn}`;
  actionMessage.value = '';
  actionError.value = '';
  try {
    await action();
    actionMessage.value = `${label}成功`;
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : `${label}失败`;
  } finally {
    actionLoading.value = '';
  }
}

function cancelOrder(order: OrderDetailVO) {
  void runOrderAction(order.orderSn, '取消订单', () => orderStore.cancel(order.orderSn));
}

function refundOrder(order: OrderDetailVO) {
  void runOrderAction(order.orderSn, '申请退款', () => orderStore.refund(order.orderSn));
}

function confirmOrder(order: OrderDetailVO) {
  void runOrderAction(order.orderSn, '确认收货', () => orderStore.confirm(order.orderSn));
}

function openReview(order: OrderDetailVO) {
  const item = primaryItem(order);
  if (!item) {
    return;
  }
  reviewDraft.orderSn = order.orderSn;
  reviewDraft.skuId = item.skuId;
  reviewDraft.skuName = item.skuName;
  reviewDraft.rating = 5;
  reviewDraft.content = '';
}

function closeReview() {
  reviewDraft.orderSn = '';
  reviewDraft.skuId = 0;
  reviewDraft.skuName = '';
  reviewDraft.rating = 5;
  reviewDraft.content = '';
}

async function submitReview() {
  if (!reviewDraft.content.trim()) {
    actionError.value = '请填写评价内容';
    return;
  }
  reviewSubmitting.value = true;
  actionError.value = '';
  actionMessage.value = '';
  try {
    const sku = await getSku(reviewDraft.skuId);
    await createReview({
      orderSn: reviewDraft.orderSn,
      skuId: reviewDraft.skuId,
      spuId: sku.spuId,
      rating: reviewDraft.rating,
      content: reviewDraft.content.trim()
    });
    actionMessage.value = '评价已提交';
    closeReview();
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '评价提交失败';
  } finally {
    reviewSubmitting.value = false;
  }
}

function previousPage() {
  if (pageNo.value > 1) {
    load(orderStore.status, pageNo.value - 1);
  }
}

function nextPage() {
  if (pageNo.value < totalPages.value) {
    load(orderStore.status, pageNo.value + 1);
  }
}

onMounted(() => load(null, 1));
</script>

<template>
  <main class="orders-page order-center-page">
    <section class="order-center-header">
      <div>
        <h1>我的订单</h1>
        <p>查看秒杀订单、普通订单和支付状态，订单数据来自当前登录账号。</p>
      </div>
      <label class="order-search">
        <Search :size="19" />
        <input v-model.trim="orderKeyword" placeholder="搜索订单号或商品名称…" aria-label="搜索订单" />
      </label>
    </section>

    <section class="order-filter-bar">
      <button
        v-for="filter in filters"
        :key="filter.label"
        type="button"
        :class="{ active: orderStore.status === filter.status }"
        @click="load(filter.status, 1)"
      >
        {{ filter.label }}
      </button>
    </section>

    <p v-if="actionMessage" class="form-message success">{{ actionMessage }}</p>
    <p v-if="actionError" class="form-message error">{{ actionError }}</p>

    <section class="order-list-surface">
      <header class="order-table-head">
        <span>商品信息</span>
        <span>数量</span>
        <span>实付金额</span>
        <span>订单类型</span>
        <span>状态</span>
        <span>下单时间</span>
        <span>操作</span>
      </header>

      <div v-if="orderStore.loading" class="orders-loading">
        <ShoppingBag :size="26" />
        正在加载订单…
      </div>

      <div v-else-if="orderStore.error" class="orders-error">
        <PackageOpen :size="28" />
        <strong>{{ orderStore.error }}</strong>
        <button type="button" @click="load(orderStore.status, pageNo)">重试</button>
      </div>

      <div v-else-if="filteredOrders.length === 0" class="orders-empty">
        <PackageOpen :size="34" />
        <strong>{{ orderKeyword ? '没有匹配订单' : '暂无订单' }}</strong>
        <span>{{ orderKeyword ? '请换一个关键词搜索。' : '可以从秒杀专区或购物车创建订单。' }}</span>
        <RouterLink v-if="!orderKeyword" to="/">去逛逛</RouterLink>
      </div>

      <template v-else>
        <article v-for="order in filteredOrders" :key="order.orderSn" class="order-item-row">
          <div class="order-sn-line">
            <span>订单号：{{ order.orderSn }}</span>
            <button type="button" @click="toggleDetails(order.orderSn)">
              {{ expandedOrder === order.orderSn ? '收起明细' : '查看明细' }}
              <ChevronDown :size="16" :class="{ rotated: expandedOrder === order.orderSn }" />
            </button>
          </div>

          <div class="order-row-main">
            <div class="order-product-cell">
              <img :src="productImage(order)" :alt="skuName(order)" loading="lazy" @error="handleImageError" />
              <div>
                <RouterLink :to="`/products/${primaryItem(order)?.skuId || 0}`">{{ skuName(order) }}</RouterLink>
                <span>{{ primaryItem(order)?.skuPrice !== undefined ? money(primaryItem(order)?.skuPrice) : '价格同步中' }}</span>
              </div>
            </div>

            <span class="quantity-cell">x {{ quantity(order) || 1 }}</span>
            <strong class="amount-cell">{{ money(orderPayAmount(order)) }}</strong>
            <span class="order-type-tag" :class="orderTypeTone(order.orderType)">{{ orderTypeLabel(order.orderType) }}</span>
            <StatusBadge :status="order.status" />
            <time>{{ formatTime(order.createTime) }}</time>
            <div class="order-actions">
              <RouterLink v-if="order.status === 0" class="order-pay-button" :to="`/pay/${order.orderSn}`">去支付</RouterLink>
              <button v-if="order.status === 0" class="order-ghost-button" type="button" @click="cancelOrder(order)">
                <LoaderCircle v-if="actionLoading === `取消订单-${order.orderSn}`" :size="15" class="spin" />
                取消
              </button>
              <button v-if="order.status === 1" class="order-ghost-button" type="button" @click="refundOrder(order)">
                <LoaderCircle v-if="actionLoading === `申请退款-${order.orderSn}`" :size="15" class="spin" />
                退款
              </button>
              <button v-if="order.status === 2" class="order-pay-button" type="button" @click="confirmOrder(order)">
                <LoaderCircle v-if="actionLoading === `确认收货-${order.orderSn}`" :size="15" class="spin" />
                确认收货
              </button>
              <button v-if="order.status === 3 && primaryItem(order)" class="order-ghost-button" type="button" @click="openReview(order)">
                <MessageSquareText :size="15" />
                评价
              </button>
              <RouterLink class="order-ghost-button" :to="`/orders/${order.orderSn}`">详情页</RouterLink>
            </div>
          </div>

          <div v-if="expandedOrder === order.orderSn" class="order-detail-drawer">
            <div v-for="item in order.items || []" :key="`${order.orderSn}-${item.skuId}`">
              <img :src="normalizeImage(item.skuPic)" :alt="item.skuName" loading="lazy" @error="handleImageError" />
              <span>{{ item.skuName }}</span>
              <b>{{ money(item.skuPrice) }}</b>
              <em>x {{ item.quantity }}</em>
            </div>
            <p v-if="!order.items?.length">该订单暂无更多商品明细。</p>
          </div>
        </article>
      </template>
    </section>

    <section v-if="reviewOpen" class="review-modal-panel">
      <form class="buyer-form compact-form" @submit.prevent="submitReview">
        <h2>评价商品</h2>
        <p>{{ reviewDraft.skuName }}</p>
        <label>
          评分
          <select v-model.number="reviewDraft.rating">
            <option :value="5">5 星</option>
            <option :value="4">4 星</option>
            <option :value="3">3 星</option>
            <option :value="2">2 星</option>
            <option :value="1">1 星</option>
          </select>
        </label>
        <label>
          评价内容
          <textarea v-model.trim="reviewDraft.content" placeholder="说说这次购物体验…" />
        </label>
        <div class="form-actions">
          <button type="submit" class="primary-action" :disabled="reviewSubmitting">
            <LoaderCircle v-if="reviewSubmitting" :size="18" class="spin" />
            提交评价
          </button>
          <button type="button" class="ghost-action" @click="closeReview">取消</button>
        </div>
      </form>
    </section>

    <footer class="orders-pagination">
      <span>{{ orderKeyword ? `匹配 ${filteredOrders.length} / 共 ${total} 条订单` : `共 ${total} 条订单` }}</span>
      <div>
        <button type="button" :disabled="pageNo <= 1" @click="previousPage">
          <ChevronLeft :size="17" />
          上一页
        </button>
        <strong>{{ pageNo }} / {{ totalPages }}</strong>
        <button type="button" :disabled="pageNo >= totalPages" @click="nextPage">
          下一页
          <ChevronRight :size="17" />
        </button>
      </div>
      <button type="button" class="refresh-orders" @click="load(orderStore.status, pageNo)">
        <RotateCcw :size="17" />
        刷新
      </button>
    </footer>
  </main>
</template>
