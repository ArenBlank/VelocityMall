package com.velocitymall.admin.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSkuVO {

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String skuCode;

    private BigDecimal price;

    private Integer stock;

    private Integer lockStock;

    private Integer availableStock;

    private Integer saleCount;

    private String coverImg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
