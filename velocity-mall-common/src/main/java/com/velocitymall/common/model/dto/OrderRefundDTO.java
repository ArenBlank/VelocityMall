package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Order refund stock rollback message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderRefundDTO extends BaseMessageDTO {

    private String orderSn;

    private Integer orderType;

    private List<OrderItemDTO> items;
}
