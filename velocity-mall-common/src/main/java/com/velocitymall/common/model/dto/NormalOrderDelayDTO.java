package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Normal order delay close message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NormalOrderDelayDTO extends BaseMessageDTO {

    private String orderSn;

    private List<OrderItemDTO> items;
}
