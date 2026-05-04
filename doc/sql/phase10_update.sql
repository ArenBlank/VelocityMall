-- VelocityMall Phase 10 payment success and physical stock deduction migration
USE `velocity_mall`;

ALTER TABLE `oms_order`
    ADD COLUMN `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间' AFTER `pay_type`;

ALTER TABLE `pms_sku`
    ADD COLUMN `sale_count` INT NOT NULL DEFAULT 0 COMMENT '销量' AFTER `lock_stock`;

CREATE TABLE IF NOT EXISTS `mq_consume_log`
(
    `id`             BIGINT       NOT NULL COMMENT '消费记录ID',
    `topic`          VARCHAR(128) NOT NULL COMMENT '消息主题',
    `consumer_group` VARCHAR(128) NOT NULL COMMENT '消费者分组',
    `order_sn`       VARCHAR(64)  NOT NULL COMMENT '订单号',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_group_order` (`topic`, `consumer_group`, `order_sn`),
    KEY `idx_order_sn` (`order_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='MQ消费幂等记录表';
