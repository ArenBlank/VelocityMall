-- VelocityMall Phase 3 联调测试数据
-- 可重复执行：会重置测试 SKU 的 stock、lock_stock 与 version，方便多次验证下单链路。

USE `velocity_mall`;

INSERT INTO `pms_category`
(`id`, `parent_id`, `name`, `sort`, `icon`, `level`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (1, 0, '手机数码', 1, 'https://static.velocitymall.local/category/digital.png', 1, NOW(), NOW(), 0)
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
    (1001, 1, 'Velocity Phone Pro', 'VelocityMall Phase 3 联调测试手机', 1, 0, NOW(), NOW(), 0)
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
     'https://static.velocitymall.local/sku/velocity-phone-pro-black.png', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    `spu_id` = VALUES(`spu_id`),
    `sku_name` = VALUES(`sku_name`),
    `sku_code` = VALUES(`sku_code`),
    `price` = VALUES(`price`),
    `stock` = 100,
    `lock_stock` = 0,
    `cover_img` = VALUES(`cover_img`),
    `version` = 0,
    `update_time` = NOW(),
    `is_deleted` = 0;
