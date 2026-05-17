package com.velocitymall.admin.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserVO {

    private String adminId;

    private String username;

    private String realName;

    private Integer status;

    private List<AdminRoleVO> roles;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
