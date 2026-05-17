package com.velocitymall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.admin.entity.AdminRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminRolePermissionMapper extends BaseMapper<AdminRolePermission> {

    @Delete("DELETE FROM ums_admin_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
