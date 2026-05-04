package com.velocitymall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.coupon.entity.CouponHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取流水 Mapper。
 */
@Mapper
public interface CouponHistoryMapper extends BaseMapper<CouponHistory> {
}
