package com.velocitymall.admin.controller;

import com.velocitymall.admin.annotation.RequireAdminPermission;
import com.velocitymall.admin.constant.AdminPermissionCodes;
import com.velocitymall.admin.model.dto.AdminPasswordResetRequest;
import com.velocitymall.admin.model.dto.AdminRoleRequest;
import com.velocitymall.admin.model.dto.AdminStatusRequest;
import com.velocitymall.admin.model.dto.AdminUserCreateRequest;
import com.velocitymall.admin.model.dto.AdminUserUpdateRequest;
import com.velocitymall.admin.model.vo.AdminPermissionVO;
import com.velocitymall.admin.model.vo.AdminRoleVO;
import com.velocitymall.admin.model.vo.AdminUserVO;
import com.velocitymall.admin.service.AdminRbacService;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/rbac")
public class AdminRbacController {

    private final AdminRbacService adminRbacService;

    @GetMapping("/admins")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_READ)
    public Result<PageVO<AdminUserVO>> listAdmins(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(adminRbacService.listAdmins(page, size, keyword, status));
    }

    @PostMapping("/admins")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminUserVO> createAdmin(@Valid @RequestBody AdminUserCreateRequest request) {
        return Result.success(adminRbacService.createAdmin(request));
    }

    @PutMapping("/admins/{admin-id}")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminUserVO> updateAdmin(
            @PathVariable("admin-id") @Min(1) Long adminId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return Result.success(adminRbacService.updateAdmin(adminId, request));
    }

    @PutMapping("/admins/{admin-id}/status")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminUserVO> updateAdminStatus(
            @PathVariable("admin-id") @Min(1) Long adminId,
            @Valid @RequestBody AdminStatusRequest request
    ) {
        return Result.success(adminRbacService.updateAdminStatus(adminId, request));
    }

    @PutMapping("/admins/{admin-id}/password")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<Void> resetAdminPassword(
            @PathVariable("admin-id") @Min(1) Long adminId,
            @Valid @RequestBody AdminPasswordResetRequest request
    ) {
        adminRbacService.resetAdminPassword(adminId, request);
        return Result.success();
    }

    @GetMapping("/roles")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_READ)
    public Result<List<AdminRoleVO>> listRoles() {
        return Result.success(adminRbacService.listRoles());
    }

    @PostMapping("/roles")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminRoleVO> createRole(@Valid @RequestBody AdminRoleRequest request) {
        return Result.success(adminRbacService.createRole(request));
    }

    @PutMapping("/roles/{role-id}")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminRoleVO> updateRole(
            @PathVariable("role-id") @Min(1) Long roleId,
            @Valid @RequestBody AdminRoleRequest request
    ) {
        return Result.success(adminRbacService.updateRole(roleId, request));
    }

    @PutMapping("/roles/{role-id}/status")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_WRITE)
    public Result<AdminRoleVO> updateRoleStatus(
            @PathVariable("role-id") @Min(1) Long roleId,
            @Valid @RequestBody AdminStatusRequest request
    ) {
        return Result.success(adminRbacService.updateRoleStatus(roleId, request));
    }

    @GetMapping("/permissions")
    @RequireAdminPermission(AdminPermissionCodes.RBAC_READ)
    public Result<List<AdminPermissionVO>> listPermissions() {
        return Result.success(adminRbacService.listPermissions());
    }
}
