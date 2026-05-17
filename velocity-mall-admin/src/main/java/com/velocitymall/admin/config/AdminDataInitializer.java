package com.velocitymall.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.admin.constant.AdminPermissionCodes;
import com.velocitymall.admin.constant.AdminRoleCodes;
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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Bootstraps built-in admin accounts, roles, and permissions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private static final int STATUS_ENABLED = 1;

    private static final List<PermissionDefinition> PERMISSIONS = List.of(
            new PermissionDefinition(AdminPermissionCodes.PRODUCT_READ, "商品查看", "product", "read", "查看商品列表和详情"),
            new PermissionDefinition(AdminPermissionCodes.PRODUCT_WRITE, "商品维护", "product", "write", "新增、编辑、上下架商品和维护封面"),
            new PermissionDefinition(AdminPermissionCodes.ORDER_READ, "订单查看", "order", "read", "查看订单列表和详情"),
            new PermissionDefinition(AdminPermissionCodes.ORDER_DELIVER, "订单发货", "order", "deliver", "对已付款订单执行发货"),
            new PermissionDefinition(AdminPermissionCodes.SECKILL_READ, "秒杀查看", "seckill", "read", "查看秒杀活动"),
            new PermissionDefinition(AdminPermissionCodes.SECKILL_WRITE, "秒杀维护", "seckill", "write", "新增、编辑、启停秒杀活动"),
            new PermissionDefinition(AdminPermissionCodes.SECKILL_PREHEAT, "秒杀预热", "seckill", "preheat", "手动预热秒杀库存"),
            new PermissionDefinition(AdminPermissionCodes.COUPON_READ, "优惠券查看", "coupon", "read", "查看优惠券"),
            new PermissionDefinition(AdminPermissionCodes.COUPON_WRITE, "优惠券维护", "coupon", "write", "新增、编辑、启停优惠券"),
            new PermissionDefinition(AdminPermissionCodes.REVIEW_READ, "评价查看", "review", "read", "查看商品评价"),
            new PermissionDefinition(AdminPermissionCodes.REVIEW_DELETE, "评价删除", "review", "delete", "删除不合规评价"),
            new PermissionDefinition(AdminPermissionCodes.SYSTEM_REBUILD, "搜索索引重建", "system", "rebuild", "手动重建 SKU 搜索索引"),
            new PermissionDefinition(AdminPermissionCodes.RBAC_READ, "权限查看", "rbac", "read", "查看管理员、角色和权限"),
            new PermissionDefinition(AdminPermissionCodes.RBAC_WRITE, "权限维护", "rbac", "write", "维护管理员、角色和授权关系")
    );

    private static final List<RoleDefinition> ROLES = List.of(
            new RoleDefinition(AdminRoleCodes.SUPER_ADMIN, "超级管理员", "拥有全部后台权限"),
            new RoleDefinition(AdminRoleCodes.OPS_STAFF, "运营人员", "维护商品、秒杀、优惠券和评价，不包含发货和系统运维"),
            new RoleDefinition(AdminRoleCodes.VIEWER, "只读人员", "只能查看后台运营数据")
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            AdminRoleCodes.SUPER_ADMIN, PERMISSIONS.stream().map(PermissionDefinition::code).toList(),
            AdminRoleCodes.OPS_STAFF, List.of(
                    AdminPermissionCodes.PRODUCT_READ,
                    AdminPermissionCodes.PRODUCT_WRITE,
                    AdminPermissionCodes.ORDER_READ,
                    AdminPermissionCodes.SECKILL_READ,
                    AdminPermissionCodes.SECKILL_WRITE,
                    AdminPermissionCodes.SECKILL_PREHEAT,
                    AdminPermissionCodes.COUPON_READ,
                    AdminPermissionCodes.COUPON_WRITE,
                    AdminPermissionCodes.REVIEW_READ,
                    AdminPermissionCodes.REVIEW_DELETE
            ),
            AdminRoleCodes.VIEWER, List.of(
                    AdminPermissionCodes.PRODUCT_READ,
                    AdminPermissionCodes.ORDER_READ,
                    AdminPermissionCodes.SECKILL_READ,
                    AdminPermissionCodes.COUPON_READ,
                    AdminPermissionCodes.REVIEW_READ
            )
    );

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final AdminRoleRelationMapper adminRoleRelationMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        PERMISSIONS.forEach(this::ensurePermission);
        ROLES.forEach(this::ensureRole);
        ROLE_PERMISSIONS.forEach(this::ensureRolePermissions);

        Admin superAdmin = ensureAdmin("admin", "123456", "系统管理员");
        Admin operator = ensureAdmin("operator", "123456", "运营人员");
        Admin viewer = ensureAdmin("viewer", "123456", "只读人员");
        ensureAdminRole(superAdmin.getId(), AdminRoleCodes.SUPER_ADMIN);
        ensureAdminRole(operator.getId(), AdminRoleCodes.OPS_STAFF);
        ensureAdminRole(viewer.getId(), AdminRoleCodes.VIEWER);
        log.info("Admin RBAC bootstrap finished: admin/operator/viewer");
    }

    private void ensurePermission(PermissionDefinition definition) {
        AdminPermission existing = selectPermission(definition.code());
        if (existing == null) {
            adminPermissionMapper.insert(AdminPermission.builder()
                    .permissionCode(definition.code())
                    .permissionName(definition.name())
                    .resource(definition.resource())
                    .action(definition.action())
                    .description(definition.description())
                    .status(STATUS_ENABLED)
                    .build());
            return;
        }
        AdminPermission update = AdminPermission.builder()
                .id(existing.getId())
                .permissionName(definition.name())
                .resource(definition.resource())
                .action(definition.action())
                .description(definition.description())
                .status(STATUS_ENABLED)
                .build();
        adminPermissionMapper.updateById(update);
    }

    private void ensureRole(RoleDefinition definition) {
        AdminRole existing = selectRole(definition.code());
        if (existing == null) {
            adminRoleMapper.insert(AdminRole.builder()
                    .roleCode(definition.code())
                    .roleName(definition.name())
                    .description(definition.description())
                    .status(STATUS_ENABLED)
                    .build());
            return;
        }
        AdminRole update = AdminRole.builder()
                .id(existing.getId())
                .roleName(definition.name())
                .description(definition.description())
                .status(STATUS_ENABLED)
                .build();
        adminRoleMapper.updateById(update);
    }

    private Admin ensureAdmin(String username, String password, String realName) {
        Admin existing = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)
                .last("LIMIT 1"));
        if (existing == null) {
            Admin admin = Admin.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .realName(realName)
                    .status(STATUS_ENABLED)
                    .build();
            adminMapper.insert(admin);
            return admin;
        }
        Admin update = Admin.builder()
                .id(existing.getId())
                .realName(StringUtils.hasText(existing.getRealName()) ? existing.getRealName() : realName)
                .status(STATUS_ENABLED)
                .build();
        adminMapper.updateById(update);
        return adminMapper.selectById(existing.getId());
    }

    private void ensureRolePermissions(String roleCode, List<String> permissionCodes) {
        AdminRole role = selectRole(roleCode);
        if (role == null) {
            return;
        }
        for (String permissionCode : permissionCodes) {
            AdminPermission permission = selectPermission(permissionCode);
            if (permission != null) {
                ensureRolePermission(role.getId(), permission.getId());
            }
        }
    }

    private void ensureRolePermission(Long roleId, Long permissionId) {
        Long count = adminRolePermissionMapper.selectCount(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId)
                .eq(AdminRolePermission::getPermissionId, permissionId));
        if (count != null && count > 0) {
            return;
        }
        adminRolePermissionMapper.insert(AdminRolePermission.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build());
    }

    private void ensureAdminRole(Long adminId, String roleCode) {
        AdminRole role = selectRole(roleCode);
        if (role == null) {
            return;
        }
        Long count = adminRoleRelationMapper.selectCount(new LambdaQueryWrapper<AdminRoleRelation>()
                .eq(AdminRoleRelation::getAdminId, adminId)
                .eq(AdminRoleRelation::getRoleId, role.getId()));
        if (count != null && count > 0) {
            return;
        }
        adminRoleRelationMapper.insert(AdminRoleRelation.builder()
                .adminId(adminId)
                .roleId(role.getId())
                .build());
    }

    private AdminRole selectRole(String roleCode) {
        return adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    private AdminPermission selectPermission(String permissionCode) {
        return adminPermissionMapper.selectOne(new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getPermissionCode, permissionCode)
                .last("LIMIT 1"));
    }

    private record PermissionDefinition(
            String code,
            String name,
            String resource,
            String action,
            String description
    ) {
    }

    private record RoleDefinition(String code, String name, String description) {
    }
}
