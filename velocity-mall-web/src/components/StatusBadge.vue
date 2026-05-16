<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status: number;
}>();

const labels: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已关闭',
  5: '已退款'
};

const tones: Record<number, string> = {
  0: 'pending',
  1: 'paid',
  2: 'shipping',
  3: 'done',
  4: 'closed',
  5: 'refund'
};

const label = computed(() => labels[props.status] || '未知');
const tone = computed(() => tones[props.status] || 'unknown');
</script>

<template>
  <span class="status-badge" :class="`status-${tone}`">{{ label }}</span>
</template>
