package com.velocitymall.coupon.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Buyer-facing coupon definition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponVO {

    private Long couponId;

    private String name;

    private BigDecimal amount;

    private BigDecimal minPoint;

    private Integer stock;

    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
