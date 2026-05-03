package com.velocitymall.product.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuVO {

    private Long skuId;

    private String skuName;

    private String skuCode;

    private BigDecimal price;

    private Integer availableStock;

    private String coverImg;
}
