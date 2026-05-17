package com.velocitymall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.admin.entity.AdminRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    @Select("""
            SELECT DISTINCT r.role_code
            FROM ums_admin_role_relation ar
            INNER JOIN ums_admin_role r ON ar.role_id = r.id
            WHERE ar.admin_id = #{adminId}
              AND ar.is_deleted = 0
              AND r.is_deleted = 0
              AND r.status = 1
            ORDER BY r.role_code
            """)
    List<String> selectRoleCodesByAdminId(@Param("adminId") Long adminId);

    @Select("""
            SELECT r.*
            FROM ums_admin_role_relation ar
            INNER JOIN ums_admin_role r ON ar.role_id = r.id
            WHERE ar.admin_id = #{adminId}
              AND ar.is_deleted = 0
              AND r.is_deleted = 0
            ORDER BY r.role_code
            """)
    List<AdminRole> selectRolesByAdminId(@Param("adminId") Long adminId);
}
