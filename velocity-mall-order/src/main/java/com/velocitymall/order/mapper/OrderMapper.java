package com.velocitymall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订单主表 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 条件关闭待付款订单。
     *
     * @param orderSn 订单号
     * @return 影响行数
     */
    @Update("UPDATE oms_order SET status = 4, version = version + 1 WHERE order_sn = #{orderSn} AND status = 0 AND is_deleted = 0")
    int closeOrder(@Param("orderSn") String orderSn);

    /**
     * Cancel a waiting-pay order owned by the current user.
     *
     * @param orderSn order number
     * @param userId user ID
     * @return affected rows
     */
    @Update("""
            UPDATE oms_order
            SET status = 4,
                version = version + 1
            WHERE order_sn = #{orderSn}
              AND user_id = #{userId}
              AND status = 0
              AND is_deleted = 0
            """)
    int cancelOrder(@Param("orderSn") String orderSn, @Param("userId") Long userId);

    /**
     * Mark a paid order as refunded.
     *
     * @param orderSn order number
     * @param userId user ID
     * @return affected rows
     */
    @Update("""
            UPDATE oms_order
            SET status = 5,
                version = version + 1
            WHERE order_sn = #{orderSn}
              AND user_id = #{userId}
              AND status = 1
              AND is_deleted = 0
            """)
    int markRefunded(@Param("orderSn") String orderSn, @Param("userId") Long userId);

    /**
     * Mark a waiting-pay order as paid.
     *
     * @param orderSn order number
     * @param payType payment type
     * @return affected rows
     */
    @Update("""
            UPDATE oms_order
            SET status = 1,
                pay_type = #{payType},
                pay_time = NOW(),
                version = version + 1
            WHERE order_sn = #{orderSn}
              AND status = 0
              AND is_deleted = 0
            """)
    int markPaid(@Param("orderSn") String orderSn, @Param("payType") Integer payType);

    @Update("""
            UPDATE oms_order
            SET status = 2,
                delivery_company = #{deliveryCompany},
                delivery_sn = #{deliverySn},
                delivery_time = NOW(),
                version = version + 1
            WHERE order_sn = #{orderSn}
              AND status = 1
              AND is_deleted = 0
            """)
    int markDelivered(@Param("orderSn") String orderSn,
                      @Param("deliveryCompany") String deliveryCompany,
                      @Param("deliverySn") String deliverySn);

    @Update("""
            UPDATE oms_order
            SET status = 3,
                receive_time = NOW(),
                version = version + 1
            WHERE order_sn = #{orderSn}
              AND user_id = #{userId}
              AND status = 2
              AND is_deleted = 0
            """)
    int markReceived(@Param("orderSn") String orderSn, @Param("userId") Long userId);

    /**
     * Count paid orders containing the specified SKU for a user.
     *
     * @param userId user ID
     * @param orderSn order number
     * @param skuId SKU ID
     * @return matching order count
     */
    @Select("""
            SELECT COUNT(1)
            FROM oms_order o
            INNER JOIN oms_order_item oi ON o.order_sn = oi.order_sn
            WHERE o.user_id = #{userId}
              AND o.order_sn = #{orderSn}
              AND oi.sku_id = #{skuId}
              AND o.status = 3
              AND o.is_deleted = 0
              AND oi.is_deleted = 0
            """)
    Long countCompletedSkuOrders(
            @Param("userId") Long userId,
            @Param("orderSn") String orderSn,
            @Param("skuId") Long skuId
    );
}
