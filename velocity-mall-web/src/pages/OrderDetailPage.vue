<script setup lang="ts">
import { AlertTriangle, ChevronLeft, LoaderCircle, PackageCheck } from 'lucide-vue-next';
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import StatusBadge from '@/components/StatusBadge.vue';
import { pickFallbackCover } from '@/config/media';
import { useOrderStore } from '@/stores/orderStore';
import { money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

const props = defineProps<{ orderSn: string }>();
const router = useRouter();
const orderStore = useOrderStore();
const actionLoading = ref('');
const actionError = ref('');
const actionMessage = ref('');

const order = computed(() => orderStore.current);
const items = computed(() => order.value?.items || []);
const payAmount = computed(() => order.value?.payAmount ?? order.value?.totalAmount ?? 0);

function formatTime(value?: string | null) {
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

function imageFor(src?: string, skuId?: number) {
  return normalizeProductImage(src, pickFallbackCover(skuId));
}

async function run(label: string, action: () => Promise<void>) {
  actionLoading.value = label;
  actionError.value = '';
  actionMessage.value = '';
  try {
    await action();
    actionMessage.value = `${label}成功`;
    await orderStore.loadOrder(props.orderSn);
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : `${label}失败`;
  } finally {
    actionLoading.value = '';
  }
}

function loadOrder() {
  actionError.value = '';
  actionMessage.value = '';
  void orderStore.loadOrder(props.orderSn);
}

onMounted(loadOrder);
watch(() => props.orderSn, loadOrder);
</script>

<template>
  <main class="buyer-page order-detail-page">
    <section class="buyer-page-head">
      <div>
        <span class="eyebrow"><PackageCheck :size="18" /> 订单详情</span>
        <h1>订单 {{ orderSn }}</h1>
        <p>查看商品明细、支付状态和订单操作。</p>
      </div>
      <button type="button" class="outline-action" @click="router.push('/orders')">
        <ChevronLeft :size="18" />
        返回订单
      </button>
    </section>

    <p v-if="actionMessage" class="form-message success">{{ actionMessage }}</p>
    <p v-if="actionError || orderStore.error" class="form-message error">{{ actionError || orderStore.error }}</p>

    <section v-if="orderStore.loading && !order" class="inline-loading">
      <LoaderCircle :size="20" class="spin" />
      正在同步订单详情...
    </section>
    <section v-else-if="!order" class="empty-inline">
      <AlertTriangle :size="22" />
      暂未查到订单详情，请稍后刷新。
    </section>

    <section v-else class="order-detail-layout">
      <article class="order-detail-card">
        <header>
          <h2>商品明细</h2>
          <StatusBadge :status="order.status" />
        </header>
        <div v-for="item in items" :key="`${order.orderSn}-${item.skuId}`" class="order-detail-product">
          <img :src="imageFor(item.skuPic, item.skuId)" :alt="item.skuName" />
          <div>
            <RouterLink :to="`/products/${item.skuId}`">{{ item.skuName }}</RouterLink>
            <span>SKU {{ item.skuId }} · x {{ item.quantity }}</span>
          </div>
          <strong>{{ money(item.skuPrice) }}</strong>
        </div>
      </article>

      <aside class="order-detail-summary">
        <h2>订单信息</h2>
        <dl>
          <div><dt>订单号</dt><dd>{{ order.orderSn }}</dd></div>
          <div><dt>订单类型</dt><dd>{{ order.orderType === 1 ? '秒杀订单' : '普通订单' }}</dd></div>
          <div><dt>下单时间</dt><dd>{{ formatTime(order.createTime) }}</dd></div>
          <div><dt>商品金额</dt><dd>{{ money(order.totalAmount) }}</dd></div>
          <div><dt>实付金额</dt><dd>{{ money(payAmount) }}</dd></div>
        </dl>
        <RouterLink v-if="order.status === 0" class="primary-action full" :to="`/pay/${order.orderSn}`">去支付</RouterLink>
        <button v-if="order.status === 0" class="ghost-action" type="button" :disabled="Boolean(actionLoading)" @click="run('取消订单', () => orderStore.cancel(orderSn))">
          取消订单
        </button>
        <button v-if="order.status === 1" class="ghost-action" type="button" :disabled="Boolean(actionLoading)" @click="run('申请退款', () => orderStore.refund(orderSn))">
          申请退款
        </button>
        <button v-if="order.status === 2" class="primary-action full" type="button" :disabled="Boolean(actionLoading)" @click="run('确认收货', () => orderStore.confirm(orderSn))">
          确认收货
        </button>
      </aside>
    </section>
  </main>
</template>
