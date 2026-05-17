<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>评价管理</h1>
        <p>查看商品评价，可删除不合规评价。</p>
      </div>
    </div>
    <form class="filters panel" @submit.prevent="load(1)">
      <label>SPU ID<input v-model.number="filters.spuId" min="1" type="number" /></label>
      <label>关键词<input v-model.trim="filters.keyword" placeholder="评价内容" /></label>
      <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
      <button class="outline-button" type="button" @click="reset">重置</button>
    </form>
    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>评价</th>
            <th>商品</th>
            <th>用户</th>
            <th>互动</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in records" :key="review.id">
            <td><strong>{{ review.rating }} 分</strong><br /><span>{{ review.content }}</span></td>
            <td>SPU {{ review.spuId }}<br /><span class="subtext">SKU {{ review.skuId }}</span></td>
            <td>{{ review.userId }}<br /><span class="subtext">{{ review.orderSn }}</span></td>
            <td>赞 {{ review.likeCount }} · 踩 {{ review.dislikeCount }}</td>
            <td>{{ formatTime(review.createTime) }}</td>
            <td><button v-if="canDeleteReview" class="danger-button compact" type="button" @click="remove(review.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="!store.loading && records.length === 0" />
      <Pager v-if="store.page" :page="store.page.current" :pages="store.page.pages" :total="store.page.total" @change="load" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { Search } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import { AdminPermissions } from '@/constants/permissions';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { useAdminReviewStore } from '@/stores/adminReviewStore';
import { formatTime } from '@/utils/format';

const auth = useAdminAuthStore();
const store = useAdminReviewStore();
const records = computed(() => store.page?.records || []);
const canDeleteReview = computed(() => auth.hasPermission(AdminPermissions.REVIEW_DELETE));
const filters = reactive({ spuId: null as number | null, keyword: '' });

async function load(page = 1) {
  await store.load({ page, size: 10, spuId: filters.spuId, keyword: filters.keyword });
}

function reset() {
  filters.spuId = null;
  filters.keyword = '';
  void load(1);
}

async function remove(id: number) {
  if (!window.confirm('确认删除该评价？')) return;
  await store.remove(id);
  await load(store.page?.current || 1);
}

onMounted(() => {
  void load(1);
});
</script>
