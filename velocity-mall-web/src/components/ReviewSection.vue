<script setup lang="ts">
import { LoaderCircle, MessageSquare, SendHorizonal, ThumbsDown, ThumbsUp, Trash2 } from 'lucide-vue-next';
import { computed, reactive, ref, watch } from 'vue';

import {
  createReviewReply,
  deleteReview,
  deleteReviewReply,
  getProductReviewStats,
  interactReview,
  listProductReviews,
  listReviewReplies
} from '@/api/reviewApi';
import type { PageVO, ReviewReplyVO, ReviewStatsVO, ReviewVO } from '@/api/types';

const props = defineProps<{
  spuId: number;
}>();

const loading = ref(false);
const error = ref('');
const stats = ref<ReviewStatsVO | null>(null);
const page = ref<PageVO<ReviewVO> | null>(null);
const deletingId = ref<string | null>(null);
const interactingId = ref<string | null>(null);
const activeReplyReviewId = ref<string | null>(null);
const replyLoadingId = ref<string | null>(null);
const replySubmittingId = ref<string | null>(null);
const deletingReplyId = ref<string | null>(null);
const replyDrafts = reactive<Record<string, string>>({});
const replyPages = reactive<Record<string, PageVO<ReviewReplyVO> | undefined>>({});
const pageNo = ref(1);
const pageSize = 5;
const replyPageSize = 10;

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

function getReplyPage(reviewId: string) {
  return replyPages[reviewId];
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
  if (interactingId.value) {
    return;
  }
  interactingId.value = review.id;
  error.value = '';
  try {
    await interactReview(review.id, type);
    await loadReviews();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '互动失败，请稍后再试';
  } finally {
    interactingId.value = null;
  }
}

async function loadReplies(reviewId: string) {
  replyLoadingId.value = reviewId;
  error.value = '';
  try {
    replyPages[reviewId] = await listReviewReplies(reviewId, { page: 1, size: replyPageSize });
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回复加载失败';
  } finally {
    replyLoadingId.value = null;
  }
}

async function toggleReplies(review: ReviewVO) {
  if (activeReplyReviewId.value === review.id) {
    activeReplyReviewId.value = null;
    return;
  }
  activeReplyReviewId.value = review.id;
  if (!replyPages[review.id]) {
    await loadReplies(review.id);
  }
}

async function submitReply(review: ReviewVO) {
  const content = (replyDrafts[review.id] || '').trim();
  if (!content) {
    error.value = '请先填写回复内容';
    return;
  }
  replySubmittingId.value = review.id;
  error.value = '';
  try {
    await createReviewReply(review.id, content);
    replyDrafts[review.id] = '';
    await Promise.all([loadReplies(review.id), loadReviews()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回复失败，请稍后再试';
  } finally {
    replySubmittingId.value = null;
  }
}

async function removeReply(review: ReviewVO, reply: ReviewReplyVO) {
  deletingReplyId.value = reply.id;
  error.value = '';
  try {
    await deleteReviewReply(review.id, reply.id);
    await Promise.all([loadReplies(review.id), loadReviews()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回复删除失败';
  } finally {
    deletingReplyId.value = null;
  }
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

watch(
  () => props.spuId,
  () => {
    pageNo.value = 1;
    activeReplyReviewId.value = null;
    void loadReviews();
  },
  { immediate: true }
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
      正在加载评价…
    </div>
    <div v-else-if="error" class="form-message error">{{ error }}</div>
    <div v-else-if="reviews.length === 0" class="empty-inline">
      暂无评价。完成订单后可以在“我的订单”中评价商品。
    </div>
    <template v-else>
      <article v-for="review in reviews" :key="review.id" class="review-item">
        <div>
          <strong>{{ review.rating }} 星</strong>
          <time>{{ formatTime(review.createTime) }}</time>
        </div>
        <p>{{ review.content }}</p>
        <footer>
          <button
            type="button"
            :class="{ active: review.currentInteractionType === 1 }"
            :disabled="interactingId === review.id"
            @click="interact(review, 1)"
          >
            <LoaderCircle v-if="interactingId === review.id" :size="15" class="spin" />
            <ThumbsUp v-else :size="15" />
            {{ review.likeCount }}
          </button>
          <button
            type="button"
            :class="{ active: review.currentInteractionType === 2 }"
            :disabled="interactingId === review.id"
            @click="interact(review, 2)"
          >
            <LoaderCircle v-if="interactingId === review.id" :size="15" class="spin" />
            <ThumbsDown v-else :size="15" />
            {{ review.dislikeCount }}
          </button>
          <button
            type="button"
            :class="{ active: activeReplyReviewId === review.id }"
            @click="toggleReplies(review)"
          >
            <MessageSquare :size="15" />
            {{ review.replyCount }}
          </button>
          <button v-if="review.mine" type="button" class="danger-text review-delete-button" @click="removeReview(review)">
            <LoaderCircle v-if="deletingId === review.id" :size="15" class="spin" />
            <Trash2 v-else :size="15" />
            删除
          </button>
        </footer>

        <section v-if="activeReplyReviewId === review.id" class="review-reply-panel">
          <div v-if="replyLoadingId === review.id" class="inline-loading compact">
            <LoaderCircle :size="16" class="spin" />
            正在加载回复…
          </div>
          <div v-else-if="getReplyPage(review.id)?.records.length" class="review-reply-list">
            <article v-for="reply in getReplyPage(review.id)?.records" :key="reply.id" class="review-reply-item">
              <div>
                <strong>用户 {{ reply.userId }}</strong>
                <time>{{ formatTime(reply.createTime) }}</time>
              </div>
              <p>{{ reply.content }}</p>
              <button v-if="reply.mine" type="button" @click="removeReply(review, reply)">
                <LoaderCircle v-if="deletingReplyId === reply.id" :size="14" class="spin" />
                <Trash2 v-else :size="14" />
                删除
              </button>
            </article>
          </div>
          <div v-else class="empty-inline compact">暂无回复，来做第一个回复的人。</div>

          <form class="review-reply-form" @submit.prevent="submitReply(review)">
            <input
              v-model.trim="replyDrafts[review.id]"
              maxlength="500"
              placeholder="写下你的回复…"
              autocomplete="off"
            />
            <button type="submit" :disabled="replySubmittingId === review.id">
              <LoaderCircle v-if="replySubmittingId === review.id" :size="16" class="spin" />
              <SendHorizonal v-else :size="16" />
              回复
            </button>
          </form>
        </section>
      </article>
    </template>
    <footer v-if="page && page.total > pageSize" class="review-pagination">
      <button type="button" :disabled="pageNo <= 1 || loading" @click="previousPage">上一页</button>
      <span>{{ pageNo }} / {{ totalPages }}</span>
      <button type="button" :disabled="pageNo >= totalPages || loading" @click="nextPage">下一页</button>
    </footer>
  </section>
</template>
