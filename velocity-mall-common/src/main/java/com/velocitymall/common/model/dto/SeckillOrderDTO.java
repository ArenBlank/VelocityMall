package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Seckill order message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SeckillOrderDTO extends BaseMessageDTO {

    private Long userId;

    private Long skuId;

    private Integer quantity;

    private String orderSn;
}
