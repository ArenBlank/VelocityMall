package com.velocitymall.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.VersionedEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 订单主表实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order")
public class Order extends VersionedEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("order_sn")
    private String orderSn;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("pay_type")
    private Integer payType;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;
}
