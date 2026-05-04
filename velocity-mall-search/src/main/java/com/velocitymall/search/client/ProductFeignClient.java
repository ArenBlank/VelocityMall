package com.velocitymall.search.client;

import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Product service RPC client for search index synchronization.
 */
@FeignClient(name = "velocity-mall-product")
public interface ProductFeignClient {

    /**
     * Query SKU source data by SKU ID.
     *
     * @param skuId SKU ID
     * @return SKU source data
     */
    @GetMapping("/api/v1/products/inner/skus/{sku-id}")
    Result<ProductSkuSearchDTO> getSkuSearchSource(@PathVariable("sku-id") Long skuId);

    /**
     * Page query published SKU source data.
     *
     * @param page page number
     * @param size page size
     * @return paged SKU source data
     */
    @GetMapping("/api/v1/products/inner/skus/search-source")
    Result<PageVO<ProductSkuSearchDTO>> listPublishedSearchSources(
            @RequestParam("page") Long page,
            @RequestParam("size") Long size
    );
}
