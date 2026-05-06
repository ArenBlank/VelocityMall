package com.velocitymall.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.common.context.MqTraceContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.mapper.OrderMapper;
import com.velocitymall.order.model.dto.OrderMessageDTO;
import com.velocitymall.order.model.dto.UnlockStockDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单延时关单消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "velocity-mall-order-delay-topic", consumerGroup = "order-delay-consumer-group")
public class OrderDelayConsumer implements RocketMQListener<OrderMessageDTO> {

    private static final Integer ORDER_STATUS_WAIT_PAY = 0;

    private static final Integer ORDER_STATUS_CLOSED = 4;

    private final OrderMapper orderMapper;

    private final ProductFeignClient productFeignClient;

    @Override
    public void onMessage(OrderMessageDTO message) {
        MqTraceContext.runWithTrace(message, () -> handleMessage(message));
    }

    private void handleMessage(OrderMessageDTO message) {
        log.info("收到订单延时关单消息, orderSn: {}", message.getOrderSn());

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, message.getOrderSn())
        );
        if (order == null || !isCloseOrWaitPay(order.getStatus())) {
            log.info("订单不存在或状态非待付款/已关闭，安全丢弃消息. orderSn: {}", message.getOrderSn());
            return;
        }

        if (ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            int updatedRows = orderMapper.closeOrder(message.getOrderSn());
            if (updatedRows == 0) {
                log.warn("并发关单拦截：订单状态已发生变更，放弃释放库存. orderSn: {}", message.getOrderSn());
                return;
            }
        }

        UnlockStockDTO unlockDTO = new UnlockStockDTO();
        unlockDTO.setOrderSn(message.getOrderSn());
        unlockDTO.setSkuId(message.getSkuId());
        unlockDTO.setQuantity(message.getQuantity());

        Result<Void> result = productFeignClient.unlockStock(unlockDTO);
        if (result == null || !ResultCode.SUCCESS.getCode().equals(result.getCode())) {
            String messageText = result == null ? "商品服务无响应" : result.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, "释放库存失败，触发 MQ 重试: " + messageText);
        }

        log.info("订单关单及库存释放完成. orderSn: {}", message.getOrderSn());
    }

    private boolean isCloseOrWaitPay(Integer status) {
        return ORDER_STATUS_WAIT_PAY.equals(status) || ORDER_STATUS_CLOSED.equals(status);
    }
}
