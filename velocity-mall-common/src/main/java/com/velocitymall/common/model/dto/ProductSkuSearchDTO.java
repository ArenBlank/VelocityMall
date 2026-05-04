package com.velocitymall.common.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU source data used to build the search index.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuSearchDTO {

    private Long skuId;

    private String skuName;

    private String skuPic;

    private BigDecimal price;

    private Integer saleCount;

    /**
     * Product publish status: 0-off shelf, 1-on shelf.
     */
    private Integer status;
}
