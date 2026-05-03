package com.velocitymall.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单延时关单消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessageDTO {

    private String orderSn;

    private Long skuId;

    private Integer quantity;
}
