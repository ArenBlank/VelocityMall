package com.velocitymall.common.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order detail view object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {

    private String orderSn;

    private BigDecimal totalAmount;

    private Integer payType;

    private Integer status;

    private Integer orderType;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}
