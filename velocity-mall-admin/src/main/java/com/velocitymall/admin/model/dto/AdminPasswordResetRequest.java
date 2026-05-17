package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPasswordResetRequest {

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
}
