package com.velocitymall.search.controller;

import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.search.model.vo.RebuildIndexVO;
import com.velocitymall.search.model.vo.SearchSkuVO;
import com.velocitymall.search.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product search API.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * Search SKU documents.
     *
     * @param keyword keyword
     * @param sort sort option
     * @param page page number
     * @param size page size
     * @return paged search result
     */
    @GetMapping("/skus")
    public Result<PageVO<SearchSkuVO>> searchSkus(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "sale_desc") String sort,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page必须大于0")
            @Max(value = 100, message = "page不能大于100") Long page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size必须大于0")
            @Max(value = 100, message = "size不能大于100") Long size
    ) {
        return Result.success(searchService.searchSkus(keyword, sort, page, size));
    }

    /**
     * Internal offline SKU index rebuild.
     *
     * @return rebuild result
     */
    @PostMapping("/inner/skus/rebuild-index")
    public Result<RebuildIndexVO> rebuildSkuIndex() {
        return Result.success(searchService.rebuildSkuIndex());
    }
}
