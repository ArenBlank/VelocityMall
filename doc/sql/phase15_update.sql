CREATE DATABASE IF NOT EXISTS `velocity_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE `velocity_mall`;

CREATE TABLE IF NOT EXISTS `sms_coupon` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `name` VARCHAR(128) NOT NULL COMMENT '优惠券名称',
    `amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '抵扣金额',
    `min_point` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '优惠券剩余库存',
    `limit_per_user` INT NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    `start_time` DATETIME NOT NULL COMMENT '领取开始时间',
    `end_time` DATETIME NOT NULL COMMENT '领取结束时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态，0-禁用，1-启用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`),
    KEY `idx_stock` (`stock`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '营销优惠券表';

CREATE TABLE IF NOT EXISTS `sms_coupon_history` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `user_id` BIGINT NOT NULL COMMENT '领取用户ID',
    `claim_time` DATETIME NOT NULL COMMENT '领取时间',
    `use_status` TINYINT NOT NULL DEFAULT 0 COMMENT '使用状态，0-未使用，1-已使用，2-已过期',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_user` (`coupon_id`, `user_id`),
    KEY `idx_user_status` (`user_id`, `use_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户优惠券领取流水表';
