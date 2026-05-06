package com.velocitymall.common.context;

import com.velocitymall.common.constant.TraceConstant;
import com.velocitymall.common.model.dto.BaseMessageDTO;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * Trace context helper for RocketMQ producers and consumers.
 */
public final class MqTraceContext {

    private MqTraceContext() {
    }

    public static <T extends BaseMessageDTO> T prepare(T message, String businessId) {
        if (message == null) {
            return null;
        }
        String traceId = MDC.get(TraceConstant.TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        message.setTraceId(traceId);
        message.setBusinessId(businessId);
        return message;
    }

    public static void runWithTrace(BaseMessageDTO message, Runnable action) {
        String traceId = message == null ? null : message.getTraceId();
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
            if (message != null) {
                message.setTraceId(traceId);
            }
        }
        MDC.put(TraceConstant.TRACE_ID, traceId);
        try {
            action.run();
        } finally {
            MDC.remove(TraceConstant.TRACE_ID);
        }
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
