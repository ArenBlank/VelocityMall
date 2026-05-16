package com.velocitymall.common.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flash-sale order generation result for buyer polling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultVO {

    private String state;

    private String orderSn;

    private Integer orderStatus;

    private String message;
}
