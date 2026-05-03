package com.velocitymall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
}
