package com.velocitymall.admin.interceptor;

import com.velocitymall.admin.annotation.RequireAdminPermission;
import com.velocitymall.admin.context.AdminContext;
import com.velocitymall.admin.entity.Admin;
import com.velocitymall.admin.mapper.AdminMapper;
import com.velocitymall.admin.mapper.AdminPermissionMapper;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ID_HEADER = "X-Admin-Id";

    private static final int STATUS_ENABLED = 1;

    private final AdminMapper adminMapper;

    private final AdminPermissionMapper adminPermissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String adminId = request.getHeader(ADMIN_ID_HEADER);
        if (!StringUtils.hasText(adminId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        try {
            Long parsedAdminId = Long.valueOf(adminId);
            Admin admin = adminMapper.selectById(parsedAdminId);
            if (admin == null || admin.getStatus() == null || admin.getStatus() != STATUS_ENABLED) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "管理员不存在或已禁用");
            }
            verifyPermission(handler, parsedAdminId);
            AdminContext.setAdminId(parsedAdminId);
            return true;
        } catch (NumberFormatException exception) {
            log.warn("非法管理员上下文, adminId: {}", adminId);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.removeAdminId();
    }

    private void verifyPermission(Object handler, Long adminId) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        RequireAdminPermission annotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                RequireAdminPermission.class
        );
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(),
                    RequireAdminPermission.class
            );
        }
        if (annotation == null || !StringUtils.hasText(annotation.value())) {
            return;
        }

        List<String> permissionCodes = adminPermissionMapper.selectPermissionCodesByAdminId(adminId);
        if (!permissionCodes.contains(annotation.value())) {
            log.warn("管理员权限不足, adminId: {}, required: {}", adminId, annotation.value());
            throw new BusinessException(ResultCode.FORBIDDEN, "无访问权限");
        }
    }
}
