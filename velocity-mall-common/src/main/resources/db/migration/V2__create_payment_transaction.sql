CREATE TABLE IF NOT EXISTS `oms_payment_transaction`
(
    `id`               BIGINT         NOT NULL COMMENT '支付流水ID',
    `order_sn`         VARCHAR(64)    NOT NULL COMMENT '订单号',
    `user_id`          BIGINT         NOT NULL COMMENT '用户ID',
    `transaction_type` TINYINT        NOT NULL COMMENT '流水类型，1-支付，2-退款',
    `pay_type`         TINYINT                 DEFAULT NULL COMMENT '支付方式，1-支付宝，2-微信，3-余额',
    `amount`           DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '交易金额',
    `request_no`       VARCHAR(96)    NOT NULL COMMENT '商户请求号',
    `trade_no`         VARCHAR(96)             DEFAULT NULL COMMENT '模拟第三方交易号',
    `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '流水状态，0-处理中，1-成功，2-失败',
    `callback_payload` TEXT                    DEFAULT NULL COMMENT '回调原始报文',
    `fail_reason`      VARCHAR(255)            DEFAULT NULL COMMENT '失败原因',
    `success_time`     DATETIME                DEFAULT NULL COMMENT '成功时间',
    `version`          INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_no` (`request_no`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    UNIQUE KEY `uk_order_transaction_type` (`order_sn`, `transaction_type`),
    KEY `idx_order_sn` (`order_sn`),
    KEY `idx_user_type_status` (`user_id`, `transaction_type`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='订单支付退款流水表';
