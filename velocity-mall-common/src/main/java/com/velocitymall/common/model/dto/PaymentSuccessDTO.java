package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Payment success event payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentSuccessDTO extends BaseMessageDTO {

    private String orderSn;

    private Long userId;

    private Integer orderType;

    private List<OrderItemDTO> items;
}
