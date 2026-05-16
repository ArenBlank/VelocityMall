package com.velocitymall.admin.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSeckillActivityVO {

    private Long id;

    private Long skuId;

    private Long spuId;

    private String activityName;

    private BigDecimal seckillPrice;

    private BigDecimal originalPrice;

    private Integer seckillStock;

    private Integer remainingStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String state;

    private LocalDateTime createTime;
}
