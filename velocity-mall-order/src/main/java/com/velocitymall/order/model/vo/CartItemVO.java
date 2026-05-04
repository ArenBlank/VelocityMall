package com.velocitymall.order.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cart item view object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO {

    private Long skuId;

    private String skuName;

    private BigDecimal price;

    private Integer quantity;

    private Integer availableStock;

    private BigDecimal totalAmount;
}
