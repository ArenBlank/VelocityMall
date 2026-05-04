package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment success event payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessDTO {

    private String orderSn;

    private Long userId;

    private Integer orderType;

    private List<OrderItemDTO> items;
}
