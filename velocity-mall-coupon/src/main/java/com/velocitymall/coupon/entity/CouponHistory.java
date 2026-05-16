package com.velocitymall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 用户优惠券领取流水实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sms_coupon_history")
public class CouponHistory extends BaseEntity {

    @TableField("coupon_id")
    private Long couponId;

    @TableField("user_id")
    private Long userId;

    @TableField("claim_time")
    private LocalDateTime claimTime;

    @TableField("use_status")
    private Integer useStatus;

    @TableField("use_time")
    private LocalDateTime useTime;

    @TableField("order_sn")
    private String orderSn;
}
