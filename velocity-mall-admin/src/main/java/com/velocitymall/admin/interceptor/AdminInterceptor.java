package com.velocitymall.admin.interceptor;

import com.velocitymall.admin.context.AdminContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ID_HEADER = "X-Admin-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String adminId = request.getHeader(ADMIN_ID_HEADER);
        if (!StringUtils.hasText(adminId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        try {
            AdminContext.setAdminId(Long.valueOf(adminId));
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
}
