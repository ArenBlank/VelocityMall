package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.product.entity.Spu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 SPU Mapper。
 */
@Mapper
public interface SpuMapper extends BaseMapper<Spu> {

    /**
     * Update SPU publish status with optimistic version increment.
     *
     * @param spuId SPU ID
     * @param publishStatus publish status
     * @return affected rows
     */
    @Update("""
            UPDATE pms_spu
            SET publish_status = #{publishStatus},
                version = version + 1
            WHERE id = #{spuId}
              AND is_deleted = 0
            """)
    int updatePublishStatus(@Param("spuId") Long spuId, @Param("publishStatus") Integer publishStatus);
}
