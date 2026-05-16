package com.velocitymall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.VersionedEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@TableName("oms_order")
public class AdminOrder extends VersionedEntity {

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

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("order_type")
    private Integer orderType;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_province")
    private String receiverProvince;

    @TableField("receiver_city")
    private String receiverCity;

    @TableField("receiver_region")
    private String receiverRegion;

    @TableField("receiver_detail_address")
    private String receiverDetailAddress;

    @TableField("delivery_company")
    private String deliveryCompany;

    @TableField("delivery_sn")
    private String deliverySn;

    @TableField("delivery_time")
    private LocalDateTime deliveryTime;

    @TableField("receive_time")
    private LocalDateTime receiveTime;
}
