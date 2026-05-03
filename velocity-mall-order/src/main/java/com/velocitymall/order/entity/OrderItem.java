package com.velocitymall.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 订单明细实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_item")
public class OrderItem extends BaseEntity {

    @TableField("order_id")
    private Long orderId;

    @TableField("order_sn")
    private String orderSn;

    @TableField("spu_id")
    private Long spuId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("sku_name")
    private String skuName;

    @TableField("sku_price")
    private BigDecimal skuPrice;

    @TableField("sku_quantity")
    private Integer skuQuantity;
}
