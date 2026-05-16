package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminStatusRequest {

    @NotNull(message = "status不能为空")
    private Integer status;
}
