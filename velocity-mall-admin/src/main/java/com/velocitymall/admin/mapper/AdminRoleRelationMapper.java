package com.velocitymall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.admin.entity.AdminRoleRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminRoleRelationMapper extends BaseMapper<AdminRoleRelation> {

    @Delete("DELETE FROM ums_admin_role_relation WHERE admin_id = #{adminId}")
    int deleteByAdminId(@Param("adminId") Long adminId);
}
