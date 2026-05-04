-- VelocityMall Phase 11 cart checkout and normal order stock lock migration
USE `velocity_mall`;

ALTER TABLE `oms_order`
    ADD COLUMN `order_type` TINYINT NOT NULL DEFAULT 0 COMMENT '订单类型，0-普通订单，1-秒杀订单' AFTER `pay_time`;

CREATE TABLE IF NOT EXISTS `pms_stock_lock_log`
(
    `id`          BIGINT      NOT NULL COMMENT '库存锁定流水ID',
    `order_sn`    VARCHAR(64) NOT NULL COMMENT '订单号',
    `sku_id`      BIGINT      NOT NULL COMMENT 'SKU ID',
    `quantity`    INT         NOT NULL DEFAULT 1 COMMENT '锁定数量',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '锁定状态，0-已锁定，1-已释放，2-已真实扣减',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sku` (`order_sn`, `sku_id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商品库存锁定流水表';
