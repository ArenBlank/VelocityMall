package com.velocitymall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.order.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Payment and refund transaction mapper.
 */
@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {

    @Update("""
            UPDATE oms_payment_transaction
            SET status = 0,
                pay_type = #{payType},
                amount = #{amount},
                trade_no = NULL,
                callback_payload = NULL,
                fail_reason = NULL,
                success_time = NULL,
                version = version + 1
            WHERE id = #{id}
              AND status <> 1
              AND is_deleted = 0
            """)
    int resetForRetry(@Param("id") Long id,
                      @Param("payType") Integer payType,
                      @Param("amount") java.math.BigDecimal amount);

    @Update("""
            UPDATE oms_payment_transaction
            SET status = 1,
                trade_no = #{tradeNo},
                callback_payload = #{callbackPayload},
                fail_reason = NULL,
                success_time = NOW(),
                version = version + 1
            WHERE id = #{id}
              AND status <> 1
              AND is_deleted = 0
            """)
    int markSuccess(@Param("id") Long id,
                    @Param("tradeNo") String tradeNo,
                    @Param("callbackPayload") String callbackPayload);

    @Update("""
            UPDATE oms_payment_transaction
            SET status = 2,
                trade_no = #{tradeNo},
                callback_payload = #{callbackPayload},
                fail_reason = #{failReason},
                version = version + 1
            WHERE id = #{id}
              AND status <> 1
              AND is_deleted = 0
            """)
    int markFailed(@Param("id") Long id,
                   @Param("tradeNo") String tradeNo,
                   @Param("callbackPayload") String callbackPayload,
                   @Param("failReason") String failReason);
}
