package com.velocitymall.common.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal coupon usage request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponUseDTO {

    private Long userId;

    private Long couponHistoryId;

    private String orderSn;

    private BigDecimal orderAmount;
}
