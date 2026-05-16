package com.velocitymall.common.model.dto;

import java.math.BigDecimal;
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

    private BigDecimal seckillPrice;

    public SeckillOrderDTO(Long userId, Long skuId, Integer quantity, String orderSn) {
        this(userId, skuId, quantity, orderSn, null);
    }
}
