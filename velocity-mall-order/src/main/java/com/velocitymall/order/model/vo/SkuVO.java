package com.velocitymall.order.model.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单服务侧 SKU 快照视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuVO {

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String skuCode;

    private BigDecimal price;

    private Integer availableStock;

    private String coverImg;
}
