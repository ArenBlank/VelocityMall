package com.velocitymall.review.model.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品评价展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {

    private Long id;

    private Long skuId;

    private Long spuId;

    private Integer rating;

    private String content;

    private Integer hasPictures;

    private Integer likeCount;

    private Integer dislikeCount;

    private Integer replyCount;

    private Integer currentInteractionType;

    private Boolean mine;

    private LocalDateTime createTime;
}
