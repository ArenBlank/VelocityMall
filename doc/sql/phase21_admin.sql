USE `velocity_mall`;

DROP TABLE IF EXISTS `ums_admin`;
CREATE TABLE `ums_admin`
(
    `id`          BIGINT      NOT NULL COMMENT '管理员ID',
    `username`    VARCHAR(64) NOT NULL COMMENT '管理员用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt密文密码',
    `real_name`   VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '账号状态，0-禁用，1-启用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '管理员表';

-- 种子管理员: username=admin, password=123456
INSERT INTO `ums_admin` (`id`, `username`, `password`, `real_name`, `status`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 'admin', '$2a$10$j/jaxAC8fXLIrZH361eoye3cvkCoPDPcDcCTcDJ7uphwG8h0.L0bS', '系统管理员', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
