CREATE TABLE IF NOT EXISTS `ums_admin_role`
(
    `id`          BIGINT       NOT NULL COMMENT '角色ID',
    `role_code`   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '角色说明',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态，0-禁用，1-启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员角色表';

CREATE TABLE IF NOT EXISTS `ums_admin_permission`
(
    `id`              BIGINT       NOT NULL COMMENT '权限ID',
    `permission_code` VARCHAR(96)  NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `resource`        VARCHAR(64)  NOT NULL COMMENT '资源域',
    `action`          VARCHAR(64)  NOT NULL COMMENT '动作',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '权限说明',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态，0-禁用，1-启用',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_resource_action` (`resource`, `action`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员权限表';

CREATE TABLE IF NOT EXISTS `ums_admin_role_permission`
(
    `id`            BIGINT   NOT NULL COMMENT '主键ID',
    `role_id`       BIGINT   NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT   NOT NULL COMMENT '权限ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员角色权限关联表';

CREATE TABLE IF NOT EXISTS `ums_admin_role_relation`
(
    `id`          BIGINT   NOT NULL COMMENT '主键ID',
    `admin_id`    BIGINT   NOT NULL COMMENT '管理员ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员角色关联表';
