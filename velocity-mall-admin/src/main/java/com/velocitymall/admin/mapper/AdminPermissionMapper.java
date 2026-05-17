package com.velocitymall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.admin.entity.AdminPermission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermission> {

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM ums_admin_role_relation ar
            INNER JOIN ums_admin_role r ON ar.role_id = r.id
            INNER JOIN ums_admin_role_permission rp ON rp.role_id = r.id
            INNER JOIN ums_admin_permission p ON rp.permission_id = p.id
            WHERE ar.admin_id = #{adminId}
              AND ar.is_deleted = 0
              AND r.is_deleted = 0
              AND r.status = 1
              AND rp.is_deleted = 0
              AND p.is_deleted = 0
              AND p.status = 1
            ORDER BY p.permission_code
            """)
    List<String> selectPermissionCodesByAdminId(@Param("adminId") Long adminId);

    @Select("""
            SELECT p.*
            FROM ums_admin_role_permission rp
            INNER JOIN ums_admin_permission p ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND rp.is_deleted = 0
              AND p.is_deleted = 0
            ORDER BY p.resource, p.permission_code
            """)
    List<AdminPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}
