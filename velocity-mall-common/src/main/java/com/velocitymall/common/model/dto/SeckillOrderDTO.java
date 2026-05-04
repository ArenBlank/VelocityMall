package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seckill order message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderDTO {

    private Long userId;

    private Long skuId;

    private Integer quantity;

    private String orderSn;
}
