package com.velocitymall.product.model.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU basic information update request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSkuDTO {

    private String skuName;

    @DecimalMin(value = "0.00", message = "SKU价格不能小于0")
    private BigDecimal price;

    private String coverImg;
}
