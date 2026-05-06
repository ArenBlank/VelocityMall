CREATE DATABASE IF NOT EXISTS `velocity_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE `velocity_mall`;

CREATE TABLE IF NOT EXISTS `oms_product_review` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `spu_id` BIGINT NOT NULL COMMENT 'SPU ID',
    `rating` TINYINT NOT NULL COMMENT '评分，1-5星',
    `content` VARCHAR(1000) NOT NULL COMMENT '评价内容',
    `has_pictures` TINYINT NOT NULL DEFAULT 0 COMMENT '是否有图，0-无图，1-有图',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数冗余',
    `dislike_count` INT NOT NULL DEFAULT 0 COMMENT '点踩数冗余',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数冗余',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    UNIQUE KEY `uk_user_order_sku` (`user_id`, `order_sn`, `sku_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品评价表';

CREATE TABLE IF NOT EXISTS `oms_review_interaction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '互动用户ID',
    `interaction_type` TINYINT NOT NULL COMMENT '互动类型，1-点赞，2-点踩',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_user` (`review_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '评价互动表';
