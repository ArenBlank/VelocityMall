<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import {
  AlertTriangle,
  CheckCircle2,
  CreditCard,
  LoaderCircle,
} from 'lucide-vue-next';

import StatusBadge from '@/components/StatusBadge.vue';
import { pickFallbackCover } from '@/config/media';
import { useOrderStore } from '@/stores/orderStore';
import { money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

const props = defineProps<{ orderSn: string }>();
const router = useRouter();
const orderStore = useOrderStore();
const error = ref('');
const paying = ref(false);
const paymentSuccess = ref(false);

const order = computed(() => orderStore.current);
const primaryItem = computed(() => order.value?.items?.[0] || null);
const quantity = computed(() => order.value?.items?.reduce((sum, item) => sum + Number(item.quantity || 0), 0) || 1);
const productImage = computed(() => normalizeProductImage(primaryItem.value?.skuPic, pickFallbackCover(primaryItem.value?.skuId)));
const productName = computed(() => primaryItem.value?.skuName || '订单商品');
const itemPrice = computed(() => primaryItem.value?.skuPrice ?? 0);
const amount = computed(() => order.value?.payAmount ?? order.value?.totalAmount ?? 0);
const isPaid = computed(() => paymentSuccess.value || order.value?.status === 1);
const status = computed(() => (isPaid.value ? 1 : order.value?.status ?? 0));
const canPay = computed(() => Boolean(order.value) && status.value === 0 && !paying.value);

async function pay() {
  if (!canPay.value) {
    return;
  }
  error.value = '';
  paying.value = true;
  try {
    await orderStore.pay(props.orderSn);
    paymentSuccess.value = true;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '支付失败，请稍后重试';
  } finally {
    paying.value = false;
  }
}

function loadOrder() {
  error.value = '';
  paymentSuccess.value = false;
  void orderStore.loadOrder(props.orderSn).catch((err) => {
    error.value = err instanceof Error ? err.message : '订单详情加载失败';
  });
}

async function backToOrders() {
  await router.push('/orders');
}

onMounted(loadOrder);

watch(
  () => props.orderSn,
  () => loadOrder()
);
</script>

<template>
  <main class="payment-page">
    <section class="payment-header">
      <div>
        <h1>确认支付</h1>
        <p>订单生成后请尽快完成支付，超时订单可能会自动关闭。</p>
      </div>
      <StatusBadge :status="status" />
    </section>

    <section class="payment-layout">
      <article class="payment-main-card">
        <header class="payment-order-head">
          <span>订单号：{{ orderSn }}</span>
          <b>{{ isPaid ? '支付完成' : '待支付' }}</b>
        </header>

        <div v-if="orderStore.loading && !order" class="payment-loading">
          <LoaderCircle :size="28" class="spin" />
          正在同步订单信息...
        </div>

        <div v-else-if="!order" class="payment-empty">
          <AlertTriangle :size="30" />
          <strong>订单详情暂不可用</strong>
          <span>{{ error || orderStore.error || '请稍后重试，或返回我的订单确认状态。' }}</span>
          <button type="button" @click="loadOrder">重新同步</button>
        </div>

        <template v-else>
          <div class="payment-product">
            <img :src="productImage" :alt="productName" />
            <div>
              <strong>{{ productName }}</strong>
              <span>数量 x{{ quantity }}</span>
              <small>单价 {{ money(itemPrice) }}</small>
            </div>
            <em>{{ money(amount) }}</em>
          </div>

          <div class="payment-amount-box">
            <span>应付金额</span>
            <strong>{{ money(amount) }}</strong>
            <small>当前为演示支付，不接入真实支付渠道</small>
          </div>

          <p v-if="error || orderStore.error" class="payment-error">
            <AlertTriangle :size="18" />
            {{ error || orderStore.error }}
          </p>

          <div class="payment-actions">
            <button v-if="!isPaid" class="payment-primary" type="button" :disabled="!canPay" @click="pay">
              <LoaderCircle v-if="paying" :size="22" class="spin" />
              <CreditCard v-else :size="22" />
              {{ paying ? '支付处理中' : '模拟支付' }}
            </button>
            <button v-else class="payment-success-button" type="button" @click="backToOrders">
              <CheckCircle2 :size="22" />
              支付成功，返回我的订单
            </button>

            <RouterLink class="payment-secondary" to="/orders">返回我的订单</RouterLink>
          </div>
        </template>
      </article>

      <aside class="payment-side-card" :class="{ success: isPaid }">
        <div class="payment-state-icon">
          <CheckCircle2 v-if="isPaid" :size="52" />
          <CreditCard v-else :size="52" />
        </div>
        <h2>{{ isPaid ? '支付成功，返回我的订单' : '待支付确认' }}</h2>
        <p>
          {{
            isPaid
              ? '订单状态已更新，您可以回到我的订单查看后续履约状态。'
              : '请确认订单信息与金额，点击模拟支付完成演示流程。'
          }}
        </p>
      </aside>
    </section>
  </main>
</template>
