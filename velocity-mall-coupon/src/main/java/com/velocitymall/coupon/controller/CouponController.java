package com.velocitymall.coupon.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.coupon.service.CouponService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端优惠券接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/{coupon-id}/claim")
    public Result<Void> claimCoupon(@PathVariable("coupon-id") @Min(1) Long couponId) {
        couponService.claimCoupon(couponId);
        return Result.success();
    }
}
