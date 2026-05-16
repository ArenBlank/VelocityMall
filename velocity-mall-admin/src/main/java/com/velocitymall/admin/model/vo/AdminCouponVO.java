package com.velocitymall.admin.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCouponVO {

    private Long id;

    private String name;

    private BigDecimal amount;

    private BigDecimal minPoint;

    private Integer stock;

    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime createTime;
}
