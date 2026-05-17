import { request } from './http';
import type { PageVO, ReviewReplyVO, ReviewStatsVO, ReviewVO } from './types';

export interface ReviewCreatePayload {
  orderSn: string;
  skuId: number;
  spuId: number;
  rating: number;
  content: string;
}

export function createReview(data: ReviewCreatePayload) {
  return request<void>({
    url: '/api/v1/reviews',
    method: 'POST',
    data
  });
}

export function listProductReviews(spuId: number, params: { page?: number; size?: number } = {}) {
  return request<PageVO<ReviewVO>>({
    url: `/api/v1/reviews/products/${spuId}`,
    method: 'GET',
    params: {
      page: params.page ?? 1,
      size: params.size ?? 5
    }
  });
}

export function getProductReviewStats(spuId: number) {
  return request<ReviewStatsVO>({
    url: `/api/v1/reviews/products/${spuId}/stats`,
    method: 'GET'
  });
}

export function interactReview(reviewId: string | number, interactionType: 1 | 2) {
  return request<void>({
    url: `/api/v1/reviews/${reviewId}/interaction`,
    method: 'POST',
    data: { interactionType }
  });
}

export function deleteReview(reviewId: string | number) {
  return request<void>({
    url: `/api/v1/reviews/${reviewId}`,
    method: 'DELETE'
  });
}

export function listReviewReplies(reviewId: string | number, params: { page?: number; size?: number } = {}) {
  return request<PageVO<ReviewReplyVO>>({
    url: `/api/v1/reviews/${reviewId}/replies`,
    method: 'GET',
    params: {
      page: params.page ?? 1,
      size: params.size ?? 10
    }
  });
}

export function createReviewReply(reviewId: string | number, content: string) {
  return request<void>({
    url: `/api/v1/reviews/${reviewId}/replies`,
    method: 'POST',
    data: { content }
  });
}

export function deleteReviewReply(reviewId: string | number, replyId: string | number) {
  return request<void>({
    url: `/api/v1/reviews/${reviewId}/replies/${replyId}`,
    method: 'DELETE'
  });
}
