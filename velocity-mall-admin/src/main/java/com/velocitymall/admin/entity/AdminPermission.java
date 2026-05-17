package com.velocitymall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("ums_admin_permission")
public class AdminPermission extends BaseEntity {

    @TableField("permission_code")
    private String permissionCode;

    @TableField("permission_name")
    private String permissionName;

    @TableField("resource")
    private String resource;

    @TableField("action")
    private String action;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;
}
