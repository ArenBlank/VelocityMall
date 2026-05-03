package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.product.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 SKU Mapper。
 */
@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 条件锁定库存，避免普通下单场景发生超卖。
     *
     * @param skuId SKU ID
     * @param quantity 锁定数量
     * @return 影响行数
     */
    @Update("""
            UPDATE pms_sku
            SET lock_stock = lock_stock + #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND stock - lock_stock >= #{quantity}
              AND is_deleted = 0
            """)
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 条件释放锁定库存，避免锁定库存被扣成负数。
     *
     * @param skuId SKU ID
     * @param quantity 释放数量
     * @return 影响行数
     */
    @Update("""
            UPDATE pms_sku
            SET lock_stock = lock_stock - #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND lock_stock >= #{quantity}
              AND is_deleted = 0
            """)
    int unlockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
