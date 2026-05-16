USE `velocity_mall`;

CREATE TABLE IF NOT EXISTS `sms_seckill_activity`
(
    `id`             BIGINT         NOT NULL COMMENT '秒杀活动ID',
    `sku_id`         BIGINT         NOT NULL COMMENT 'SKU ID',
    `spu_id`         BIGINT         NOT NULL COMMENT 'SPU ID',
    `activity_name`  VARCHAR(128)   NOT NULL COMMENT '活动名称',
    `seckill_price`  DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '秒杀价',
    `original_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '原价快照',
    `seckill_stock`  INT            NOT NULL DEFAULT 0 COMMENT '秒杀活动库存',
    `start_time`     DATETIME       NOT NULL COMMENT '活动开始时间',
    `end_time`       DATETIME       NOT NULL COMMENT '活动结束时间',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态，0-禁用，1-启用',
    `version`        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sku_status_time` (`sku_id`, `status`, `start_time`, `end_time`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='秒杀活动表';

INSERT INTO `sms_seckill_activity`
(`id`, `sku_id`, `spu_id`, `activity_name`, `seckill_price`, `original_price`, `seckill_stock`,
 `start_time`, `end_time`, `status`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (23001, 2001, 1001, 'Velocity Phone Pro Phase 23 Flash Sale', 4999.00, 7999.00, 100,
     '2026-01-01 10:00:00', '2027-12-31 23:59:59', 1, 0, NOW(), NOW(), 0),
    (23002, 2002, 1002, 'Velocity Phone Air Future Flash Sale Demo', 3999.00, 5999.00, 300,
     '2027-06-01 10:00:00', '2027-06-30 23:59:59', 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `sku_id` = VALUES(`sku_id`),
    `spu_id` = VALUES(`spu_id`),
    `activity_name` = VALUES(`activity_name`),
    `seckill_price` = VALUES(`seckill_price`),
    `original_price` = VALUES(`original_price`),
    `seckill_stock` = VALUES(`seckill_stock`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `status` = VALUES(`status`),
    `version` = 0,
    `update_time` = NOW(),
    `is_deleted` = 0;
