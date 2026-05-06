package com.velocitymall.review.service;

import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.review.model.dto.ReviewCreateDTO;
import com.velocitymall.review.model.dto.ReviewInteractionDTO;
import com.velocitymall.review.model.vo.ReviewStatsVO;
import com.velocitymall.review.model.vo.ReviewVO;

/**
 * 商品评价服务。
 */
public interface ReviewService {

    /**
     * 发布商品评价。
     *
     * @param dto 评价请求
     */
    void createReview(ReviewCreateDTO dto);

    /**
     * 分页查询商品评价。
     *
     * @param spuId SPU ID
     * @param page 页码
     * @param size 每页数量
     * @return 评价分页
     */
    PageVO<ReviewVO> listProductReviews(Long spuId, Long page, Long size);

    /**
     * Get cached product review stats.
     *
     * @param spuId SPU ID
     * @return review stats
     */
    ReviewStatsVO getReviewStats(Long spuId);

    /**
     * Like, dislike, cancel, or switch a review interaction.
     *
     * @param reviewId review ID
     * @param dto interaction request
     */
    void interactReview(Long reviewId, ReviewInteractionDTO dto);

    /**
     * Delete current user's review.
     *
     * @param reviewId review ID
     */
    void deleteReview(Long reviewId);
}
