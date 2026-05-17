package com.velocitymall.review.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.review.client.OrderFeignClient;
import com.velocitymall.review.entity.ProductReview;
import com.velocitymall.review.entity.ReviewInteraction;
import com.velocitymall.review.entity.ReviewReply;
import com.velocitymall.review.mapper.ProductReviewMapper;
import com.velocitymall.review.mapper.ReviewInteractionMapper;
import com.velocitymall.review.mapper.ReviewReplyMapper;
import com.velocitymall.review.model.dto.ReviewCreateDTO;
import com.velocitymall.review.model.dto.ReviewInteractionDTO;
import com.velocitymall.review.model.dto.ReviewReplyCreateDTO;
import com.velocitymall.review.model.vo.ReviewReplyVO;
import com.velocitymall.review.model.vo.ReviewStatsVO;
import com.velocitymall.review.model.vo.ReviewVO;
import com.velocitymall.review.service.ReviewService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 商品评价服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final int HAS_NO_PICTURES = 0;

    private static final int INITIAL_COUNT = 0;

    private static final int INTERACTION_TYPE_LIKE = 1;

    private static final int INTERACTION_TYPE_DISLIKE = 2;

    private static final long MAX_PAGE_SIZE = 100L;

    private static final int NO_INTERACTION = 0;

    private static final String REVIEW_STATS_CACHE_KEY_PREFIX = "velocitymall:review:stats:";

    private static final String REVIEW_STATS_LOCK_KEY_PREFIX = "velocitymall:review:lock:stats:";

    private static final Duration EMPTY_STATS_TTL = Duration.ofMinutes(1);

    private static final long STATS_CACHE_BASE_MINUTES = 30L;

    private static final long STATS_CACHE_JITTER_MINUTES = 10L;

    private final ProductReviewMapper productReviewMapper;

    private final ReviewInteractionMapper reviewInteractionMapper;

    private final ReviewReplyMapper reviewReplyMapper;

    private final OrderFeignClient orderFeignClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReview(ReviewCreateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        checkPurchaseEligibility(userId, dto.getOrderSn(), dto.getSkuId());
        ProductReview review = ProductReview.builder()
                .userId(userId)
                .orderSn(dto.getOrderSn())
                .skuId(dto.getSkuId())
                .spuId(dto.getSpuId())
                .rating(dto.getRating())
                .content(dto.getContent())
                .hasPictures(HAS_NO_PICTURES)
                .likeCount(INITIAL_COUNT)
                .dislikeCount(INITIAL_COUNT)
                .replyCount(INITIAL_COUNT)
                .build();

        try {
            productReviewMapper.insert(review);
            evictReviewStatsCache(dto.getSpuId());
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "该订单商品已评价");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void interactReview(Long reviewId, ReviewInteractionDTO dto) {
        Long userId = getCurrentUserId();
        Integer interactionType = dto.getInteractionType();
        validateInteractionType(interactionType);
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }

        ReviewInteraction existingInteraction = reviewInteractionMapper.selectActiveByReviewAndUser(reviewId, userId);
        if (existingInteraction == null) {
            createFirstInteraction(reviewId, userId, interactionType);
            return;
        }

        handleExistingInteraction(reviewId, userId, interactionType, existingInteraction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId) {
        Long userId = getCurrentUserId();
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }
        if (!Objects.equals(userId, review.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该评价");
        }
        int deletedRows = productReviewMapper.deleteById(reviewId);
        if (deletedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }
        evictReviewStatsCache(review.getSpuId());
    }

    @Override
    public PageVO<ReviewReplyVO> listReviewReplies(Long reviewId, Long page, Long size) {
        validatePage(page, size);
        ensureReviewExists(reviewId);
        Page<ReviewReply> replyPage = new Page<>(page, size);
        LambdaQueryWrapper<ReviewReply> wrapper = new LambdaQueryWrapper<ReviewReply>()
                .eq(ReviewReply::getReviewId, reviewId)
                .orderByAsc(ReviewReply::getCreateTime);
        Page<ReviewReply> resultPage = reviewReplyMapper.selectPage(replyPage, wrapper);
        List<ReviewReplyVO> records = resultPage.getRecords().stream()
                .map(this::toReviewReplyVO)
                .toList();
        return new PageVO<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                resultPage.getPages(),
                records
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReviewReply(Long reviewId, ReviewReplyCreateDTO dto) {
        Long userId = getCurrentUserId();
        ensureReviewExists(reviewId);
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .userId(userId)
                .content(dto.getContent().trim())
                .build();
        reviewReplyMapper.insert(reply);
        int affectedRows = productReviewMapper.increaseReplyCount(reviewId);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewReply(Long reviewId, Long replyId) {
        Long userId = getCurrentUserId();
        ReviewReply reply = reviewReplyMapper.selectById(replyId);
        if (reply == null || !Objects.equals(reviewId, reply.getReviewId())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "回复不存在");
        }
        if (!Objects.equals(userId, reply.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该回复");
        }
        int deletedRows = reviewReplyMapper.deleteById(replyId);
        if (deletedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "回复不存在");
        }
        productReviewMapper.decreaseReplyCount(reviewId);
    }

    @Override
    public PageVO<ReviewVO> listProductReviews(Long spuId, Long page, Long size) {
        validatePage(page, size);
        Page<ProductReview> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getSpuId, spuId)
                .orderByDesc(ProductReview::getCreateTime);
        Page<ProductReview> resultPage = productReviewMapper.selectPage(reviewPage, wrapper);

        List<ProductReview> reviews = resultPage.getRecords();
        Map<Long, Integer> interactionMap = loadCurrentInteractionMap(reviews);

        List<ReviewVO> records = reviews.stream()
                .map(review -> toReviewVO(review, interactionMap.getOrDefault(review.getId(), NO_INTERACTION)))
                .toList();
        return new PageVO<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                resultPage.getPages(),
                records
        );
    }

    @Override
    public ReviewStatsVO getReviewStats(Long spuId) {
        String cacheKey = reviewStatsCacheKey(spuId);
        ReviewStatsVO cachedStats = readReviewStatsCache(cacheKey);
        if (cachedStats != null) {
            return cachedStats;
        }

        String lockKey = REVIEW_STATS_LOCK_KEY_PREFIX + spuId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 3, TimeUnit.SECONDS);
            if (!locked) {
                ReviewStatsVO latestCachedStats = readReviewStatsCache(cacheKey);
                if (latestCachedStats != null) {
                    return latestCachedStats;
                }
                throw new BusinessException(ResultCode.BIZ_WARNING, "评价统计正在刷新，请稍后再试");
            }

            ReviewStatsVO doubleCheckStats = readReviewStatsCache(cacheKey);
            if (doubleCheckStats != null) {
                return doubleCheckStats;
            }

            ReviewStatsVO stats = buildReviewStats(spuId);
            writeReviewStatsCache(cacheKey, stats);
            return stats;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价统计查询被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void validatePage(Long page, Long size) {
        if (page == null || page <= 0 || size == null || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价分页参数非法");
        }
    }

    private void checkPurchaseEligibility(Long userId, String orderSn, Long skuId) {
        Result<Boolean> result = orderFeignClient.checkPurchase(userId, orderSn, skuId);
        if (result == null
                || !ResultCode.SUCCESS.getCode().equals(result.getCode())
                || !Boolean.TRUE.equals(result.getData())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "必须购买过该商品且完成支付后才能评价");
        }
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void validateInteractionType(Integer interactionType) {
        if (!Integer.valueOf(INTERACTION_TYPE_LIKE).equals(interactionType)
                && !Integer.valueOf(INTERACTION_TYPE_DISLIKE).equals(interactionType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "互动类型非法");
        }
    }

    private ProductReview ensureReviewExists(Long reviewId) {
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }
        return review;
    }

    private void createFirstInteraction(Long reviewId, Long userId, Integer interactionType) {
        ReviewInteraction interaction = ReviewInteraction.builder()
                .reviewId(reviewId)
                .userId(userId)
                .interactionType(interactionType)
                .build();
        try {
            reviewInteractionMapper.insert(interaction);
            increaseInteractionCount(reviewId, interactionType);
        } catch (DuplicateKeyException exception) {
            ReviewInteraction existingInteraction = reviewInteractionMapper.selectActiveByReviewAndUser(reviewId, userId);
            if (existingInteraction == null) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价互动状态异常");
            }
            handleExistingInteraction(reviewId, userId, interactionType, existingInteraction);
        }
    }

    private void handleExistingInteraction(
            Long reviewId,
            Long userId,
            Integer interactionType,
            ReviewInteraction existingInteraction
    ) {
        Integer oldInteractionType = existingInteraction.getInteractionType();
        if (interactionType.equals(oldInteractionType)) {
            int deletedRows = reviewInteractionMapper.physicalDelete(existingInteraction.getId(), reviewId, userId);
            if (deletedRows == 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价互动状态异常");
            }
            decreaseInteractionCount(reviewId, interactionType);
            return;
        }

        switchInteractionCount(reviewId, oldInteractionType, interactionType);
        int updatedRows = reviewInteractionMapper.updateInteractionType(existingInteraction.getId(), interactionType);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价互动状态异常");
        }
    }

    private void increaseInteractionCount(Long reviewId, Integer interactionType) {
        int affectedRows = Integer.valueOf(INTERACTION_TYPE_LIKE).equals(interactionType)
                ? productReviewMapper.increaseLikeCount(reviewId)
                : productReviewMapper.increaseDislikeCount(reviewId);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在");
        }
    }

    private void decreaseInteractionCount(Long reviewId, Integer interactionType) {
        int affectedRows = Integer.valueOf(INTERACTION_TYPE_LIKE).equals(interactionType)
                ? productReviewMapper.decreaseLikeCount(reviewId)
                : productReviewMapper.decreaseDislikeCount(reviewId);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价互动计数异常");
        }
    }

    private void switchInteractionCount(Long reviewId, Integer oldInteractionType, Integer newInteractionType) {
        int affectedRows;
        if (Integer.valueOf(INTERACTION_TYPE_LIKE).equals(oldInteractionType)
                && Integer.valueOf(INTERACTION_TYPE_DISLIKE).equals(newInteractionType)) {
            affectedRows = productReviewMapper.switchLikeToDislike(reviewId);
        } else if (Integer.valueOf(INTERACTION_TYPE_DISLIKE).equals(oldInteractionType)
                && Integer.valueOf(INTERACTION_TYPE_LIKE).equals(newInteractionType)) {
            affectedRows = productReviewMapper.switchDislikeToLike(reviewId);
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "互动类型非法");
        }
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价互动计数异常");
        }
    }

    private Map<Long, Integer> loadCurrentInteractionMap(List<ProductReview> reviews) {
        Long userId = UserContext.getUserId();
        if (userId == null || CollectionUtils.isEmpty(reviews)) {
            return Map.of();
        }

        List<Long> reviewIds = reviews.stream()
                .map(ProductReview::getId)
                .filter(Objects::nonNull)
                .toList();
        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewInteractionMapper.selectActiveByUserAndReviewIds(userId, reviewIds)
                .stream()
                .collect(Collectors.toMap(
                        ReviewInteraction::getReviewId,
                        ReviewInteraction::getInteractionType,
                        (left, right) -> left
                ));
    }

    private ReviewStatsVO readReviewStatsCache(String cacheKey) {
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ReviewStatsVO.class);
        } catch (JsonProcessingException exception) {
            log.warn("Review stats cache deserialize failed, cacheKey: {}", cacheKey, exception);
            stringRedisTemplate.delete(cacheKey);
            return null;
        }
    }

    private void writeReviewStatsCache(String cacheKey, ReviewStatsVO stats) {
        try {
            Duration ttl = stats.getTotalCount() == null || stats.getTotalCount() == 0
                    ? EMPTY_STATS_TTL
                    : normalStatsTtl();
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), ttl);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "评价统计序列化失败");
        }
    }

    private ReviewStatsVO buildReviewStats(Long spuId) {
        Long totalCount = productReviewMapper.countBySpuId(spuId);
        Long goodCount = productReviewMapper.countGoodBySpuId(spuId);
        int total = toIntegerCount(totalCount);
        int good = toIntegerCount(goodCount);
        BigDecimal goodRate = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(good)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return ReviewStatsVO.builder()
                .spuId(spuId)
                .totalCount(total)
                .goodCount(good)
                .goodRate(goodRate)
                .build();
    }

    private Duration normalStatsTtl() {
        long jitterMinutes = ThreadLocalRandom.current().nextLong(STATS_CACHE_JITTER_MINUTES + 1);
        return Duration.ofMinutes(STATS_CACHE_BASE_MINUTES + jitterMinutes);
    }

    private int toIntegerCount(Long count) {
        if (count == null || count <= 0) {
            return 0;
        }
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count.intValue();
    }

    private void evictReviewStatsCache(Long spuId) {
        if (spuId != null) {
            stringRedisTemplate.delete(reviewStatsCacheKey(spuId));
        }
    }

    private String reviewStatsCacheKey(Long spuId) {
        return REVIEW_STATS_CACHE_KEY_PREFIX + spuId;
    }

    private ReviewVO toReviewVO(ProductReview review, Integer currentInteractionType) {
        Long currentUserId = UserContext.getUserId();
        return ReviewVO.builder()
                .id(review.getId())
                .skuId(review.getSkuId())
                .spuId(review.getSpuId())
                .rating(review.getRating())
                .content(review.getContent())
                .hasPictures(review.getHasPictures())
                .likeCount(review.getLikeCount())
                .dislikeCount(review.getDislikeCount())
                .replyCount(review.getReplyCount())
                .currentInteractionType(currentInteractionType == null ? NO_INTERACTION : currentInteractionType)
                .mine(Objects.equals(currentUserId, review.getUserId()))
                .createTime(review.getCreateTime())
                .build();
    }

    private ReviewReplyVO toReviewReplyVO(ReviewReply reply) {
        Long currentUserId = UserContext.getUserId();
        return ReviewReplyVO.builder()
                .id(reply.getId())
                .reviewId(reply.getReviewId())
                .userId(reply.getUserId())
                .content(reply.getContent())
                .mine(Objects.equals(currentUserId, reply.getUserId()))
                .createTime(reply.getCreateTime())
                .build();
    }
}
