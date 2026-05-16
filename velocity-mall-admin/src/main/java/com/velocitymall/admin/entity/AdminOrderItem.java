package com.velocitymall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_item")
public class AdminOrderItem extends BaseEntity {

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

    @TableField("sku_pic")
    private String skuPic;

    @TableField("sku_price")
    private BigDecimal skuPrice;

    @TableField("sku_quantity")
    private Integer skuQuantity;
}
