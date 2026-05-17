package com.velocitymall.admin.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminRoleVO {

    private String id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private List<AdminPermissionVO> permissions;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
