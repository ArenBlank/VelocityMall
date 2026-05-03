package com.velocitymall.order.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long orderId;

    private String orderSn;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer status;
}
