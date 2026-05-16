package com.velocitymall.seckill.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Buyer-facing flash-sale activity data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillActivityVO {

    private Long activityId;

    private Long skuId;

    private Long spuId;

    private String activityName;

    private BigDecimal seckillPrice;

    private BigDecimal originalPrice;

    private Integer seckillStock;

    private Integer remainingStock;

    private String startTime;

    private String endTime;

    private Integer status;

    private String state;
}
