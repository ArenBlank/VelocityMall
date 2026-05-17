package com.velocitymall.admin.service;

import com.velocitymall.admin.model.dto.AdminPasswordResetRequest;
import com.velocitymall.admin.model.dto.AdminRoleRequest;
import com.velocitymall.admin.model.dto.AdminStatusRequest;
import com.velocitymall.admin.model.dto.AdminUserCreateRequest;
import com.velocitymall.admin.model.dto.AdminUserUpdateRequest;
import com.velocitymall.admin.model.vo.AdminPermissionVO;
import com.velocitymall.admin.model.vo.AdminRoleVO;
import com.velocitymall.admin.model.vo.AdminUserVO;
import com.velocitymall.common.model.vo.PageVO;
import java.util.List;

public interface AdminRbacService {

    PageVO<AdminUserVO> listAdmins(Long page, Long size, String keyword, Integer status);

    AdminUserVO createAdmin(AdminUserCreateRequest request);

    AdminUserVO updateAdmin(Long adminId, AdminUserUpdateRequest request);

    AdminUserVO updateAdminStatus(Long adminId, AdminStatusRequest request);

    void resetAdminPassword(Long adminId, AdminPasswordResetRequest request);

    List<AdminRoleVO> listRoles();

    AdminRoleVO createRole(AdminRoleRequest request);

    AdminRoleVO updateRole(Long roleId, AdminRoleRequest request);

    AdminRoleVO updateRoleStatus(Long roleId, AdminStatusRequest request);

    List<AdminPermissionVO> listPermissions();
}
