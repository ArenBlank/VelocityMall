package com.velocitymall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
