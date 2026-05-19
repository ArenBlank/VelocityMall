package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillTestRequest {

    @NotNull(message = "skuId不能为空")
    @Min(1)
    private Long skuId;

    @Min(1)
    private Integer stock = 1000;
}
