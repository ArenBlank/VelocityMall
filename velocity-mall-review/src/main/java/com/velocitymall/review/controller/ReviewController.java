package com.velocitymall.review.controller;

import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.review.model.dto.ReviewCreateDTO;
import com.velocitymall.review.model.dto.ReviewInteractionDTO;
import com.velocitymall.review.model.dto.ReviewReplyCreateDTO;
import com.velocitymall.review.model.vo.ReviewReplyVO;
import com.velocitymall.review.model.vo.ReviewStatsVO;
import com.velocitymall.review.model.vo.ReviewVO;
import com.velocitymall.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端商品评价接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<Void> createReview(@Valid @RequestBody ReviewCreateDTO dto) {
        reviewService.createReview(dto);
        return Result.success();
    }

    @GetMapping("/products/{spu-id}")
    public Result<PageVO<ReviewVO>> listProductReviews(
            @PathVariable("spu-id") @Min(1) Long spuId,
            @RequestParam(value = "page", defaultValue = "1")
            @Min(value = 1, message = "page必须大于0") Long page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "size必须大于0")
            @Max(value = 100, message = "size不能大于100") Long size
    ) {
        return Result.success(reviewService.listProductReviews(spuId, page, size));
    }

    @GetMapping("/products/{spu-id}/stats")
    public Result<ReviewStatsVO> getReviewStats(@PathVariable("spu-id") @Min(1) Long spuId) {
        return Result.success(reviewService.getReviewStats(spuId));
    }

    @PostMapping("/{review-id}/interaction")
    public Result<Void> interactReview(
            @PathVariable("review-id") @Min(1) Long reviewId,
            @Valid @RequestBody ReviewInteractionDTO dto
    ) {
        reviewService.interactReview(reviewId, dto);
        return Result.success();
    }

    @GetMapping("/{review-id}/replies")
    public Result<PageVO<ReviewReplyVO>> listReviewReplies(
            @PathVariable("review-id") @Min(1) Long reviewId,
            @RequestParam(value = "page", defaultValue = "1")
            @Min(value = 1, message = "page必须大于0") Long page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "size必须大于0")
            @Max(value = 100, message = "size不能大于100") Long size
    ) {
        return Result.success(reviewService.listReviewReplies(reviewId, page, size));
    }

    @PostMapping("/{review-id}/replies")
    public Result<Void> createReviewReply(
            @PathVariable("review-id") @Min(1) Long reviewId,
            @Valid @RequestBody ReviewReplyCreateDTO dto
    ) {
        reviewService.createReviewReply(reviewId, dto);
        return Result.success();
    }

    @DeleteMapping("/{review-id}/replies/{reply-id}")
    public Result<Void> deleteReviewReply(
            @PathVariable("review-id") @Min(1) Long reviewId,
            @PathVariable("reply-id") @Min(1) Long replyId
    ) {
        reviewService.deleteReviewReply(reviewId, replyId);
        return Result.success();
    }

    @DeleteMapping("/{review-id}")
    public Result<Void> deleteReview(@PathVariable("review-id") @Min(1) Long reviewId) {
        reviewService.deleteReview(reviewId);
        return Result.success();
    }
}
