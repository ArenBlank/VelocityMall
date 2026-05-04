package com.velocitymall.search.service;

import com.velocitymall.common.model.dto.ProductSyncDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.search.model.vo.RebuildIndexVO;
import com.velocitymall.search.model.vo.SearchSkuVO;

/**
 * Search service.
 */
public interface SearchService {

    /**
     * Search published SKU documents.
     *
     * @param keyword keyword
     * @param sort sort option
     * @param page page number
     * @param size page size
     * @return paged SKU results
     */
    PageVO<SearchSkuVO> searchSkus(String keyword, String sort, Long page, Long size);

    /**
     * Rebuild SKU search index from product service.
     *
     * @return rebuild result
     */
    RebuildIndexVO rebuildSkuIndex();

    /**
     * Synchronize one product search document by MQ event.
     *
     * @param dto product sync event
     */
    void syncProduct(ProductSyncDTO dto);
}
