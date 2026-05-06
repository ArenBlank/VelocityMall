package com.velocitymall.common.config;

import com.velocitymall.common.constant.TraceConstant;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Propagates the current trace ID to OpenFeign requests.
 */
@Configuration(proxyBeanMethods = false)
public class FeignTraceConfig {

    @Bean
    public RequestInterceptor feignTraceInterceptor() {
        return template -> {
            String traceId = MDC.get(TraceConstant.TRACE_ID);
            if (StringUtils.hasText(traceId)) {
                template.header(TraceConstant.TRACE_ID, traceId);
            }
        };
    }
}
