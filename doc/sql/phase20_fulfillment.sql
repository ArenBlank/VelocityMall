USE `velocity_mall`;

ALTER TABLE `oms_order`
    ADD COLUMN `delivery_company` VARCHAR(64)  DEFAULT NULL COMMENT '物流公司' AFTER `receiver_detail_address`,
    ADD COLUMN `delivery_sn`      VARCHAR(64)  DEFAULT NULL COMMENT '物流单号' AFTER `delivery_company`,
    ADD COLUMN `delivery_time`    DATETIME     DEFAULT NULL COMMENT '发货时间' AFTER `delivery_sn`,
    ADD COLUMN `receive_time`     DATETIME     DEFAULT NULL COMMENT '确认收货时间' AFTER `delivery_time`;
