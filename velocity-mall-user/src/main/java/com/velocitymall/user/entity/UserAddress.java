package com.velocitymall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
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
@TableName("ums_user_address")
public class UserAddress extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("region")
    private String region;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("is_default")
    private Integer isDefault;
}
