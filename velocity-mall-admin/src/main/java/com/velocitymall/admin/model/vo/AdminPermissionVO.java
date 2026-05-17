package com.velocitymall.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminPermissionVO {

    private String id;

    private String permissionCode;

    private String permissionName;

    private String resource;

    private String action;

    private String description;

    private Integer status;
}
