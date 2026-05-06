package com.velocitymall.review.interceptor;

import com.velocitymall.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Optional user context loader for public review read APIs.
 */
@Slf4j
@Component
public class OptionalUserInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userId)) {
            try {
                UserContext.setUserId(Long.valueOf(userId));
            } catch (NumberFormatException exception) {
                log.warn("Optional user context ignored, invalid userId: {}", userId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.removeUserId();
    }
}
