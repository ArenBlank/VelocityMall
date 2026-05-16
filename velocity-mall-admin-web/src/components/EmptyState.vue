<template>
  <div class="empty-state">
    <Inbox :size="28" />
    <strong>{{ title }}</strong>
    <span>{{ description }}</span>
  </div>
</template>

<script setup lang="ts">
import { Inbox } from 'lucide-vue-next';

withDefaults(
  defineProps<{
    title?: string;
    description?: string;
  }>(),
  {
    title: '暂无数据',
    description: '当前筛选条件下没有可展示记录。'
  }
);
</script>
