package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.product.entity.Category;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 商品分类 Mapper。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * Query all enabled categories for homepage tree building.
     *
     * @return enabled category list
     */
    @Select("""
            SELECT id,
                   parent_id,
                   name,
                   sort,
                   icon,
                   level,
                   status,
                   create_time,
                   update_time,
                   is_deleted
            FROM pms_category
            WHERE status = 1
              AND is_deleted = 0
              AND level <= 3
            ORDER BY level ASC, sort ASC, id ASC
            """)
    List<Category> selectEnabledCategories();
}
