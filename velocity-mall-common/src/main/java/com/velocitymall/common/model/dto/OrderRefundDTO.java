package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order refund stock rollback message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundDTO {

    private String orderSn;

    private Integer orderType;

    private List<OrderItemDTO> items;
}
