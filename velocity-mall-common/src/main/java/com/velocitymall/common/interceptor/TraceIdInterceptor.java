package com.velocitymall.common.interceptor;

import com.velocitymall.common.constant.TraceConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Loads the HTTP trace ID into MDC for service logs.
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TraceConstant.TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        traceId = traceId.trim();
        MDC.put(TraceConstant.TRACE_ID, traceId);
        response.setHeader(TraceConstant.TRACE_ID, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TraceConstant.TRACE_ID);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
