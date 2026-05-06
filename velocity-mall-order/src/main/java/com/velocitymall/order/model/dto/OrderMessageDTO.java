package com.velocitymall.order.model.dto;

import com.velocitymall.common.model.dto.BaseMessageDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 订单延时关单消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderMessageDTO extends BaseMessageDTO {

    private String orderSn;

    private Long skuId;

    private Integer quantity;
}
