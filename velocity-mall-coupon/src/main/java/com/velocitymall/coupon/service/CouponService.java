package com.velocitymall.coupon.service;

import com.velocitymall.common.model.dto.CouponUseDTO;
import com.velocitymall.common.model.vo.CouponUseVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.coupon.model.vo.CouponVO;
import com.velocitymall.coupon.model.vo.UserCouponVO;

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

    PageVO<CouponVO> listAvailableCoupons(Long page, Long size);

    PageVO<UserCouponVO> listMyCoupons(Long page, Long size, Integer useStatus);

    CouponUseVO useCoupon(CouponUseDTO dto);

    void releaseUsedCoupon(String orderSn);
}
