package com.velocitymall.coupon.controller;

import com.velocitymall.common.model.dto.CouponUseDTO;
import com.velocitymall.common.model.vo.CouponUseVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.coupon.model.vo.CouponVO;
import com.velocitymall.coupon.model.vo.UserCouponVO;
import com.velocitymall.coupon.service.CouponService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/available")
    public Result<PageVO<CouponVO>> listAvailableCoupons(
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "size", defaultValue = "10") Long size) {
        return Result.success(couponService.listAvailableCoupons(page, size));
    }

    @GetMapping("/my")
    public Result<PageVO<UserCouponVO>> listMyCoupons(
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "useStatus", required = false) Integer useStatus) {
        return Result.success(couponService.listMyCoupons(page, size, useStatus));
    }

    @PostMapping("/inner/use")
    public Result<CouponUseVO> useCoupon(@RequestBody CouponUseDTO dto) {
        return Result.success(couponService.useCoupon(dto));
    }

    @PostMapping("/inner/release")
    public Result<Void> releaseCoupon(@RequestParam("orderSn") String orderSn) {
        couponService.releaseUsedCoupon(orderSn);
        return Result.success();
    }
}
