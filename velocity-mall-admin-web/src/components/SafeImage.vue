<template>
  <div class="safe-image" :class="className">
    <img v-if="visibleSrc && !failed" :src="visibleSrc" :alt="alt" @error="failed = true" />
    <span v-else>{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { normalizeImageUrl } from '@/utils/images';

const props = withDefaults(
  defineProps<{
    src?: string | null;
    alt?: string;
    label?: string;
    className?: string;
  }>(),
  {
    alt: '',
    label: '无图',
    className: ''
  }
);

const failed = ref(false);
const visibleSrc = computed(() => normalizeImageUrl(props.src));

watch(
  () => props.src,
  () => {
    failed.value = false;
  }
);
</script>
