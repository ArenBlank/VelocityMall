package com.velocitymall.search.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search SKU response object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSkuVO {

    private Long skuId;

    private String skuName;

    private String skuPic;

    private BigDecimal price;

    private Integer saleCount;
}
