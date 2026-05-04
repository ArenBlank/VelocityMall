package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seckill stock rollback message payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillRollbackDTO {

    private Long skuId;

    private Long userId;
}
