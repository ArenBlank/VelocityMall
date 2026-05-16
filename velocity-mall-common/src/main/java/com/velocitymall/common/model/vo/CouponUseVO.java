package com.velocitymall.common.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coupon usage result for order settlement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponUseVO {

    private Long couponHistoryId;

    private Long couponId;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;
}
