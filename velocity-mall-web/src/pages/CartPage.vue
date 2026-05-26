<script setup lang="ts">
import { LoaderCircle, MapPin, MinusCircle, ShoppingCart, TicketPercent } from 'lucide-vue-next';
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { useAddressStore } from '@/stores/addressStore';
import { useCartStore } from '@/stores/cartStore';
import { useCouponStore } from '@/stores/couponStore';
import { money } from '@/utils/format';

const router = useRouter();
const cartStore = useCartStore();
const addressStore = useAddressStore();
const couponStore = useCouponStore();
const selectedSkuIds = ref<Set<number>>(new Set());
const selectedAddressId = ref<number | null>(null);
const selectedCouponHistoryId = ref<number | null>(null);
const message = ref('');
const error = ref('');

const selectedItems = computed(() => cartStore.items.filter((item) => selectedSkuIds.value.has(item.skuId)));
const selectedAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0));
const allSelected = computed(() => cartStore.items.length > 0 && selectedSkuIds.value.size === cartStore.items.length);
const applicableCoupons = computed(() =>
  couponStore.unusedCoupons.filter((coupon) => coupon.available && selectedAmount.value >= Number(coupon.minPoint || 0))
);
const selectedCoupon = computed(() =>
  applicableCoupons.value.find((coupon) => coupon.historyId === selectedCouponHistoryId.value) || null
);
const couponDiscount = computed(() =>
  selectedCoupon.value ? Math.min(Number(selectedCoupon.value.amount || 0), selectedAmount.value) : 0
);
const payableAmount = computed(() => Math.max(selectedAmount.value - couponDiscount.value, 0));

function toggleItem(skuId: number) {
  const next = new Set(selectedSkuIds.value);
  if (next.has(skuId)) {
    next.delete(skuId);
  } else {
    next.add(skuId);
  }
  selectedSkuIds.value = next;
}

function toggleAll() {
  selectedSkuIds.value = allSelected.value ? new Set() : new Set(cartStore.items.map((item) => item.skuId));
}

async function removeItem(skuId: number) {
  message.value = '';
  error.value = '';
  await cartStore.removeItem(skuId);
  const next = new Set(selectedSkuIds.value);
  next.delete(skuId);
  selectedSkuIds.value = next;
}

async function submitCartOrder() {
  message.value = '';
  error.value = '';
  if (selectedItems.value.length === 0) {
    error.value = '请至少选择一个商品';
    return;
  }
  if (!selectedAddressId.value) {
    error.value = '请先选择收货地址';
    return;
  }
  try {
    const order = await cartStore.submitSelected(
      [...selectedSkuIds.value],
      selectedAddressId.value,
      selectedCouponHistoryId.value
    );
    message.value = '订单已创建，正在前往支付页';
    await router.push(`/pay/${order.orderSn}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '订单提交失败';
  }
}

onMounted(async () => {
  await Promise.all([cartStore.loadItems(), addressStore.loadAddresses(), couponStore.loadCoupons()]);
  selectedSkuIds.value = new Set(cartStore.items.map((item) => item.skuId));
  selectedAddressId.value = addressStore.defaultAddress?.id || null;
});

watch([selectedAmount, applicableCoupons], () => {
  if (selectedCouponHistoryId.value && !selectedCoupon.value) {
    selectedCouponHistoryId.value = null;
  }
});

watch(
  () => addressStore.defaultAddress?.id,
  (id) => {
    if (!selectedAddressId.value && id) {
      selectedAddressId.value = id;
    }
  }
);
</script>

<template>
  <main class="buyer-page cart-page">
    <section class="buyer-page-head">
      <div>
        <span class="eyebrow"><ShoppingCart :size="18" /> 购物车</span>
        <h1>购物车结算</h1>
        <p>购物车商品来自当前账号，提交后会创建普通订单并进入模拟支付。</p>
      </div>
      <RouterLink class="outline-action" to="/">继续逛逛</RouterLink>
    </section>

    <section class="cart-workspace">
      <section class="cart-list-panel">
        <header class="cart-list-head">
          <label>
            <input type="checkbox" :checked="allSelected" @change="toggleAll" />
            全选
          </label>
          <span>{{ cartStore.items.length }} 件商品</span>
        </header>

        <div v-if="cartStore.loading" class="inline-loading">
          <LoaderCircle :size="20" class="spin" />
          正在加载购物车…
        </div>
        <div v-else-if="cartStore.items.length === 0" class="empty-inline">
          购物车暂无商品，可以先进入秒杀详情页加入商品。
        </div>
        <article v-for="item in cartStore.items" v-else :key="item.skuId" class="cart-item-row">
          <input type="checkbox" :checked="selectedSkuIds.has(item.skuId)" @change="toggleItem(item.skuId)" />
          <RouterLink :to="`/products/${item.skuId}`" class="cart-thumb">
            <img :src="cartStore.imageFor(item.skuId)" :alt="item.skuName" loading="lazy" />
          </RouterLink>
          <div class="cart-info">
            <RouterLink :to="`/products/${item.skuId}`">{{ item.skuName }}</RouterLink>
            <span>可售库存 {{ item.availableStock }}</span>
          </div>
          <strong>{{ money(item.price) }}</strong>
          <span>x {{ item.quantity }}</span>
          <b>{{ money(item.totalAmount) }}</b>
          <button type="button" class="danger-text" @click="removeItem(item.skuId)">
            <MinusCircle :size="16" />
            移除
          </button>
        </article>
      </section>

      <aside class="checkout-panel">
        <h2>确认结算</h2>
        <label class="address-select">
          <span><MapPin :size="17" /> 收货地址</span>
          <select v-model.number="selectedAddressId">
            <option :value="null">请选择地址</option>
            <option v-for="address in addressStore.items" :key="address.id" :value="address.id">
              {{ address.receiverName }} - {{ address.city }}{{ address.region }}
            </option>
          </select>
        </label>
        <RouterLink v-if="addressStore.items.length === 0" to="/addresses" class="inline-link">先新增地址</RouterLink>
        <label class="coupon-select">
          <span><TicketPercent :size="17" /> 优惠券</span>
          <select v-model.number="selectedCouponHistoryId" :disabled="couponStore.loading || applicableCoupons.length === 0">
            <option :value="null">{{ applicableCoupons.length ? '不使用优惠券' : '暂无可用优惠券' }}</option>
            <option v-for="coupon in applicableCoupons" :key="coupon.historyId" :value="coupon.historyId">
              {{ coupon.name }} - 立减 {{ money(coupon.amount) }}，满 {{ money(coupon.minPoint) }} 可用
            </option>
          </select>
          <RouterLink class="inline-link" to="/coupons">去领券</RouterLink>
        </label>
        <dl>
          <div>
            <dt>已选商品</dt>
            <dd>{{ selectedItems.length }} 件</dd>
          </div>
          <div>
            <dt>合计金额</dt>
            <dd>{{ money(selectedAmount) }}</dd>
          </div>
          <div>
            <dt>优惠抵扣</dt>
            <dd>- {{ money(couponDiscount) }}</dd>
          </div>
          <div class="checkout-payable">
            <dt>应付金额</dt>
            <dd>{{ money(payableAmount) }}</dd>
          </div>
        </dl>
        <button type="button" class="primary-action full" :disabled="cartStore.submitting" @click="submitCartOrder">
          <LoaderCircle v-if="cartStore.submitting" :size="18" class="spin" />
          提交普通订单
        </button>
        <p v-if="message" class="form-message success">{{ message }}</p>
        <p v-if="error || cartStore.error || addressStore.error" class="form-message error">
          {{ error || cartStore.error || addressStore.error }}
        </p>
      </aside>
    </section>
  </main>
</template>
