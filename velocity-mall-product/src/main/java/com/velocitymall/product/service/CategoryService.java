package com.velocitymall.product.service;

import com.velocitymall.common.model.vo.CategoryTreeVO;
import java.util.List;

/**
 * Product category service.
 */
public interface CategoryService {

    /**
     * Query homepage category tree.
     *
     * @return category tree
     */
    List<CategoryTreeVO> getCategoryTree();
}
