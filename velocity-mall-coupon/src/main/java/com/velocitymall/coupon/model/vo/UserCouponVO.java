package com.velocitymall.coupon.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coupon claimed by the current buyer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponVO {

    private Long historyId;

    private Long couponId;

    private String name;

    private BigDecimal amount;

    private BigDecimal minPoint;

    private Integer useStatus;

    private LocalDateTime claimTime;

    private LocalDateTime useTime;

    private String orderSn;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer couponStatus;

    private Boolean available;
}
