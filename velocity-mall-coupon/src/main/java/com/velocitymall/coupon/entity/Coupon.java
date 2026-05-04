package com.velocitymall.coupon.entity;

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

/**
 * 营销优惠券实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sms_coupon")
public class Coupon extends VersionedEntity {

    @TableField("name")
    private String name;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("min_point")
    private BigDecimal minPoint;

    @TableField("stock")
    private Integer stock;

    @TableField("limit_per_user")
    private Integer limitPerUser;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;
}
