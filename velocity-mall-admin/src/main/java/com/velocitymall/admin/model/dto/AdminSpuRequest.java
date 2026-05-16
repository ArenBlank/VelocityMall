package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminSpuRequest {

    @NotNull(message = "categoryId不能为空")
    @Min(value = 1, message = "categoryId必须大于0")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    private Integer publishStatus;
}
