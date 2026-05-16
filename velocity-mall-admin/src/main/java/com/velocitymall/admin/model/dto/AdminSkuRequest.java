package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminSkuRequest {

    @NotNull(message = "spuId不能为空")
    @Min(value = 1, message = "spuId必须大于0")
    private Long spuId;

    @NotBlank(message = "SKU名称不能为空")
    private String skuName;

    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    private String coverImg;
}
