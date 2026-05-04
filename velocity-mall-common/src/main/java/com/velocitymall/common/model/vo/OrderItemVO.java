package com.velocitymall.common.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order item view object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVO {

    private Long skuId;

    private String skuName;

    private String skuPic;

    private BigDecimal skuPrice;

    private Integer quantity;
}
