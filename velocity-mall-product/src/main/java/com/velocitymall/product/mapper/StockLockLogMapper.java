package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.product.entity.StockLockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Stock lock lifecycle log mapper.
 */
@Mapper
public interface StockLockLogMapper extends BaseMapper<StockLockLog> {

    /**
     * Mark a stock lock as released.
     *
     * @param orderSn order number
     * @param skuId SKU ID
     * @return affected rows
     */
    @Update("""
            UPDATE pms_stock_lock_log
            SET status = 1
            WHERE order_sn = #{orderSn}
              AND sku_id = #{skuId}
              AND status = 0
              AND is_deleted = 0
            """)
    int markReleased(@Param("orderSn") String orderSn, @Param("skuId") Long skuId);

    /**
     * Mark a stock lock as physically deducted.
     *
     * @param orderSn order number
     * @param skuId SKU ID
     * @return affected rows
     */
    @Update("""
            UPDATE pms_stock_lock_log
            SET status = 2
            WHERE order_sn = #{orderSn}
              AND sku_id = #{skuId}
              AND status = 0
              AND is_deleted = 0
            """)
    int markDeducted(@Param("orderSn") String orderSn, @Param("skuId") Long skuId);
}
