-- VelocityMall Phase 14 category tree cache migration
USE `velocity_mall`;

SET @category_status_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pms_category'
      AND COLUMN_NAME = 'status'
);
SET @category_status_column_sql = IF(
        @category_status_column_exists = 0,
        'ALTER TABLE `pms_category` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''启用状态，0-禁用，1-启用'' AFTER `level`',
        'SELECT 1'
    );
PREPARE category_status_column_stmt FROM @category_status_column_sql;
EXECUTE category_status_column_stmt;
DEALLOCATE PREPARE category_status_column_stmt;

SET @category_status_sort_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pms_category'
      AND INDEX_NAME = 'idx_status_sort'
);
SET @category_status_sort_index_sql = IF(
        @category_status_sort_index_exists = 0,
        'CREATE INDEX `idx_status_sort` ON `pms_category` (`status`, `sort`)',
        'SELECT 1'
    );
PREPARE category_status_sort_index_stmt FROM @category_status_sort_index_sql;
EXECUTE category_status_sort_index_stmt;
DEALLOCATE PREPARE category_status_sort_index_stmt;
