package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normal order delay close message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalOrderDelayDTO {

    private String orderSn;

    private List<OrderItemDTO> items;
}
