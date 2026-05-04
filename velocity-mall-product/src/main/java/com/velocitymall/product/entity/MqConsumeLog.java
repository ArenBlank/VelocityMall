package com.velocitymall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * MQ consumer idempotency log.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mq_consume_log")
public class MqConsumeLog extends BaseEntity {

    @TableField("topic")
    private String topic;

    @TableField("consumer_group")
    private String consumerGroup;

    @TableField("order_sn")
    private String orderSn;
}
