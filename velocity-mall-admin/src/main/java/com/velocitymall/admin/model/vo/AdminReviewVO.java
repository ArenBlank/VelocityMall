package com.velocitymall.admin.model.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReviewVO {

    private Long id;

    private Long userId;

    private String orderSn;

    private Long skuId;

    private Long spuId;

    private Integer rating;

    private String content;

    private Integer likeCount;

    private Integer dislikeCount;

    private LocalDateTime createTime;
}
