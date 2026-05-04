package com.velocitymall.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch stock lock or unlock request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLockDTO {

    private String orderSn;

    private List<OrderItemDTO> items;
}
