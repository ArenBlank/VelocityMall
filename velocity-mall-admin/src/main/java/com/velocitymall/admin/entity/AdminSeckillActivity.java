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
@TableName("sms_seckill_activity")
public class AdminSeckillActivity extends VersionedEntity {

    @TableField("sku_id")
    private Long skuId;

    @TableField("spu_id")
    private Long spuId;

    @TableField("activity_name")
    private String activityName;

    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("seckill_stock")
    private Integer seckillStock;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;
}
