USE `velocity_mall`;

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
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户收货地址表';

ALTER TABLE `oms_order`
    ADD COLUMN `receiver_name`           VARCHAR(32)  DEFAULT NULL COMMENT '收货人姓名快照' AFTER `remark`,
    ADD COLUMN `receiver_phone`          VARCHAR(20)  DEFAULT NULL COMMENT '收货人手机号快照' AFTER `receiver_name`,
    ADD COLUMN `receiver_province`       VARCHAR(32)  DEFAULT NULL COMMENT '收货省份快照' AFTER `receiver_phone`,
    ADD COLUMN `receiver_city`           VARCHAR(32)  DEFAULT NULL COMMENT '收货城市快照' AFTER `receiver_province`,
    ADD COLUMN `receiver_region`         VARCHAR(32)  DEFAULT NULL COMMENT '收货区县快照' AFTER `receiver_city`,
    ADD COLUMN `receiver_detail_address` VARCHAR(255) DEFAULT NULL COMMENT '收货详细地址快照' AFTER `receiver_region`;
