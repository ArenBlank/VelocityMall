CREATE TABLE IF NOT EXISTS `oms_review_reply`
(
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `review_id`   BIGINT       NOT NULL COMMENT '评价ID',
    `user_id`     BIGINT       NOT NULL COMMENT '回复用户ID',
    `content`     VARCHAR(500) NOT NULL COMMENT '回复内容',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_review_time` (`review_id`, `create_time`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评价回复表';
