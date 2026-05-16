<template>
  <div class="pager" v-if="total > 0">
    <span>共 {{ total }} 条</span>
    <button type="button" class="ghost-button" :disabled="page <= 1" @click="$emit('change', page - 1)">上一页</button>
    <strong>{{ page }} / {{ Math.max(pages, 1) }}</strong>
    <button type="button" class="ghost-button" :disabled="page >= pages" @click="$emit('change', page + 1)">下一页</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  page: number;
  pages: number;
  total: number;
}>();

defineEmits<{
  change: [page: number];
}>();
</script>
