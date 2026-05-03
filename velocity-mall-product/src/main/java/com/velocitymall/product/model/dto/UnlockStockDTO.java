package com.velocitymall.product.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 释放锁定库存请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnlockStockDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderSn;

    @NotNull(message = "SKU ID不能为空")
    @Min(value = 1, message = "SKU ID必须大于0")
    private Long skuId;

    @NotNull(message = "释放数量不能为空")
    @Min(value = 1, message = "释放数量必须大于0")
    private Integer quantity;
}
