package com.velocitymall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.param.RequestItemParser;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * Sentinel gateway flow control configuration.
 */
@Configuration
public class SentinelGatewayConfig {

    private static final String SECKILL_API_NAME = "seckill_api";

    private static final String SECKILL_API_PATH_PATTERN = "/api/v1/seckill/execute/.*";

    private static final String SECKILL_SKU_REGEX_PATTERN = "/api/v1/seckill/execute/(.*)";

    private static final Pattern SECKILL_SKU_PATTERN = Pattern.compile(SECKILL_SKU_REGEX_PATTERN);

    private static final String SECKILL_SKU_FIELD_NAME = "skuId";

    private static final double SECKILL_QPS_LIMIT =
            Double.parseDouble(System.getenv().getOrDefault("SECKILL_QPS_LIMIT", "5.0"));

    private static final long INTERVAL_SECONDS = 1L;

    private static final int SENTINEL_GATEWAY_FILTER_ORDER = -1;

    @PostConstruct
    public void init() {
        initCustomBlockHandler();
        initCustomApiDefinitions();
        initGatewayRules();
    }

    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter(SENTINEL_GATEWAY_FILTER_ORDER, seckillSkuRequestItemParser());
    }

    private void initCustomBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, throwable) -> {
            Map<String, Object> responseBody = new HashMap<>(4);
            responseBody.put("code", 42900);
            responseBody.put("message", "当前抢购人数过多，请稍后再试");
            responseBody.put("data", null);

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(responseBody));
        };
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }

    private void initCustomApiDefinitions() {
        Set<ApiPredicateItem> predicateItems = new HashSet<>();
        predicateItems.add(new ApiPathPredicateItem()
                .setPattern(SECKILL_API_PATH_PATTERN)
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_REGEX));

        Set<ApiDefinition> definitions = new HashSet<>();
        definitions.add(new ApiDefinition(SECKILL_API_NAME).setPredicateItems(predicateItems));
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initGatewayRules() {
        GatewayParamFlowItem skuParamItem = new GatewayParamFlowItem()
                .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                .setFieldName(SECKILL_SKU_FIELD_NAME);

        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule(SECKILL_API_NAME)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(SECKILL_QPS_LIMIT)
                .setIntervalSec(INTERVAL_SECONDS)
                .setParamItem(skuParamItem));
        GatewayRuleManager.loadRules(rules);
    }

    private RequestItemParser<ServerWebExchange> seckillSkuRequestItemParser() {
        return new RequestItemParser<>() {
            @Override
            public String getPath(ServerWebExchange exchange) {
                return exchange.getRequest().getPath().value();
            }

            @Override
            public String getRemoteAddress(ServerWebExchange exchange) {
                if (exchange.getRequest().getRemoteAddress() == null
                        || exchange.getRequest().getRemoteAddress().getAddress() == null) {
                    return null;
                }
                return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }

            @Override
            public String getHeader(ServerWebExchange exchange, String headerName) {
                return exchange.getRequest().getHeaders().getFirst(headerName);
            }

            @Override
            public String getUrlParam(ServerWebExchange exchange, String fieldName) {
                ServerHttpRequest request = exchange.getRequest();
                if (SECKILL_SKU_FIELD_NAME.equals(fieldName)) {
                    Matcher matcher = SECKILL_SKU_PATTERN.matcher(request.getPath().value());
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
                return request.getQueryParams().getFirst(fieldName);
            }

            @Override
            public String getCookieValue(ServerWebExchange exchange, String cookieName) {
                HttpCookie cookie = exchange.getRequest().getCookies().getFirst(cookieName);
                return cookie == null ? null : cookie.getValue();
            }
        };
    }
}
