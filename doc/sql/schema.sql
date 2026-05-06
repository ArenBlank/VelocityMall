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
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '启用状态，0-禁用，1-启用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status_sort` (`status`, `sort`)
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
    `sale_count`  INT            NOT NULL DEFAULT 0 COMMENT '销量',
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
    `pay_time`     DATETIME                DEFAULT NULL COMMENT '支付时间',
    `order_type`   TINYINT        NOT NULL DEFAULT 0 COMMENT '订单类型，0-普通订单，1-秒杀订单',
    `status`       TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态，0-待付款，1-已付款，2-已发货，3-已完成，4-已关闭/超时取消，5-已退款',
    `remark`                  VARCHAR(500)            DEFAULT NULL COMMENT '订单备注',
    `receiver_name`           VARCHAR(32)             DEFAULT NULL COMMENT '收货人姓名快照',
    `receiver_phone`          VARCHAR(20)             DEFAULT NULL COMMENT '收货人手机号快照',
    `receiver_province`       VARCHAR(32)             DEFAULT NULL COMMENT '收货省份快照',
    `receiver_city`           VARCHAR(32)             DEFAULT NULL COMMENT '收货城市快照',
    `receiver_region`         VARCHAR(32)             DEFAULT NULL COMMENT '收货区县快照',
    `receiver_detail_address` VARCHAR(255)            DEFAULT NULL COMMENT '收货详细地址快照',
    `delivery_company`        VARCHAR(64)             DEFAULT NULL COMMENT '物流公司',
    `delivery_sn`             VARCHAR(64)             DEFAULT NULL COMMENT '物流单号',
    `delivery_time`           DATETIME                DEFAULT NULL COMMENT '发货时间',
    `receive_time`            DATETIME                DEFAULT NULL COMMENT '确认收货时间',
    `version`                 INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
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
    `sku_pic`      VARCHAR(255)            DEFAULT NULL COMMENT 'SKU图片快照',
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

DROP TABLE IF EXISTS `pms_stock_lock_log`;
CREATE TABLE `pms_stock_lock_log`
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

DROP TABLE IF EXISTS `mq_consume_log`;
CREATE TABLE `mq_consume_log`
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

-- ============================================================
-- Phase 15: 营销域优惠券系统
-- ============================================================

DROP TABLE IF EXISTS `sms_coupon`;
CREATE TABLE `sms_coupon`
(
    `id`             BIGINT         NOT NULL COMMENT '优惠券ID',
    `name`           VARCHAR(128)   NOT NULL COMMENT '优惠券名称',
    `amount`         DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券面额',
    `min_point`      DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '最低消费门槛',
    `stock`          INT            NOT NULL DEFAULT 0 COMMENT '发放库存',
    `limit_per_user` INT            NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `start_time`     DATETIME       NOT NULL COMMENT '可领取开始时间',
    `end_time`       DATETIME       NOT NULL COMMENT '可领取结束时间',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '优惠券状态，0-禁用，1-启用',
    `version`        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券定义表';

DROP TABLE IF EXISTS `sms_coupon_history`;
CREATE TABLE `sms_coupon_history`
(
    `id`          BIGINT   NOT NULL COMMENT '领取记录ID',
    `coupon_id`   BIGINT   NOT NULL COMMENT '优惠券ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `claim_time`  DATETIME NOT NULL COMMENT '领取时间',
    `use_status`  TINYINT  NOT NULL DEFAULT 0 COMMENT '使用状态，0-未使用，1-已使用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_user` (`coupon_id`, `user_id`),
    KEY `idx_user_status` (`user_id`, `use_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券领取记录表';

-- ============================================================
-- Phase 16: 用户评价与互动体系
-- ============================================================

DROP TABLE IF EXISTS `oms_product_review`;
CREATE TABLE `oms_product_review`
(
    `id`           BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`      BIGINT       NOT NULL COMMENT '评价用户ID',
    `order_sn`     VARCHAR(64)  NOT NULL COMMENT '订单编号',
    `sku_id`       BIGINT       NOT NULL COMMENT 'SKU ID',
    `spu_id`       BIGINT       NOT NULL COMMENT 'SPU ID',
    `rating`       TINYINT      NOT NULL COMMENT '评分，1-5星',
    `content`      VARCHAR(1000) NOT NULL COMMENT '评价内容',
    `has_pictures` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否有图，0-无图，1-有图',
    `like_count`    INT         NOT NULL DEFAULT 0 COMMENT '点赞数冗余',
    `dislike_count` INT         NOT NULL DEFAULT 0 COMMENT '点踩数冗余',
    `reply_count`   INT         NOT NULL DEFAULT 0 COMMENT '回复数冗余',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    UNIQUE KEY `uk_user_order_sku` (`user_id`, `order_sn`, `sku_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商品评价表';

DROP TABLE IF EXISTS `oms_review_interaction`;
CREATE TABLE `oms_review_interaction`
(
    `id`               BIGINT   NOT NULL COMMENT '主键ID',
    `review_id`        BIGINT   NOT NULL COMMENT '评价ID',
    `user_id`          BIGINT   NOT NULL COMMENT '互动用户ID',
    `interaction_type` TINYINT  NOT NULL COMMENT '互动类型，1-点赞，2-点踩',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_user` (`review_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评价互动表';

-- ============================================================
-- Phase 18: 用户系统
-- ============================================================

DROP TABLE IF EXISTS `ums_user`;
CREATE TABLE `ums_user`
(
    `id`          BIGINT       NOT NULL COMMENT '用户ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt密文密码',
    `nickname`    VARCHAR(64)           DEFAULT NULL COMMENT '用户昵称',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态，0-禁用，1-启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

-- ============================================================
-- Phase 19: 收货地址与订单地址快照
-- ============================================================

DROP TABLE IF EXISTS `ums_user_address`;
CREATE TABLE `ums_user_address`
(
    `id`              BIGINT       NOT NULL COMMENT '地址ID',
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `receiver_name`   VARCHAR(32)  NOT NULL COMMENT '收货人姓名',
    `receiver_phone`  VARCHAR(20)  NOT NULL COMMENT '收货人手机号',
    `province`        VARCHAR(32)  NOT NULL COMMENT '省份',
    `city`            VARCHAR(32)  NOT NULL COMMENT '城市',
    `region`          VARCHAR(32)  NOT NULL COMMENT '区/县',
    `detail_address`  VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认地址，0-否，1-是',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户收货地址表';

-- ============================================================
-- Phase 21: 管理后台
-- ============================================================

DROP TABLE IF EXISTS `ums_admin`;
CREATE TABLE `ums_admin`
(
    `id`          BIGINT       NOT NULL COMMENT '管理员ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '管理员用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt密文密码',
    `real_name`   VARCHAR(64)           DEFAULT NULL COMMENT '真实姓名',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态，0-禁用，1-启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='管理员表';
