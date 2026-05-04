package com.velocitymall.coupon.service;

/**
 * 优惠券业务接口。
 */
public interface CouponService {

    /**
     * 领取优惠券。
     *
     * @param couponId 优惠券 ID
     */
    void claimCoupon(Long couponId);
}
