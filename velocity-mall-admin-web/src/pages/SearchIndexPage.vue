<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>搜索索引工具</h1>
        <p>当商品新增、修改或上下架后，可手动重建 SKU 搜索索引。</p>
      </div>
    </div>
    <div class="panel panel-body">
      <h2>SKU 索引重建</h2>
      <p class="muted">该操作调用管理端真实接口，并由搜索服务从商品服务拉取可搜索 SKU。</p>
      <button class="primary-button" type="button" :disabled="store.loading" @click="rebuild">
        <LoaderCircle v-if="store.loading" :size="17" class="spin" />
        重建 SKU 搜索索引
      </button>
      <div class="message" :class="{ success: Boolean(store.result), error: Boolean(store.error) }">
        <template v-if="store.result">
          {{ store.result.message }}，已索引 {{ store.result.indexedCount }} 个 SKU
        </template>
        <template v-else>
          {{ store.error || '等待执行。' }}
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { LoaderCircle } from 'lucide-vue-next';
import { useAdminSystemStore } from '@/stores/adminSystemStore';

const store = useAdminSystemStore();

function rebuild() {
  void store.rebuildSkuIndex();
}
</script>
