package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order item payload used by cross-service messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private Long skuId;

    private Integer quantity;
}
