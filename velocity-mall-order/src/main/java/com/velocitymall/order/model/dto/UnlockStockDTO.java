package com.velocitymall.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 释放库存请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnlockStockDTO {

    private String orderSn;

    private Long skuId;

    private Integer quantity;
}
