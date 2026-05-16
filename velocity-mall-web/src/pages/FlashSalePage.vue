<script setup lang="ts">
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  CreditCard,
  ListChecks,
  LoaderCircle,
  PackageCheck,
  ShieldCheck,
  ShoppingCart,
  Star,
  UsersRound,
  Zap
} from 'lucide-vue-next';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import FlashRibbon from '@/components/FlashRibbon.vue';
import ReviewSection from '@/components/ReviewSection.vue';
import SeckillResultPanel from '@/components/SeckillResultPanel.vue';
import type { SkuVO } from '@/api/types';
import { pickFallbackCover } from '@/config/media';
import { useCartStore } from '@/stores/cartStore';
import { useProductStore } from '@/stores/productStore';
import { useSeckillActivityStore } from '@/stores/seckillActivityStore';
import { type SeckillStatus, useSeckillStore } from '@/stores/seckillStore';
import { countdownParts, money } from '@/utils/format';
import { normalizeProductImage } from '@/utils/images';

type DetailStatus = SeckillStatus | 'BEFORE_START';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();
const productStore = useProductStore();
const seckillActivityStore = useSeckillActivityStore();
const seckillStore = useSeckillStore();
const now = ref(Date.now());
const activeImageIndex = ref(0);
const addingCart = ref(false);
const cartMessage = ref('');
let clockTimer: number | null = null;

const skuId = computed(() => {
  const rawSkuId = Number(route.params.skuId);
  return Number.isFinite(rawSkuId) && rawSkuId > 0 ? rawSkuId : 0;
});
const activity = computed(() => seckillActivityStore.activityBySkuId(skuId.value));
const startAt = computed(() => new Date(activity.value?.startTime || now.value).getTime());
const endAt = computed(() => new Date(activity.value?.endTime || now.value).getTime());
const isBeforeStart = computed(() => now.value < startAt.value);
const isEnded = computed(() => now.value > endAt.value);
const countdown = computed(() =>
  countdownParts((isBeforeStart.value ? activity.value?.startTime : activity.value?.endTime) || new Date(now.value).toISOString())
);

const searchRecord = computed(() =>
  productStore.searchPage?.records.find((record) => record.skuId === skuId.value) || null
);

const productTitle = computed(() => productStore.spu?.name || productStore.sku?.skuName || activity.value?.activityName || '商品信息同步中');
const skuDisplayName = computed(() => productStore.sku?.skuName || searchRecord.value?.skuName || '商品信息同步中');
const skuCode = computed(() => productStore.sku?.skuCode || '同步中');
const subtitle = computed(() => {
  if (!productStore.sku?.skuName) {
    return '商品信息同步中';
  }
  return productStore.sku.skuName.replace(productTitle.value, '').trim() || productStore.sku.skuName;
});
const backendImage = computed(() => productStore.sku?.coverImg || searchRecord.value?.skuPic || '');
const productImage = computed(() => normalizeProductImage(backendImage.value, pickFallbackCover(skuId.value)));
const galleryImages = computed(() => {
  const realSkuImages = productStore.spu?.skuList.map((sku) => normalizeProductImage(sku.coverImg)).filter(Boolean) || [];
  const images = [productImage.value, ...realSkuImages].filter(Boolean);
  return [...new Set(images)];
});
const hasMultipleGalleryImages = computed(() => galleryImages.value.length > 1);
const currentImage = computed(() => galleryImages.value[activeImageIndex.value] || productImage.value);
const availableStock = computed(() => activity.value?.remainingStock ?? productStore.sku?.availableStock ?? null);
const saleCount = computed(() => searchRecord.value?.saleCount ?? 0);
const stockText = computed(() => {
  if (productStore.loading && !productStore.sku) {
    return '可售库存同步中';
  }
  if (availableStock.value === null) {
    return '可售库存待同步';
  }
  return `可售库存 ${availableStock.value}`;
});
const stockSubText = computed(() => (productStore.sku ? '库存信息已更新' : '正在获取最新库存'));
const description = computed(() => productStore.spu?.description || '商品描述暂未同步');
const regularPrice = computed(() => productStore.sku?.price ?? activity.value?.originalPrice ?? 0);
const seckillPrice = computed(() => activity.value?.seckillPrice ?? 0);
const realSkuList = computed<SkuVO[]>(() => {
  if (productStore.spu?.skuList?.length) {
    return productStore.spu.skuList;
  }
  return productStore.sku ? [productStore.sku] : [];
});
const showResultPanel = computed(() => Boolean(activity.value) && seckillStore.status !== 'READY' && !isBeforeStart.value);
const spuIdForReviews = computed(() => productStore.sku?.spuId || productStore.spu?.spuId || activity.value?.spuId || 0);

const currentStatus = computed<DetailStatus>(() => {
  if (isBeforeStart.value) {
    return 'BEFORE_START';
  }
  return seckillStore.status;
});

const buttonText = computed(() => {
  if (isBeforeStart.value) {
    return '活动未开始';
  }
  if (isEnded.value) {
    return '活动已结束';
  }
  if (seckillStore.status === 'SUCCESS') {
    return '订单已生成，去支付';
  }
  if (seckillStore.status === 'SOLD_OUT') {
    return '库存不足 / 已抢光';
  }
  if (seckillStore.status === 'DUPLICATE') {
    return '请勿重复抢购';
  }
  if (seckillStore.status === 'LIMITED') {
    return '活动火爆，稍后重试';
  }
  if (seckillStore.status === 'FAILED') {
    return '重新抢购';
  }
  if (seckillStore.status === 'QUEUED') {
    return '排队中';
  }
  return seckillStore.message || '立即抢购';
});

const buttonDisabled = computed(() =>
  !activity.value ||
  seckillActivityStore.loading ||
  isBeforeStart.value ||
  isEnded.value ||
  ['SUBMITTING', 'QUEUED', 'SOLD_OUT', 'DUPLICATE'].includes(seckillStore.status)
);

const flowSteps = computed(() => [
  {
    label: '提交抢购',
    active: ['SUBMITTING', 'QUEUED', 'SUCCESS'].includes(seckillStore.status)
  },
  {
    label: '生成订单',
    active: ['QUEUED', 'SUCCESS'].includes(seckillStore.status)
  },
  {
    label: '去支付',
    active: seckillStore.status === 'SUCCESS'
  }
]);

async function handleSeckill() {
  if (seckillStore.status === 'SUCCESS' && seckillStore.orderSn) {
    await router.push(`/pay/${seckillStore.orderSn}`);
    return;
  }
  if (buttonDisabled.value) {
    return;
  }
  await seckillStore.submit(skuId.value);
}

async function goPay() {
  if (seckillStore.orderSn) {
    await router.push(`/pay/${seckillStore.orderSn}`);
    return;
  }
  await router.push('/orders');
}

function retrySeckill() {
  seckillStore.reset();
}

async function goOrders() {
  await router.push('/orders');
}

async function browseProducts() {
  await router.push('/');
}

async function handleAddToCart() {
  if (!skuId.value || addingCart.value) {
    return;
  }
  addingCart.value = true;
  cartMessage.value = '';
  try {
    await cartStore.addItem(skuId.value, 1);
    cartMessage.value = '已加入购物车，可前往购物车提交普通订单';
  } catch (error) {
    cartMessage.value = error instanceof Error ? error.message : '加入购物车失败';
  } finally {
    addingCart.value = false;
  }
}

async function chooseSku(targetSkuId: number) {
  if (targetSkuId !== skuId.value) {
    await router.push(`/products/${targetSkuId}`);
  }
}

function loadCurrentProduct() {
  seckillStore.reset();
  activeImageIndex.value = 0;
  void seckillActivityStore.loadActivity(skuId.value).then((loadedActivity) => {
    void productStore.loadProduct(skuId.value, loadedActivity?.spuId || activity.value?.spuId || 0);
  });
}

onMounted(() => {
  loadCurrentProduct();
  clockTimer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

watch(skuId, loadCurrentProduct);

onUnmounted(() => {
  seckillStore.clearTimer();
  if (clockTimer !== null) {
    window.clearInterval(clockTimer);
  }
});
</script>

<template>
  <main class="product-page">
    <FlashRibbon
      :sale-count="saleCount"
      :available-stock="activity?.remainingStock ?? availableStock ?? undefined"
      :loading="productStore.loading || seckillActivityStore.loading"
      :start-time="activity?.startTime"
      :end-time="activity?.endTime"
    />

    <section class="detail-layout">
      <aside class="gallery-panel detail-gallery">
        <span class="new-badge">商品图</span>
        <button
          v-if="hasMultipleGalleryImages"
          class="gallery-arrow left"
          type="button"
          aria-label="上一张"
          @click="activeImageIndex = Math.max(0, activeImageIndex - 1)"
        >
          <ChevronLeft :size="24" />
        </button>
        <img class="main-product-image" :src="currentImage" :alt="skuDisplayName" />
        <button
          v-if="hasMultipleGalleryImages"
          class="gallery-arrow right"
          type="button"
          aria-label="下一张"
          @click="activeImageIndex = Math.min(galleryImages.length - 1, activeImageIndex + 1)"
        >
          <ChevronRight :size="24" />
        </button>
        <div v-if="hasMultipleGalleryImages" class="thumb-row real-thumbs">
          <button
            v-for="(thumb, index) in galleryImages"
            :key="thumb"
            class="thumb"
            :class="{ active: index === activeImageIndex }"
            type="button"
            @click="activeImageIndex = index"
          >
            <img :src="thumb" alt="" />
          </button>
        </div>
      </aside>

      <section class="product-info detail-info">
        <div class="tag-row compact-tags">
          <span>秒杀 SKU {{ skuId }}</span>
          <span>{{ productStore.loading ? '信息更新中' : '信息已更新' }}</span>
        </div>
        <h1>{{ productTitle }}</h1>
        <p class="sku-line">{{ subtitle }}</p>
        <p class="product-subtitle">SKU 编码：{{ skuCode }}</p>

        <div class="price-block detail-price">
          <span class="currency">¥</span>
          <strong>{{ seckillPrice }}</strong>
          <em>秒杀价</em>
          <del>{{ money(regularPrice) }}</del>
        </div>

        <div class="sync-row">
          <div class="sync-card primary">
            <LoaderCircle v-if="productStore.loading && !productStore.sku" :size="18" class="spin" />
            <PackageCheck v-else :size="18" />
            <strong>{{ stockText }}</strong>
            <span>{{ stockSubText }}</span>
          </div>
          <div class="sync-card">
            <Clock3 :size="18" />
            <strong>{{ countdown[0] }} : {{ countdown[1] }} : {{ countdown[2] }}</strong>
            <span>{{ isBeforeStart ? '距开始' : '距结束' }}</span>
          </div>
        </div>

        <dl class="real-data-list">
          <div>
            <dt>商品名称</dt>
            <dd>{{ skuDisplayName }}</dd>
          </div>
          <div>
            <dt>商品标识</dt>
            <dd>SKU {{ productStore.sku?.skuId || skuId }} / SPU {{ productStore.sku?.spuId || activity?.spuId || '同步中' }}</dd>
          </div>
          <div>
            <dt>累计销量</dt>
            <dd>{{ saleCount.toLocaleString('zh-CN') }} 件</dd>
          </div>
          <div>
            <dt>同款 SKU</dt>
            <dd class="sku-record-list">
              <button
                v-for="sku in realSkuList"
                :key="sku.skuId"
                class="sku-record"
                :class="{ active: sku.skuId === skuId }"
                type="button"
                @click="chooseSku(sku.skuId)"
              >
                <strong>{{ sku.skuName }}</strong>
                <span>库存 {{ sku.availableStock }} · {{ money(sku.price) }}</span>
              </button>
              <span v-if="realSkuList.length === 0" class="empty-real-data">SKU 列表加载中</span>
            </dd>
          </div>
        </dl>
      </section>

      <aside class="seckill-panel contract-panel">
        <header>
          <h2><Zap :size="25" fill="currentColor" /> 秒杀通道</h2>
          <span>{{ productStore.loading ? '信息更新中' : '信息已更新' }}</span>
        </header>

        <div class="queue-card contract-queue">
          <p>当前商品销量</p>
          <strong><UsersRound :size="38" fill="currentColor" /> {{ saleCount.toLocaleString('zh-CN') }} <span>件</span></strong>
          <small>抢购成功后会排队生成订单，请勿重复提交</small>

          <div class="contract-flow">
            <span v-for="step in flowSteps" :key="step.label" :class="{ active: step.active }">
              {{ step.label }}
            </span>
          </div>
        </div>

        <button
          class="buy-button detail-buy-button"
          :class="`status-${currentStatus.toLowerCase().replace('_', '-')}`"
          :disabled="buttonDisabled"
          type="button"
          @click="handleSeckill"
        >
          <LoaderCircle v-if="['SUBMITTING', 'QUEUED'].includes(seckillStore.status)" :size="25" class="spin" />
          <CreditCard v-else-if="seckillStore.status === 'SUCCESS'" :size="25" />
          <Zap v-else :size="25" fill="currentColor" />
          {{ buttonText }}
        </button>
        <button class="cart-secondary-button" type="button" :disabled="addingCart || productStore.loading" @click="handleAddToCart">
          <LoaderCircle v-if="addingCart" :size="20" class="spin" />
          <ShoppingCart v-else :size="20" />
          加入购物车
        </button>
        <p v-if="cartMessage" class="cart-inline-message">{{ cartMessage }}</p>
        <small class="limit-text">限购 1 件 · 按钮提交后自动锁定，防止重复点击</small>
      </aside>
    </section>

    <SeckillResultPanel
      v-if="showResultPanel && activity"
      :activity="activity"
      :status="seckillStore.status"
      :message="seckillStore.message"
      :order-sn="seckillStore.orderSn"
      :submitted-at="seckillStore.submittedAt"
      :queued-at="seckillStore.queuedAt"
      :order-ready-at="seckillStore.orderReadyAt"
      :product-image="productImage"
      :product-name="productTitle"
      :sku-name="skuDisplayName"
      @pay="goPay"
      @orders="goOrders"
      @retry="retrySeckill"
      @browse="browseProducts"
    />

    <section class="detail-sections real-detail-sections">
      <article class="detail-section spec-section">
        <h2>商品快照</h2>
        <div class="spec-grid">
          <div>
            <span>SKU ID</span>
            <strong>{{ productStore.sku?.skuId || skuId }}</strong>
          </div>
          <div>
            <span>SKU 编码</span>
            <strong>{{ skuCode }}</strong>
          </div>
          <div>
            <span>原价</span>
            <strong>{{ money(regularPrice) }}</strong>
          </div>
        </div>
      </article>

      <article class="detail-section description-section">
        <h2>商品描述</h2>
        <p>{{ description }}</p>
        <small v-if="productStore.error">{{ productStore.error }}。请刷新后重试。</small>
      </article>

      <article class="detail-section guarantee-section">
        <h2>抢购流程</h2>
        <div class="guarantee-grid">
          <div>
            <PackageCheck :size="31" />
            <strong>确认库存</strong>
            <span>成功后进入排队状态</span>
          </div>
          <div>
            <ListChecks :size="31" />
            <strong>生成订单</strong>
            <span>生成订单后跳转支付</span>
          </div>
          <div>
            <ShieldCheck :size="31" />
            <strong>重复提交保护</strong>
            <span>按钮提交后立即锁定</span>
          </div>
        </div>
      </article>

      <article class="detail-section review-section">
        <h2>购买提示</h2>
        <div class="review-summary">
          <div>
            <Star :size="28" fill="currentColor" />
            <strong>商品信息已同步</strong>
          </div>
          <div>
            <CheckCircle2 :size="28" />
            <strong>库存以详情页为准</strong>
          </div>
          <p>请以页面实时显示的商品名称、SKU、价格、库存和订单状态为准。</p>
        </div>
      </article>
    </section>

    <ReviewSection v-if="spuIdForReviews" :spu-id="spuIdForReviews" />

  </main>
</template>
