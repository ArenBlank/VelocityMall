-- VelocityMall Phase 12 order management and refund flow migration
USE `velocity_mall`;

ALTER TABLE `oms_order`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态，0-待付款，1-已付款，2-已发货，3-已完成，4-已关闭/超时取消，5-已退款';

ALTER TABLE `oms_order_item`
    ADD COLUMN `sku_pic` VARCHAR(255) DEFAULT NULL COMMENT 'SKU图片快照' AFTER `sku_name`;
