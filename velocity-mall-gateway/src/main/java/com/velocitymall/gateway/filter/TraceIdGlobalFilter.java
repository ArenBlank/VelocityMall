package com.velocitymall.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures every request entering the gateway carries a trace ID.
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        } else {
            traceId = traceId.trim();
        }

        String finalTraceId = traceId;
        ServerHttpRequest tracedRequest = request.mutate()
                .headers(headers -> headers.set(TRACE_ID_HEADER, finalTraceId))
                .build();
        exchange.getAttributes().put(TRACE_ID_HEADER, finalTraceId);
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, finalTraceId);
        return chain.filter(exchange.mutate().request(tracedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
