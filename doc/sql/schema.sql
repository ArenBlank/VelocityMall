-- VelocityMall 极速商城核心业务库表结构
-- Phase 3 采用单库 velocity_mall + pms_/oms_ 表前缀逻辑隔离策略

CREATE DATABASE IF NOT EXISTS `velocity_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE `velocity_mall`;

DROP TABLE IF EXISTS `pms_category`;
CREATE TABLE `pms_category`
(
    `id`          BIGINT      NOT NULL COMMENT '分类ID',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示一级分类',
    `name`        VARCHAR(64) NOT NULL COMMENT '分类名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `icon`        VARCHAR(255)         DEFAULT NULL COMMENT '分类图标地址',
    `level`       TINYINT     NOT NULL DEFAULT 1 COMMENT '分类层级',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商品分类表';

DROP TABLE IF EXISTS `pms_spu`;
CREATE TABLE `pms_spu`
(
    `id`             BIGINT       NOT NULL COMMENT 'SPU ID',
    `category_id`    BIGINT       NOT NULL COMMENT '分类ID',
    `name`           VARCHAR(128) NOT NULL COMMENT '商品名称',
    `description`    TEXT                  DEFAULT NULL COMMENT '商品描述',
    `publish_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '上架状态，0-下架，1-上架',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_publish_status` (`publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商品SPU表';

DROP TABLE IF EXISTS `pms_sku`;
CREATE TABLE `pms_sku`
(
    `id`          BIGINT         NOT NULL COMMENT 'SKU ID',
    `spu_id`      BIGINT         NOT NULL COMMENT 'SPU ID',
    `sku_name`    VARCHAR(128)   NOT NULL COMMENT 'SKU名称',
    `sku_code`    VARCHAR(64)    NOT NULL COMMENT 'SKU编码',
    `price`       DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    `stock`       INT            NOT NULL DEFAULT 0 COMMENT '实际物理库存',
    `lock_stock`  INT            NOT NULL DEFAULT 0 COMMENT '锁定库存，用于下单未支付',
    `cover_img`   VARCHAR(255)            DEFAULT NULL COMMENT 'SKU封面图',
    `version`     INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商品SKU表';

DROP TABLE IF EXISTS `oms_order`;
CREATE TABLE `oms_order`
(
    `id`           BIGINT         NOT NULL COMMENT '订单ID',
    `user_id`      BIGINT         NOT NULL COMMENT '用户ID',
    `order_sn`     VARCHAR(64)    NOT NULL COMMENT '唯一订单号',
    `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `pay_amount`   DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    `pay_type`     TINYINT                 DEFAULT NULL COMMENT '支付方式，1-支付宝，2-微信，3-余额',
    `status`       TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态，0-待付款，1-已付款，2-已发货，3-已完成，4-已关闭/超时取消',
    `remark`       VARCHAR(500)            DEFAULT NULL COMMENT '订单备注',
    `version`      INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单主表';

DROP TABLE IF EXISTS `oms_order_item`;
CREATE TABLE `oms_order_item`
(
    `id`           BIGINT         NOT NULL COMMENT '订单明细ID',
    `order_id`     BIGINT         NOT NULL COMMENT '订单ID',
    `order_sn`     VARCHAR(64)    NOT NULL COMMENT '唯一订单号',
    `spu_id`       BIGINT         NOT NULL COMMENT 'SPU ID',
    `sku_id`       BIGINT         NOT NULL COMMENT 'SKU ID',
    `sku_name`     VARCHAR(128)   NOT NULL COMMENT 'SKU名称快照',
    `sku_price`    DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT 'SKU成交单价快照',
    `sku_quantity` INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_sn` (`order_sn`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单明细表';
