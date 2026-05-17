package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 64)
    private String realName;

    private Integer status;

    @NotEmpty
    private List<String> roleIds;
}
