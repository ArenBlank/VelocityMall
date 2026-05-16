<script setup lang="ts">
import { LoaderCircle, MessageSquare, ThumbsDown, ThumbsUp, Trash2 } from 'lucide-vue-next';
import { computed, onMounted, ref, watch } from 'vue';

import { deleteReview, getProductReviewStats, interactReview, listProductReviews } from '@/api/reviewApi';
import type { PageVO, ReviewStatsVO, ReviewVO } from '@/api/types';

const props = defineProps<{
  spuId: number;
}>();

const loading = ref(false);
const error = ref('');
const stats = ref<ReviewStatsVO | null>(null);
const page = ref<PageVO<ReviewVO> | null>(null);
const deletingId = ref<number | null>(null);
const pageNo = ref(1);
const pageSize = 5;

const reviews = computed(() => page.value?.records || []);
const totalPages = computed(() => Math.max(page.value?.pages || 1, 1));
const goodRate = computed(() => {
  if (!stats.value) {
    return '-';
  }
  return `${Number(stats.value.goodRate || 0).toFixed(1)}%`;
});

function formatTime(value: string) {
  if (!value) {
    return '-';
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(parsed);
}

async function loadReviews() {
  if (!props.spuId) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const [nextStats, nextPage] = await Promise.all([
      getProductReviewStats(props.spuId),
      listProductReviews(props.spuId, { page: pageNo.value, size: pageSize })
    ]);
    stats.value = nextStats;
    page.value = nextPage;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评价加载失败';
  } finally {
    loading.value = false;
  }
}

async function interact(review: ReviewVO, type: 1 | 2) {
  await interactReview(review.id, type);
  await loadReviews();
}

async function removeReview(review: ReviewVO) {
  deletingId.value = review.id;
  error.value = '';
  try {
    await deleteReview(review.id);
    await loadReviews();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评价删除失败';
  } finally {
    deletingId.value = null;
  }
}

async function previousPage() {
  if (pageNo.value <= 1) {
    return;
  }
  pageNo.value -= 1;
  await loadReviews();
}

async function nextPage() {
  if (pageNo.value >= totalPages.value) {
    return;
  }
  pageNo.value += 1;
  await loadReviews();
}

onMounted(loadReviews);
watch(
  () => props.spuId,
  () => {
    pageNo.value = 1;
    void loadReviews();
  }
);
</script>

<template>
  <section class="detail-section buyer-review-section">
    <header>
      <div>
        <h2>买家评价</h2>
        <p>评价数据来自商品评价接口。</p>
      </div>
      <div class="review-stats-pill">
        <strong>{{ goodRate }}</strong>
        <span>好评率</span>
        <em>{{ stats?.totalCount || 0 }} 条评价</em>
      </div>
    </header>

    <div v-if="loading" class="inline-loading">
      <LoaderCircle :size="20" class="spin" />
      正在加载评价...
    </div>
    <div v-else-if="error" class="form-message error">{{ error }}</div>
    <div v-else-if="reviews.length === 0" class="empty-inline">
      暂无评价。完成订单后可以在“我的订单”中评价商品。
    </div>
    <article v-for="review in reviews" v-else :key="review.id" class="review-item">
      <div>
        <strong>{{ review.rating }} 星</strong>
        <time>{{ formatTime(review.createTime) }}</time>
      </div>
      <p>{{ review.content }}</p>
      <footer>
        <button type="button" :class="{ active: review.currentInteractionType === 1 }" @click="interact(review, 1)">
          <ThumbsUp :size="15" />
          {{ review.likeCount }}
        </button>
        <button type="button" :class="{ active: review.currentInteractionType === 2 }" @click="interact(review, 2)">
          <ThumbsDown :size="15" />
          {{ review.dislikeCount }}
        </button>
        <span>
          <MessageSquare :size="15" />
          {{ review.replyCount }}
        </span>
        <button v-if="review.mine" type="button" class="danger-text review-delete-button" @click="removeReview(review)">
          <LoaderCircle v-if="deletingId === review.id" :size="15" class="spin" />
          <Trash2 v-else :size="15" />
          删除
        </button>
      </footer>
    </article>
    <footer v-if="page && page.total > pageSize" class="review-pagination">
      <button type="button" :disabled="pageNo <= 1 || loading" @click="previousPage">上一页</button>
      <span>{{ pageNo }} / {{ totalPages }}</span>
      <button type="button" :disabled="pageNo >= totalPages || loading" @click="nextPage">下一页</button>
    </footer>
  </section>
</template>
