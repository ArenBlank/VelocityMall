package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @Size(max = 64)
    private String realName;

    private Integer status;

    @NotEmpty
    private List<String> roleIds;
}
