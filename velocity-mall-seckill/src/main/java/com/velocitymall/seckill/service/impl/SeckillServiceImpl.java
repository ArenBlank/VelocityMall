package com.velocitymall.seckill.service.impl;

import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.SeckillOrderDTO;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.seckill.entity.SeckillActivity;
import com.velocitymall.seckill.service.SeckillActivityService;
import com.velocitymall.seckill.service.SeckillService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Seckill service implementation backed by Redis Lua and RocketMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String SECKILL_HASH_TAG_PREFIX = "velocitymall:seckill:{";

    private static final String STOCK_KEY_INFIX = "}:stock";

    private static final String BOUGHT_KEY_INFIX = "}:bought";

    private static final String QPS_KEY_INFIX = "}:qps";

    private static final String LATENCY_KEY_INFIX = "}:latency";

    private static final String MQ_SENT_KEY_INFIX = "}:mq-sent";

    private static String stockKey(Object skuId) { return SECKILL_HASH_TAG_PREFIX + skuId + STOCK_KEY_INFIX; }

    private static String boughtKey(Object skuId) { return SECKILL_HASH_TAG_PREFIX + skuId + BOUGHT_KEY_INFIX; }

    private static String qpsKey(Object skuId) { return SECKILL_HASH_TAG_PREFIX + skuId + QPS_KEY_INFIX; }

    private static String latencyKey(Object skuId) { return SECKILL_HASH_TAG_PREFIX + skuId + LATENCY_KEY_INFIX; }

    private static String mqSentKey(Object skuId) { return SECKILL_HASH_TAG_PREFIX + skuId + MQ_SENT_KEY_INFIX; }

    private static final int LATENCY_SAMPLE_SIZE = 100;

    private static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";

    private static final int DEFAULT_QUANTITY = 1;

    private static final Long LUA_SUCCESS = 1L;

    private static final Long LUA_SOLD_OUT = 0L;

    private static final Long LUA_DUPLICATE = -1L;

    private static final String EXECUTE_SCRIPT_TEXT = """
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                return -1
            end
            local stock = redis.call('GET', KEYS[1])
            if (not stock) or (tonumber(stock) < tonumber(ARGV[2])) then
                return 0
            end
            redis.call('DECRBY', KEYS[1], ARGV[2])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """;

    private static final String COMPENSATE_SCRIPT_TEXT = """
            if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then
                redis.call('INCRBY', KEYS[1], ARGV[2])
                return 1
            end
            return 0
            """;

    private static final String ROLLBACK_SCRIPT_TEXT = """
            local stockKey = KEYS[1]
            local boughtKey = KEYS[2]
            local userId = ARGV[1]
            local removed = redis.call('srem', boughtKey, userId)
            if removed == 1 then
                redis.call('incrby', stockKey, 1)
                return 1
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> EXECUTE_SCRIPT = createScript(EXECUTE_SCRIPT_TEXT);

    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = createScript(COMPENSATE_SCRIPT_TEXT);

    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = createScript(ROLLBACK_SCRIPT_TEXT);

    private final StringRedisTemplate stringRedisTemplate;

    private final RocketMQTemplate rocketMQTemplate;

    private final SeckillActivityService seckillActivityService;

    @Override
    public String execute(Long skuId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return execute(skuId, userId);
    }

    @Override
    public String execute(Long skuId, Long userId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID 不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户 ID 非法");
        }

        long start = System.currentTimeMillis();
        trackQps(skuId);

        SeckillActivity activity = seckillActivityService.requireActiveActivity(skuId);
        String stockKey = stockKey(skuId);
        String boughtKey = boughtKey(skuId);
        String userIdValue = String.valueOf(userId);
        String quantityValue = String.valueOf(DEFAULT_QUANTITY);

        Long luaResult = stringRedisTemplate.execute(
                EXECUTE_SCRIPT,
                List.of(stockKey, boughtKey),
                userIdValue,
                quantityValue
        );

        if (LUA_DUPLICATE.equals(luaResult)) {
            trackLatency(skuId, start);
            throw new BusinessException(ResultCode.BIZ_WARNING, "请勿重复抢购");
        }
        if (LUA_SOLD_OUT.equals(luaResult)) {
            trackLatency(skuId, start);
            throw new BusinessException(ResultCode.BIZ_WARNING, "商品已抢光");
        }
        if (!LUA_SUCCESS.equals(luaResult)) {
            trackLatency(skuId, start);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "秒杀系统繁忙，请稍后重试");
        }

        trackLatency(skuId, start);

        String orderSn = generateSeckillOrderSn();
        SeckillOrderDTO messageDTO = new SeckillOrderDTO(
                userId,
                skuId,
                DEFAULT_QUANTITY,
                orderSn,
                activity.getSeckillPrice()
        );
        MqTraceContext.prepare(messageDTO, orderSn);
        sendSeckillOrderMessage(messageDTO, stockKey, boughtKey, userIdValue, quantityValue);
        return "秒杀成功，正在排队中...";
    }

    @Override
    public Long rollbackStock(Long skuId, Long userId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID 不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户 ID 非法");
        }

        String stockKey = stockKey(skuId);
        String boughtKey = boughtKey(skuId);
        Long result = stringRedisTemplate.execute(
                ROLLBACK_SCRIPT,
                List.of(stockKey, boughtKey),
                String.valueOf(userId)
        );
        return result == null ? 0L : result;
    }

    private void sendSeckillOrderMessage(
            SeckillOrderDTO messageDTO,
            String stockKey,
            String boughtKey,
            String userIdValue,
            String quantityValue
    ) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    SECKILL_ORDER_TOPIC,
                    MessageBuilder.withPayload(messageDTO).build()
            );
            if (sendResult == null || SendStatus.SEND_OK != sendResult.getSendStatus()) {
                compensateSeckillStock(stockKey, boughtKey, userIdValue, quantityValue);
                log.error("Seckill order message send failed. orderSn: {}, sendResult: {}",
                        messageDTO.getOrderSn(), sendResult);
                throw new BusinessException(ResultCode.BIZ_WARNING, "秒杀排队失败，请稍后重试");
            }
            log.info("Seckill order message sent. orderSn: {}, skuId: {}",
                    messageDTO.getOrderSn(), messageDTO.getSkuId());
            trackMqSent(messageDTO.getSkuId());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            compensateSeckillStock(stockKey, boughtKey, userIdValue, quantityValue);
            log.error("Seckill order message send exception, Redis occupation compensated. orderSn: {}",
                    messageDTO.getOrderSn(), exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "秒杀排队失败，请稍后重试");
        }
    }

    private void compensateSeckillStock(String stockKey, String boughtKey, String userIdValue, String quantityValue) {
        try {
            Long result = stringRedisTemplate.execute(
                    COMPENSATE_SCRIPT,
                    List.of(stockKey, boughtKey),
                    userIdValue,
                    quantityValue
            );
            log.warn("Seckill MQ failure compensation finished. stockKey: {}, boughtKey: {}, result: {}",
                    stockKey, boughtKey, result);
        } catch (Exception exception) {
            log.error("Seckill MQ failure compensation failed. stockKey: {}, boughtKey: {}",
                    stockKey, boughtKey, exception);
        }
    }

    private void trackQps(Long skuId) {
        try {
            String key = qpsKey(skuId);
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(5));
        } catch (Exception ignored) {
        }
    }

    private void trackLatency(Long skuId, long startMs) {
        try {
            long elapsed = System.currentTimeMillis() - startMs;
            String key = latencyKey(skuId);
            stringRedisTemplate.opsForList().leftPush(key, String.valueOf(elapsed));
            stringRedisTemplate.opsForList().trim(key, 0, LATENCY_SAMPLE_SIZE - 1);
        } catch (Exception ignored) {
        }
    }

    private void trackMqSent(Long skuId) {
        try {
            stringRedisTemplate.opsForValue().increment(mqSentKey(skuId));
        } catch (Exception ignored) {
        }
    }

    @Override
    public Map<String, Object> getStressMetrics(Long skuId) {
        Map<String, Object> metrics = new HashMap<>();

        String stockStr = stringRedisTemplate.opsForValue().get(stockKey(skuId));
        metrics.put("stock", stockStr != null ? Integer.parseInt(stockStr) : 0);

        String qpsStr = stringRedisTemplate.opsForValue().get(qpsKey(skuId));
        metrics.put("qps", qpsStr != null ? Integer.parseInt(qpsStr) : 0);

        List<String> latencies = stringRedisTemplate.opsForList()
                .range(latencyKey(skuId), 0, -1);
        if (latencies != null && !latencies.isEmpty()) {
            List<Long> sorted = latencies.stream()
                    .map(Long::parseLong)
                    .sorted()
                    .toList();
            int mid = sorted.size() / 2;
            metrics.put("latency", sorted.get(mid));
        } else {
            metrics.put("latency", 0);
        }

        String mqSentStr = stringRedisTemplate.opsForValue().get(mqSentKey(skuId));
        metrics.put("mqQueue", mqSentStr != null ? Integer.parseInt(mqSentStr) : 0);

        return metrics;
    }

    @Override
    public void resetStressMetrics(Long skuId) {
        try {
            stringRedisTemplate.delete(qpsKey(skuId));
            stringRedisTemplate.delete(latencyKey(skuId));
            stringRedisTemplate.delete(mqSentKey(skuId));
        } catch (Exception ignored) {
        }
    }

    private static String generateSeckillOrderSn() {
        return "SEC_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static DefaultRedisScript<Long> createScript(String scriptText) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText(scriptText);
        return redisScript;
    }
}
