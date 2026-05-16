-- VelocityMall Phase 23 联调与 C 端演示测试数据
-- 可重复执行：会重置演示 SKU 的 stock、lock_stock、version 与秒杀活动配置，方便多次验证下单和秒杀链路。

USE `velocity_mall`;

INSERT INTO `pms_category`
(`id`, `parent_id`, `name`, `sort`, `icon`, `level`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (1, 0, '手机数码', 1, NULL, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `sort` = VALUES(`sort`),
    `icon` = VALUES(`icon`),
    `level` = VALUES(`level`),
    `update_time` = NOW(),
    `is_deleted` = 0;

INSERT INTO `pms_spu`
(`id`, `category_id`, `name`, `description`, `publish_status`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (1001, 1, 'Velocity Phone Pro', 'VelocityMall Phase 23 秒杀主推演示手机', 1, 0, NOW(), NOW(), 0),
    (1002, 1, 'Velocity Phone Air', 'VelocityMall 未开始秒杀演示手机，用于展示距离活动开始倒计时与自动切换抢购状态', 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `category_id` = VALUES(`category_id`),
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `publish_status` = VALUES(`publish_status`),
    `version` = 0,
    `update_time` = NOW(),
    `is_deleted` = 0;

INSERT INTO `pms_sku`
(`id`, `spu_id`, `sku_name`, `sku_code`, `price`, `stock`, `lock_stock`, `cover_img`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (2001, 1001, 'Velocity Phone Pro 16GB+512GB 曜石黑', 'VP-PRO-16G-512G-BLACK', 7999.00, 100, 0,
     '/minio/velocity-mall-product/products/default-covers/phone-1.png', 0, NOW(), NOW(), 0),
    (2002, 1002, 'Velocity Phone Air 12GB+256GB 松柏绿', 'VP-AIR-12G-256G-GREEN', 5999.00, 300, 0,
     '/minio/velocity-mall-product/products/seckill-demo/velocity-phone-future-2002.png', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `spu_id` = VALUES(`spu_id`),
    `sku_name` = VALUES(`sku_name`),
    `sku_code` = VALUES(`sku_code`),
    `price` = VALUES(`price`),
    `stock` = VALUES(`stock`),
    `lock_stock` = 0,
    `cover_img` = VALUES(`cover_img`),
    `version` = 0,
    `update_time` = NOW(),
    `is_deleted` = 0;

INSERT INTO `sms_seckill_activity`
(`id`, `sku_id`, `spu_id`, `activity_name`, `seckill_price`, `original_price`, `seckill_stock`,
 `start_time`, `end_time`, `status`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (23001, 2001, 1001, 'Velocity Phone Pro Phase 23 Flash Sale', 4999.00, 7999.00, 100,
     '2026-01-01 10:00:00', '2027-12-31 23:59:59', 1, 0, NOW(), NOW(), 0),
    (23002, 2002, 1002, 'Velocity Phone Air Future Flash Sale Demo', 3999.00, 5999.00, 300,
     '2027-06-01 10:00:00', '2027-06-30 23:59:59', 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `sku_id` = VALUES(`sku_id`),
    `spu_id` = VALUES(`spu_id`),
    `activity_name` = VALUES(`activity_name`),
    `seckill_price` = VALUES(`seckill_price`),
    `original_price` = VALUES(`original_price`),
    `seckill_stock` = VALUES(`seckill_stock`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `status` = VALUES(`status`),
    `version` = 0,
    `update_time` = NOW(),
    `is_deleted` = 0;
