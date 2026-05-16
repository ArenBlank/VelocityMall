package com.velocitymall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.coupon.entity.CouponHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券领取流水 Mapper。
 */
@Mapper
public interface CouponHistoryMapper extends BaseMapper<CouponHistory> {

    @Update("""
            UPDATE sms_coupon_history
            SET use_status = 1,
                use_time = NOW(),
                order_sn = #{orderSn}
            WHERE id = #{historyId}
              AND user_id = #{userId}
              AND use_status = 0
              AND is_deleted = 0
            """)
    int markUsed(@Param("historyId") Long historyId,
                 @Param("userId") Long userId,
                 @Param("orderSn") String orderSn);

    @Update("""
            UPDATE sms_coupon_history
            SET use_status = 0,
                use_time = NULL,
                order_sn = NULL
            WHERE order_sn = #{orderSn}
              AND use_status = 1
              AND is_deleted = 0
            """)
    int releaseByOrderSn(@Param("orderSn") String orderSn);
}
