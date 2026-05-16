package com.velocitymall.admin.model.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminOrderItemVO {

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String skuPic;

    private BigDecimal skuPrice;

    private Integer quantity;
}
