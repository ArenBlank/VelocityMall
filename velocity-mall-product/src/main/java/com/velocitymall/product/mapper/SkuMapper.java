package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.product.entity.Sku;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 SKU Mapper。
 */
@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * Query SKU search source data and join SPU publish status.
     *
     * @param skuId SKU ID
     * @return search source data
     */
    @Select("""
            SELECT s.id AS skuId,
                   s.sku_name AS skuName,
                   s.cover_img AS skuPic,
                   s.price AS price,
                   s.sale_count AS saleCount,
                   p.publish_status AS status
            FROM pms_sku s
            INNER JOIN pms_spu p ON p.id = s.spu_id AND p.is_deleted = 0
            WHERE s.id = #{skuId}
              AND s.is_deleted = 0
            LIMIT 1
            """)
    ProductSkuSearchDTO selectSearchSourceBySkuId(@Param("skuId") Long skuId);

    /**
     * Count published SKU rows used by search index rebuild.
     *
     * @return published SKU count
     */
    @Select("""
            SELECT COUNT(1)
            FROM pms_sku s
            INNER JOIN pms_spu p ON p.id = s.spu_id AND p.is_deleted = 0
            WHERE s.is_deleted = 0
              AND p.publish_status = 1
            """)
    Long countPublishedSearchSources();

    /**
     * Page query published SKU rows used by search index rebuild.
     *
     * @param offset offset
     * @param size page size
     * @return published SKU search source rows
     */
    @Select("""
            SELECT s.id AS skuId,
                   s.sku_name AS skuName,
                   s.cover_img AS skuPic,
                   s.price AS price,
                   s.sale_count AS saleCount,
                   p.publish_status AS status
            FROM pms_sku s
            INNER JOIN pms_spu p ON p.id = s.spu_id AND p.is_deleted = 0
            WHERE s.is_deleted = 0
              AND p.publish_status = 1
            ORDER BY s.id ASC
            LIMIT #{offset}, #{size}
            """)
    List<ProductSkuSearchDTO> selectPublishedSearchSources(
            @Param("offset") Long offset,
            @Param("size") Long size
    );

    /**
     * Query SKU IDs under a SPU.
     *
     * @param spuId SPU ID
     * @return SKU IDs
     */
    @Select("""
            SELECT id
            FROM pms_sku
            WHERE spu_id = #{spuId}
              AND is_deleted = 0
            ORDER BY id ASC
            """)
    List<Long> selectSkuIdsBySpuId(@Param("spuId") Long spuId);

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

    /**
     * Lock physical stock for normal order checkout.
     *
     * @param skuId SKU ID
     * @param quantity lock quantity
     * @return affected rows
     */
    @Update("""
            UPDATE pms_sku
            SET lock_stock = lock_stock + #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND stock - lock_stock >= #{quantity}
              AND is_deleted = 0
            """)
    int lockPhysicalStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * Release locked physical stock.
     *
     * @param skuId SKU ID
     * @param quantity release quantity
     * @return affected rows
     */
    @Update("""
            UPDATE pms_sku
            SET lock_stock = lock_stock - #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND lock_stock >= #{quantity}
              AND is_deleted = 0
            """)
    int releasePhysicalLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * Deduct physical stock for a normal paid order.
     *
     * @param skuId SKU ID
     * @param quantity quantity to deduct
     * @return affected rows
     */
    @Update("""
            UPDATE pms_sku
            SET stock = stock - #{quantity},
                lock_stock = lock_stock - #{quantity},
                sale_count = sale_count + #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND stock >= #{quantity}
              AND lock_stock >= #{quantity}
              AND is_deleted = 0
            """)
    int deductNormalPhysicalStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * Deduct physical stock for a seckill paid order.
     *
     * @param skuId SKU ID
     * @param quantity quantity to deduct
     * @return affected rows
     */
    @Update("""
            UPDATE pms_sku
            SET stock = stock - #{quantity},
                sale_count = sale_count + #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND stock >= #{quantity}
              AND is_deleted = 0
            """)
    int deductSeckillPhysicalStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * Refund physical stock after an order refund.
     *
     * @param skuId SKU ID
     * @param quantity quantity to refund
     * @return affected rows
     */
    @Update("""
            UPDATE pms_sku
            SET stock = stock + #{quantity},
                sale_count = sale_count - #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND sale_count >= #{quantity}
              AND is_deleted = 0
            """)
    int refundPhysicalStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
