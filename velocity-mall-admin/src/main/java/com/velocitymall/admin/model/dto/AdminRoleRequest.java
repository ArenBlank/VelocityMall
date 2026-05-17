package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class AdminRoleRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,64}$")
    private String roleCode;

    @NotBlank
    @Size(max = 64)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Integer status;

    @NotEmpty
    private List<String> permissionIds;
}
