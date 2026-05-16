<script setup lang="ts">
import {
  AlertTriangle,
  Bell,
  CheckCircle2,
  CircleX,
  Flame,
  LoaderCircle,
  Megaphone,
  Zap
} from 'lucide-vue-next';
import { computed } from 'vue';

import type { SeckillActivityVO } from '@/api/types';
import type { SeckillStatus } from '@/stores/seckillStore';
import { money } from '@/utils/format';

const props = defineProps<{
  activity: SeckillActivityVO;
  status: SeckillStatus;
  message: string;
  orderSn: string;
  submittedAt: number | null;
  queuedAt: number | null;
  orderReadyAt: number | null;
  productImage?: string;
  productName?: string;
  skuName?: string;
}>();

const emit = defineEmits<{
  pay: [];
  orders: [];
  retry: [];
  browse: [];
}>();

type ResultTone = 'queue' | 'success' | 'soldout' | 'duplicate' | 'limited';

const visibleStatus = computed(() => props.status);
const isQueueing = computed(() => ['SUBMITTING', 'QUEUED'].includes(props.status));

const resultCopy = computed<{
  tone: ResultTone;
  title: string;
  subtext: string;
  action: string;
  actionKind: 'pay' | 'orders' | 'retry' | 'browse';
}>(() => {
  if (props.status === 'SUCCESS') {
    return {
      tone: 'success',
      title: '抢购成功，去支付',
      subtext: '订单已生成，请尽快完成支付',
      action: '立即支付',
      actionKind: 'pay'
    };
  }
  if (props.status === 'SOLD_OUT') {
    return {
      tone: 'soldout',
      title: '商品已抢光',
      subtext: '很抱歉，本场商品已经售罄',
      action: '返回首页',
      actionKind: 'browse'
    };
  }
  if (props.status === 'DUPLICATE') {
    return {
      tone: 'duplicate',
      title: '请勿重复抢购',
      subtext: '当前账号已有有效的抢购记录',
      action: '查看订单',
      actionKind: 'orders'
    };
  }
  if (props.status === 'LIMITED') {
    return {
      tone: 'limited',
      title: '活动火爆，请稍后重试',
      subtext: '当前参与人数较多，请稍后再试',
      action: '稍后重试',
      actionKind: 'retry'
    };
  }
  if (props.status === 'FAILED') {
    return {
      tone: 'limited',
      title: props.message || '排队超时，请稍后重试',
      subtext: '你也可以到我的订单中确认订单状态',
      action: '查看订单',
      actionKind: 'orders'
    };
  }
  return {
    tone: 'queue',
    title: '抢购成功，正在排队生成订单',
    subtext: '系统正在确认订单结果，请勿重复提交',
    action: '查看订单',
    actionKind: 'orders'
  };
});

const productName = computed(() => props.productName || props.activity.activityName);
const skuName = computed(() => props.skuName || `SKU ${props.activity.skuId}`);
const resultImage = computed(() => props.productImage || '');

const steps = computed(() => {
  const submitTime = formatTime(props.submittedAt || props.queuedAt);
  const queueTime = formatTime(props.queuedAt);
  const readyTime = formatTime(props.orderReadyAt);
  const isSuccess = props.status === 'SUCCESS';
  const isQueued = props.status === 'QUEUED';
  const isSubmitting = props.status === 'SUBMITTING';

  return [
    {
      label: '已提交抢购',
      time: submitTime || '等待提交',
      state: isSubmitting || isQueued || isSuccess ? 'done' : 'pending'
    },
    {
      label: '排队生成订单',
      time: queueTime || '等待排队',
      state: isQueued || isSuccess ? 'done' : isSubmitting ? 'active' : 'pending'
    },
    {
      label: '订单生成中',
      time: isSuccess ? readyTime || '已完成' : '预计稍后完成',
      state: isSuccess ? 'done' : isQueued ? 'active' : 'pending'
    },
    {
      label: '等待支付',
      time: isSuccess ? '请尽快支付' : '请留意订单状态',
      state: isSuccess ? 'active' : 'pending'
    }
  ];
});

function formatTime(value: number | null | undefined) {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(value);
}

function handlePrimaryAction() {
  const action = resultCopy.value.actionKind;
  if (action === 'pay') {
    emit('pay');
  } else if (action === 'retry') {
    emit('retry');
  } else if (action === 'browse') {
    emit('browse');
  } else {
    emit('orders');
  }
}
</script>

<template>
  <section class="result-panel" :class="`tone-${resultCopy.tone}`">
    <header class="result-product">
      <img :src="resultImage" :alt="`${productName} ${skuName}`" />
      <div>
        <h2>{{ productName }}</h2>
        <p>{{ skuName }}</p>
        <div class="result-price">
          <span>¥</span>
          <strong>{{ activity.seckillPrice }}</strong>
          <em>秒杀价</em>
          <del>{{ money(activity.originalPrice) }}</del>
        </div>
      </div>
      <b><Zap :size="20" fill="currentColor" /> 限时秒杀</b>
    </header>

    <div class="result-main">
      <div class="queue-loader" :class="{ still: !isQueueing }">
        <LoaderCircle v-if="isQueueing" :size="54" class="spin" />
        <CheckCircle2 v-else-if="visibleStatus === 'SUCCESS'" :size="58" />
        <CircleX v-else-if="visibleStatus === 'SOLD_OUT'" :size="58" />
        <AlertTriangle v-else-if="visibleStatus === 'DUPLICATE'" :size="58" />
        <Flame v-else-if="visibleStatus === 'LIMITED'" :size="58" fill="currentColor" />
        <Bell v-else :size="56" />
      </div>

      <h3>{{ resultCopy.title }}</h3>
      <p>{{ resultCopy.subtext }}</p>
      <button v-if="!isQueueing" class="result-primary-action" type="button" @click="handlePrimaryAction">
        {{ resultCopy.action }}
      </button>

      <div class="result-steps">
        <div v-for="step in steps" :key="step.label" class="result-step" :class="step.state">
          <span>
            <CheckCircle2 v-if="step.state === 'done'" :size="18" />
            <LoaderCircle v-else-if="step.state === 'active'" :size="18" class="spin" />
          </span>
          <strong>{{ step.label }}</strong>
          <small>{{ step.time }}</small>
        </div>
      </div>

      <div class="queue-notice">
        <Megaphone :size="22" />
        <span>排队期间请勿重复提交，可稍后在“我的订单”中查看订单状态。</span>
        <button type="button" @click="emit('orders')">查看订单</button>
      </div>
    </div>
  </section>
</template>
