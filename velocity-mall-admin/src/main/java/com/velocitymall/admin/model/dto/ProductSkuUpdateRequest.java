package com.velocitymall.admin.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuUpdateRequest {

    private String skuName;

    private BigDecimal price;

    private String coverImg;
}
