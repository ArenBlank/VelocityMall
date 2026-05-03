package com.velocitymall.order.service;

import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.vo.OrderVO;

/**
 * 订单服务。
 */
public interface OrderService {

    /**
     * 提交订单。
     *
     * @param dto 提交订单参数
     * @return 订单信息
     */
    OrderVO submitOrder(SubmitOrderDTO dto);
}
