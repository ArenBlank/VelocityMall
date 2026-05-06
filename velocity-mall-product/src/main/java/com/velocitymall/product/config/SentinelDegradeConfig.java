package com.velocitymall.product.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel degradation rule — triggers circuit breaker when avg RT exceeds 200ms.
 * Once open, fallback is invoked for ALL requests until the recovery window passes.
 */
@Slf4j
@Configuration
public class SentinelDegradeConfig {

    private static final String RESOURCE_NAME = "getSkuInfo";
    private static final double SLOW_REQUEST_RT_MS = 200.0;
    private static final int STAT_INTERVAL_MS = 1000;
    private static final int MIN_REQUEST_COUNT = 5;
    private static final double SLOW_RATIO_THRESHOLD = 0.5;
    private static final int RECOVERY_TIMEOUT_S = 10;

    @PostConstruct
    public void initDegradeRule() {
        List<DegradeRule> rules = new ArrayList<>();
        DegradeRule rule = new DegradeRule(RESOURCE_NAME)
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(SLOW_REQUEST_RT_MS)
                .setStatIntervalMs(STAT_INTERVAL_MS)
                .setMinRequestAmount(MIN_REQUEST_COUNT)
                .setSlowRatioThreshold(SLOW_RATIO_THRESHOLD)
                .setTimeWindow(RECOVERY_TIMEOUT_S);
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel degrade rule loaded for resource '{}': RT>{}ms, minReq={}, slowRatio={}, recoveryWindow={}s",
                RESOURCE_NAME, SLOW_REQUEST_RT_MS, MIN_REQUEST_COUNT, SLOW_RATIO_THRESHOLD, RECOVERY_TIMEOUT_S);
    }
}
