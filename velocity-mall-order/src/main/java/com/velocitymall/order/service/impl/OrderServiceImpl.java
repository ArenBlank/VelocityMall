package com.velocitymall.order.service.impl;

import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.entity.Order;
import com.velocitymall.order.entity.OrderItem;
import com.velocitymall.order.mapper.OrderItemMapper;
import com.velocitymall.order.mapper.OrderMapper;
import com.velocitymall.order.model.dto.LockStockDTO;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.vo.OrderVO;
import com.velocitymall.order.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_STATUS_WAIT_PAY = 0;

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;

    private static final long DEFAULT_SPU_ID = 0L;

    private static final String DEFAULT_SKU_NAME = "待同步SKU信息";

    private static final DateTimeFormatter ORDER_SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ProductFeignClient productFeignClient;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitOrder(SubmitOrderDTO dto) {
        // TODO: 接入 Redis 实现 SET NX 短幂等键防重提交
        String orderSn = generateOrderSn();

        Result<Void> lockStockResult = productFeignClient.lockStock(new LockStockDTO(dto.getSkuId(), dto.getQuantity()));
        if (lockStockResult == null || !ResultCode.SUCCESS.getCode().equals(lockStockResult.getCode())) {
            String message = lockStockResult == null || !StringUtils.hasText(lockStockResult.getMessage())
                    ? "锁定库存失败"
                    : lockStockResult.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, message);
        }

        Order order = Order.builder()
                .orderSn(orderSn)
                .userId(dto.getUserId())
                .totalAmount(DEFAULT_AMOUNT)
                .payAmount(DEFAULT_AMOUNT)
                .status(ORDER_STATUS_WAIT_PAY)
                .build();
        orderMapper.insert(order);

        OrderItem orderItem = OrderItem.builder()
                .orderId(order.getId())
                .orderSn(orderSn)
                .spuId(DEFAULT_SPU_ID)
                .skuId(dto.getSkuId())
                .skuName(DEFAULT_SKU_NAME)
                .skuPrice(DEFAULT_AMOUNT)
                .skuQuantity(dto.getQuantity())
                .build();
        orderItemMapper.insert(orderItem);

        return OrderVO.builder()
                .orderId(order.getId())
                .orderSn(orderSn)
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .status(order.getStatus())
                .build();
    }

    private String generateOrderSn() {
        int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "VM" + LocalDateTime.now().format(ORDER_SN_TIME_FORMATTER) + randomSuffix;
    }
}
