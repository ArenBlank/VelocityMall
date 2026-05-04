package com.velocitymall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券 Mapper。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 基于库存与乐观锁版本扣减优惠券库存。
     *
     * @param couponId 优惠券 ID
     * @param oldVersion 当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE sms_coupon
            SET stock = stock - 1,
                version = version + 1
            WHERE id = #{couponId}
              AND stock > 0
              AND version = #{oldVersion}
              AND status = 1
              AND is_deleted = 0
            """)
    int decreaseStock(@Param("couponId") Long couponId, @Param("oldVersion") Integer oldVersion);
}
