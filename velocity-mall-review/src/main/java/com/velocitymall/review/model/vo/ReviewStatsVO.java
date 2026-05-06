package com.velocitymall.review.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品评价统计展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsVO {

    private Long spuId;

    private Integer totalCount;

    private Integer goodCount;

    private BigDecimal goodRate;
}
