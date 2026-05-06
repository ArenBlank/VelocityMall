USE `velocity_mall`;

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
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表';
