package com.velocitymall.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.NormalOrderDelayDTO;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Consumer for normal order timeout closing and physical locked stock release.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "normal-order-delay-topic", consumerGroup = "normal-order-delay-consumer-group")
public class NormalOrderDelayConsumer implements RocketMQListener<NormalOrderDelayDTO> {

    private static final Integer ORDER_STATUS_WAIT_PAY = 0;

    private static final Integer ORDER_STATUS_CLOSED = 4;

    private final OrderMapper orderMapper;

    private final ProductFeignClient productFeignClient;

    @Override
    public void onMessage(NormalOrderDelayDTO message) {
        validateMessage(message);
        log.info("Received normal order delay close message. orderSn: {}", message.getOrderSn());

        Order order = selectByOrderSn(message.getOrderSn());
        if (order == null) {
            log.warn("Normal order not found, discard delay message. orderSn: {}", message.getOrderSn());
            return;
        }

        Integer status = order.getStatus();
        if (ORDER_STATUS_WAIT_PAY.equals(status)) {
            int updatedRows = orderMapper.closeOrder(message.getOrderSn());
            if (updatedRows == 0) {
                handleConcurrentClose(message);
                return;
            }
            unlockPhysicalStock(message);
            log.info("Normal order closed and locked stock released. orderSn: {}", message.getOrderSn());
            return;
        }

        if (ORDER_STATUS_CLOSED.equals(status)) {
            unlockPhysicalStock(message);
            log.info("Normal order already closed, locked stock release retried. orderSn: {}", message.getOrderSn());
            return;
        }

        log.info("Normal order status is not wait-pay or closed, discard delay message. orderSn: {}, status: {}",
                message.getOrderSn(), status);
    }

    private void validateMessage(NormalOrderDelayDTO message) {
        if (message == null || !StringUtils.hasText(message.getOrderSn()) || CollectionUtils.isEmpty(message.getItems())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "普通订单延时关单消息非法");
        }
    }

    private void handleConcurrentClose(NormalOrderDelayDTO message) {
        Order latestOrder = selectByOrderSn(message.getOrderSn());
        if (latestOrder == null) {
            log.warn("Normal order disappeared during close retry. orderSn: {}", message.getOrderSn());
            return;
        }
        if (ORDER_STATUS_CLOSED.equals(latestOrder.getStatus())) {
            unlockPhysicalStock(message);
            log.info("Normal order was closed by another consumer, locked stock release retried. orderSn: {}",
                    message.getOrderSn());
            return;
        }
        if (ORDER_STATUS_WAIT_PAY.equals(latestOrder.getStatus())) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "普通订单关单影响行数为 0，但订单仍为待支付");
        }
        log.info("Normal order status changed by concurrent flow, discard unlock. orderSn: {}, status: {}",
                message.getOrderSn(), latestOrder.getStatus());
    }

    private Order selectByOrderSn(String orderSn) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, orderSn)
                .last("LIMIT 1"));
    }

    private void unlockPhysicalStock(NormalOrderDelayDTO message) {
        Result<Void> result = productFeignClient.unlockPhysicalStock(new StockLockDTO(
                message.getOrderSn(),
                message.getItems()
        ));
        if (result == null || !ResultCode.SUCCESS.getCode().equals(result.getCode())) {
            String messageText = result == null || !StringUtils.hasText(result.getMessage())
                    ? "商品服务无响应"
                    : result.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, "普通订单释放锁定库存失败，触发 MQ 重试: " + messageText);
        }
    }
}
