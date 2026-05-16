<template>
  <span class="status-badge" :class="tone">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { activityStateText, orderStatusText, publishStatusText } from '@/utils/format';

const props = defineProps<{
  type: 'order' | 'publish' | 'activity' | 'coupon';
  value: number | string | null | undefined;
}>();

const text = computed(() => {
  if (props.type === 'order') return orderStatusText(Number(props.value));
  if (props.type === 'publish') return publishStatusText(Number(props.value));
  if (props.type === 'coupon') return Number(props.value) === 1 ? '启用' : '停用';
  return activityStateText(String(props.value ?? ''));
});

const tone = computed(() => {
  if (props.type === 'order') {
    const value = Number(props.value);
    if (value === 0) return 'warning';
    if (value === 1 || value === 2 || value === 3) return 'success';
    if (value === 5) return 'danger';
    return 'muted';
  }
  if (props.type === 'publish' || props.type === 'coupon') {
    return Number(props.value) === 1 ? 'success' : 'muted';
  }
  const value = String(props.value ?? '');
  if (value === 'ACTIVE') return 'success';
  if (value === 'NOT_STARTED') return 'warning';
  if (value === 'DISABLED') return 'muted';
  return 'danger';
});
</script>
