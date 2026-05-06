package com.velocitymall.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局鉴权过滤器。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String ADMIN_ID_HEADER = "X-Admin-Id";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String[] GET_WHITE_LIST = {
            "/api/v1/products/spus/**",
            "/api/v1/products/skus/*",
            "/api/v1/search/**",
            "/api/v1/categories/tree",
            "/api/v1/reviews/products/**"
    };

    private static final String[] POST_WHITE_LIST = {
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/admin/login"
    };

    private static final String[] BLACK_LIST = {
            "/api/v1/products/inner/**",
            "/api/v1/search/inner/**",
            "/api/v1/orders/inner/**",
            "/api/v1/users/inner/**",
            "/api/v1/products/skus/lock-stock",
            "/api/v1/products/skus/unlock-stock"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isBlackRequest(path)) {
            log.warn("非法外网访问内部接口拦截: {}", path);
            return errorResponse(exchange, HttpStatus.FORBIDDEN, 40300, "禁止访问内部接口");
        }

        if (isWhiteRequest(request, path)) {
            return optionalAuthFilter(exchange, chain, request);
        }

        String token = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(token) || !token.startsWith(BEARER_PREFIX)) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "缺少身份凭证");
        }

        if (path.startsWith("/api/v1/admin/")) {
            return adminAuthFilter(exchange, chain, request, token);
        }

        return authenticatedFilter(exchange, chain, request, token);
    }

    private Mono<Void> optionalAuthFilter(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ServerHttpRequest request
    ) {
        String token = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(token)) {
            ServerHttpRequest anonymousRequest = request.mutate()
                    .headers(headers -> headers.remove(USER_ID_HEADER))
                    .build();
            return chain.filter(exchange.mutate().request(anonymousRequest).build());
        }
        if (!token.startsWith(BEARER_PREFIX)) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "凭证无效");
        }
        return authenticatedFilter(exchange, chain, request, token);
    }

    private Mono<Void> authenticatedFilter(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ServerHttpRequest request,
            String token
    ) {
        try {
            Claims claims = parseClaims(token.substring(BEARER_PREFIX.length()));
            String userId = String.valueOf(claims.get("userId"));
            if (!StringUtils.hasText(userId) || "null".equals(userId)) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "凭证无效");
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.set(USER_ID_HEADER, userId);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception exception) {
            log.error("JWT 解析失败: {}", exception.getMessage());
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "凭证已过期或非法");
        }
    }

    private Mono<Void> adminAuthFilter(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ServerHttpRequest request,
            String token
    ) {
        try {
            Claims claims = parseClaims(token.substring(BEARER_PREFIX.length()));
            Object adminIdObj = claims.get("adminId");
            if (adminIdObj == null) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "非法的管理后台凭证");
            }
            String adminId = String.valueOf(adminIdObj);
            if (!StringUtils.hasText(adminId) || "null".equals(adminId)) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "凭证无效");
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(ADMIN_ID_HEADER);
                        headers.set(ADMIN_ID_HEADER, adminId);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception exception) {
            log.error("Admin JWT 解析失败: {}", exception.getMessage());
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, 40100, "凭证已过期或非法");
        }
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private boolean isBlackRequest(String path) {
        for (String blackPath : BLACK_LIST) {
            if (pathMatcher.match(blackPath, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWhiteRequest(ServerHttpRequest request, String path) {
        if (HttpMethod.GET.equals(request.getMethod())) {
            return matchesGetWhiteList(path);
        }
        if (HttpMethod.POST.equals(request.getMethod())) {
            return matchesPostWhiteList(path);
        }
        return false;
    }

    private boolean matchesGetWhiteList(String path) {
        if (pathMatcher.match(GET_WHITE_LIST[0], path)) {
            return true;
        }
        if (pathMatcher.match(GET_WHITE_LIST[1], path)) {
            return isNumericSkuPath(path);
        }
        if (pathMatcher.match(GET_WHITE_LIST[2], path)) {
            return true;
        }
        if (pathMatcher.match(GET_WHITE_LIST[3], path)) {
            return true;
        }
        return pathMatcher.match(GET_WHITE_LIST[4], path);
    }

    private boolean matchesPostWhiteList(String path) {
        for (String pattern : POST_WHITE_LIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumericSkuPath(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex == path.length() - 1) {
            return false;
        }
        String skuId = path.substring(lastSlashIndex + 1);
        return skuId.chars().allMatch(Character::isDigit);
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));

        Map<String, Object> resultMap = new HashMap<>(4);
        resultMap.put("code", code);
        resultMap.put("message", message);
        resultMap.put("data", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(resultMap);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
