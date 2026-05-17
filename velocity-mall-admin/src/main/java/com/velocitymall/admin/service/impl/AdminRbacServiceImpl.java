package com.velocitymall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.velocitymall.admin.constant.AdminRoleCodes;
import com.velocitymall.admin.context.AdminContext;
import com.velocitymall.admin.entity.Admin;
import com.velocitymall.admin.entity.AdminPermission;
import com.velocitymall.admin.entity.AdminRole;
import com.velocitymall.admin.entity.AdminRolePermission;
import com.velocitymall.admin.entity.AdminRoleRelation;
import com.velocitymall.admin.mapper.AdminMapper;
import com.velocitymall.admin.mapper.AdminPermissionMapper;
import com.velocitymall.admin.mapper.AdminRoleMapper;
import com.velocitymall.admin.mapper.AdminRolePermissionMapper;
import com.velocitymall.admin.mapper.AdminRoleRelationMapper;
import com.velocitymall.admin.model.dto.AdminPasswordResetRequest;
import com.velocitymall.admin.model.dto.AdminRoleRequest;
import com.velocitymall.admin.model.dto.AdminStatusRequest;
import com.velocitymall.admin.model.dto.AdminUserCreateRequest;
import com.velocitymall.admin.model.dto.AdminUserUpdateRequest;
import com.velocitymall.admin.model.vo.AdminPermissionVO;
import com.velocitymall.admin.model.vo.AdminRoleVO;
import com.velocitymall.admin.model.vo.AdminUserVO;
import com.velocitymall.admin.service.AdminRbacService;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.ResultCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminRbacServiceImpl implements AdminRbacService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminRoleRelationMapper adminRoleRelationMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public PageVO<AdminUserVO> listAdmins(Long page, Long size, String keyword, Integer status) {
        Page<Admin> adminPage = adminMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Admin>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(Admin::getUsername, keyword)
                                .or()
                                .like(Admin::getRealName, keyword))
                        .eq(status != null, Admin::getStatus, status)
                        .orderByDesc(Admin::getUpdateTime)
        );
        List<AdminUserVO> records = adminPage.getRecords().stream().map(this::toAdminUserVO).toList();
        return new PageVO<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal(),
                adminPage.getPages(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createAdmin(AdminUserCreateRequest request) {
        List<Long> roleIds = validateRoleIds(request.getRoleIds());
        Admin admin = Admin.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(trimToNull(request.getRealName()))
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_ENABLED))
                .build();
        try {
            adminMapper.insert(admin);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "管理员账号已存在");
        }
        replaceAdminRoles(admin.getId(), roleIds);
        return toAdminUserVO(adminMapper.selectById(admin.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdmin(Long adminId, AdminUserUpdateRequest request) {
        Admin admin = requireAdmin(adminId);
        List<Long> roleIds = validateRoleIds(request.getRoleIds());
        Long currentAdminId = AdminContext.getAdminId();
        if (Objects.equals(adminId, currentAdminId)) {
            if (normalizeBinaryStatus(request.getStatus(), admin.getStatus()) == STATUS_DISABLED) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "不能禁用当前登录管理员");
            }
            if (!sameIds(roleIds, currentRoleIds(adminId))) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "不能修改当前登录管理员的角色");
            }
        }
        Admin update = Admin.builder()
                .id(adminId)
                .realName(trimToNull(request.getRealName()))
                .status(normalizeBinaryStatus(request.getStatus(), admin.getStatus()))
                .build();
        adminMapper.updateById(update);
        replaceAdminRoles(adminId, roleIds);
        return toAdminUserVO(requireAdmin(adminId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdminStatus(Long adminId, AdminStatusRequest request) {
        requireAdmin(adminId);
        int status = normalizeBinaryStatus(request.getStatus(), STATUS_ENABLED);
        if (Objects.equals(adminId, AdminContext.getAdminId()) && status == STATUS_DISABLED) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "不能禁用当前登录管理员");
        }
        adminMapper.updateById(Admin.builder().id(adminId).status(status).build());
        return toAdminUserVO(requireAdmin(adminId));
    }

    @Override
    public void resetAdminPassword(Long adminId, AdminPasswordResetRequest request) {
        requireAdmin(adminId);
        Admin update = Admin.builder()
                .id(adminId)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        adminMapper.updateById(update);
    }

    @Override
    public List<AdminRoleVO> listRoles() {
        return adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                        .orderByAsc(AdminRole::getRoleCode))
                .stream()
                .map(this::toAdminRoleVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleVO createRole(AdminRoleRequest request) {
        List<Long> permissionIds = validatePermissionIds(request.getPermissionIds());
        AdminRole role = AdminRole.builder()
                .roleCode(request.getRoleCode().trim().toUpperCase())
                .roleName(request.getRoleName().trim())
                .description(trimToNull(request.getDescription()))
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_ENABLED))
                .build();
        try {
            adminRoleMapper.insert(role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "角色编码已存在");
        }
        replaceRolePermissions(role.getId(), permissionIds);
        return toAdminRoleVO(requireRole(role.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleVO updateRole(Long roleId, AdminRoleRequest request) {
        AdminRole role = requireRole(roleId);
        if (!role.getRoleCode().equals(request.getRoleCode().trim().toUpperCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色编码不允许修改");
        }
        int status = normalizeBinaryStatus(request.getStatus(), role.getStatus());
        if (AdminRoleCodes.SUPER_ADMIN.equals(role.getRoleCode()) && status == STATUS_DISABLED) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "不能禁用超级管理员角色");
        }
        AdminRole update = AdminRole.builder()
                .id(roleId)
                .roleName(request.getRoleName().trim())
                .description(trimToNull(request.getDescription()))
                .status(status)
                .build();
        adminRoleMapper.updateById(update);
        if (AdminRoleCodes.SUPER_ADMIN.equals(role.getRoleCode())) {
            replaceRolePermissions(roleId, allEnabledPermissionIds());
        } else {
            replaceRolePermissions(roleId, validatePermissionIds(request.getPermissionIds()));
        }
        return toAdminRoleVO(requireRole(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleVO updateRoleStatus(Long roleId, AdminStatusRequest request) {
        AdminRole role = requireRole(roleId);
        int status = normalizeBinaryStatus(request.getStatus(), role.getStatus());
        if (AdminRoleCodes.SUPER_ADMIN.equals(role.getRoleCode()) && status == STATUS_DISABLED) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "不能禁用超级管理员角色");
        }
        adminRoleMapper.updateById(AdminRole.builder().id(roleId).status(status).build());
        return toAdminRoleVO(requireRole(roleId));
    }

    @Override
    public List<AdminPermissionVO> listPermissions() {
        return adminPermissionMapper.selectList(new LambdaQueryWrapper<AdminPermission>()
                        .orderByAsc(AdminPermission::getResource)
                        .orderByAsc(AdminPermission::getPermissionCode))
                .stream()
                .map(this::toAdminPermissionVO)
                .toList();
    }

    private Admin requireAdmin(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "管理员不存在");
        }
        return admin;
    }

    private AdminRole requireRole(Long roleId) {
        AdminRole role = adminRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "角色不存在");
        }
        return role;
    }

    private List<Long> validateRoleIds(List<String> roleIds) {
        List<Long> normalized = normalizeIds(roleIds);
        if (normalized.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "至少选择一个角色");
        }
        Long count = adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>()
                .in(AdminRole::getId, normalized)
                .eq(AdminRole::getStatus, STATUS_ENABLED));
        if (count == null || count != normalized.size()) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "角色不存在或已禁用");
        }
        return normalized;
    }

    private List<Long> validatePermissionIds(List<String> permissionIds) {
        List<Long> normalized = normalizeIds(permissionIds);
        if (normalized.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "至少选择一个权限");
        }
        Long count = adminPermissionMapper.selectCount(new LambdaQueryWrapper<AdminPermission>()
                .in(AdminPermission::getId, normalized)
                .eq(AdminPermission::getStatus, STATUS_ENABLED));
        if (count == null || count != normalized.size()) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "权限不存在或已禁用");
        }
        return normalized;
    }

    private List<Long> normalizeIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Long> normalized = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            try {
                Long parsed = Long.valueOf(id);
                if (parsed > 0 && seen.add(parsed)) {
                    normalized.add(parsed);
                }
            } catch (NumberFormatException exception) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "ID格式错误");
            }
        }
        return normalized;
    }

    private void replaceAdminRoles(Long adminId, List<Long> roleIds) {
        adminRoleRelationMapper.deleteByAdminId(adminId);
        for (Long roleId : roleIds) {
            adminRoleRelationMapper.insert(AdminRoleRelation.builder()
                    .adminId(adminId)
                    .roleId(roleId)
                    .build());
        }
    }

    private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        adminRolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : permissionIds) {
            adminRolePermissionMapper.insert(AdminRolePermission.builder()
                    .roleId(roleId)
                    .permissionId(permissionId)
                    .build());
        }
    }

    private List<Long> allEnabledPermissionIds() {
        return adminPermissionMapper.selectList(new LambdaQueryWrapper<AdminPermission>()
                        .eq(AdminPermission::getStatus, STATUS_ENABLED))
                .stream()
                .map(AdminPermission::getId)
                .toList();
    }

    private List<Long> currentRoleIds(Long adminId) {
        return adminRoleMapper.selectRolesByAdminId(adminId).stream()
                .map(AdminRole::getId)
                .sorted()
                .toList();
    }

    private boolean sameIds(List<Long> first, List<Long> second) {
        return first.stream().sorted().toList().equals(second.stream().sorted().toList());
    }

    private int normalizeBinaryStatus(Integer status, Integer defaultStatus) {
        int resolved = status == null ? defaultStatus : status;
        if (resolved != STATUS_DISABLED && resolved != STATUS_ENABLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status只能为0或1");
        }
        return resolved;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private AdminUserVO toAdminUserVO(Admin admin) {
        List<AdminRoleVO> roles = adminRoleMapper.selectRolesByAdminId(admin.getId()).stream()
                .map(role -> AdminRoleVO.builder()
                        .id(String.valueOf(role.getId()))
                        .roleCode(role.getRoleCode())
                        .roleName(role.getRoleName())
                        .description(role.getDescription())
                        .status(role.getStatus())
                        .permissions(List.of())
                        .createTime(role.getCreateTime())
                        .updateTime(role.getUpdateTime())
                        .build())
                .toList();
        return AdminUserVO.builder()
                .adminId(String.valueOf(admin.getId()))
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .status(admin.getStatus())
                .roles(roles)
                .createTime(admin.getCreateTime())
                .updateTime(admin.getUpdateTime())
                .build();
    }

    private AdminRoleVO toAdminRoleVO(AdminRole role) {
        List<AdminPermissionVO> permissions = adminPermissionMapper.selectPermissionsByRoleId(role.getId()).stream()
                .map(this::toAdminPermissionVO)
                .toList();
        return AdminRoleVO.builder()
                .id(String.valueOf(role.getId()))
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .permissions(permissions)
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .build();
    }

    private AdminPermissionVO toAdminPermissionVO(AdminPermission permission) {
        return AdminPermissionVO.builder()
                .id(String.valueOf(permission.getId()))
                .permissionCode(permission.getPermissionCode())
                .permissionName(permission.getPermissionName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .status(permission.getStatus())
                .build();
    }
}
