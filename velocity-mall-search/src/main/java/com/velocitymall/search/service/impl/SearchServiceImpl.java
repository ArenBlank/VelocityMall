package com.velocitymall.search.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.common.model.dto.ProductSyncDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.search.client.ProductFeignClient;
import com.velocitymall.search.entity.EsSkuDocument;
import com.velocitymall.search.model.vo.RebuildIndexVO;
import com.velocitymall.search.model.vo.SearchSkuVO;
import com.velocitymall.search.repository.EsSkuRepository;
import com.velocitymall.search.service.SearchService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Search service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int PRODUCT_STATUS_PUBLISHED = 1;

    private static final long MAX_SEARCH_WINDOW = 10000L;

    private static final long REBUILD_PAGE_SIZE = 500L;

    private static final String REBUILD_LOCK_KEY = "velocitymall:search:rebuild-lock";

    private static final String SORT_PRICE_ASC = "price_asc";

    private static final String SORT_PRICE_DESC = "price_desc";

    private static final String SORT_SALE_DESC = "sale_desc";

    private final ElasticsearchOperations elasticsearchOperations;

    private final EsSkuRepository esSkuRepository;

    private final ProductFeignClient productFeignClient;

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    @Override
    public PageVO<SearchSkuVO> searchSkus(String keyword, String sort, Long page, Long size) {
        validateSearchPage(page, size);
        PageRequest pageRequest = PageRequest.of(Math.toIntExact(page - 1), Math.toIntExact(size), resolveSort(sort));
        StringQuery query = new StringQuery(buildSearchQuery(keyword), pageRequest);
        SearchHits<EsSkuDocument> hits = elasticsearchOperations.search(query, EsSkuDocument.class);

        List<SearchSkuVO> records = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(this::toSearchSkuVO)
                .toList();
        long total = hits.getTotalHits();
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageVO<>(page, size, total, pages, records);
    }

    @Override
    public RebuildIndexVO rebuildSkuIndex() {
        RLock lock = redissonClient.getLock(REBUILD_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.MINUTES);
            if (!locked) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "索引重建正在执行，请稍后再试");
            }

            // This is a lightweight offline rebuild. It should run during low traffic
            // or after pausing incremental consumers to avoid stale overwrite.
            esSkuRepository.deleteAll();
            long currentPage = 1L;
            long indexedCount = 0L;
            while (true) {
                PageVO<ProductSkuSearchDTO> sourcePage = fetchPublishedSkuSourcePage(currentPage, REBUILD_PAGE_SIZE);
                if (sourcePage == null || CollectionUtils.isEmpty(sourcePage.getRecords())) {
                    break;
                }
                List<EsSkuDocument> documents = sourcePage.getRecords()
                        .stream()
                        .map(this::toDocument)
                        .toList();
                esSkuRepository.saveAll(documents);
                indexedCount += documents.size();

                if (sourcePage.getPages() == null || currentPage >= sourcePage.getPages()) {
                    break;
                }
                currentPage++;
            }
            return RebuildIndexVO.builder()
                    .indexedCount(indexedCount)
                    .message("索引重建完成")
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "索引重建被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void syncProduct(ProductSyncDTO dto) {
        if (dto == null || dto.getSkuId() == null || dto.getSkuId() <= 0) {
            log.error("Invalid product sync message, discard. message: {}", dto);
            return;
        }
        if (dto.getAction() == null) {
            log.error("Product sync action is absent, discard. skuId: {}", dto.getSkuId());
            return;
        }
        if (Integer.valueOf(ProductSyncDTO.ACTION_DELETE).equals(dto.getAction())) {
            esSkuRepository.deleteById(dto.getSkuId());
            log.info("Product search document deleted. skuId: {}", dto.getSkuId());
            return;
        }
        if (!Integer.valueOf(ProductSyncDTO.ACTION_UPSERT).equals(dto.getAction())) {
            log.error("Invalid product sync action, discard. skuId: {}, action: {}", dto.getSkuId(), dto.getAction());
            return;
        }

        ProductSkuSearchDTO source = fetchSkuSource(dto.getSkuId());
        if (source == null) {
            log.error("Product source is absent, discard sync message. skuId: {}", dto.getSkuId());
            return;
        }
        if (!Integer.valueOf(PRODUCT_STATUS_PUBLISHED).equals(source.getStatus())) {
            esSkuRepository.deleteById(dto.getSkuId());
            log.info("Product is not published, search document deleted. skuId: {}", dto.getSkuId());
            return;
        }
        if (!isValidSource(source)) {
            log.error("Product source data is invalid, discard sync message. source: {}", source);
            return;
        }
        esSkuRepository.save(toDocument(source));
        log.info("Product search document upserted. skuId: {}", dto.getSkuId());
    }

    private ProductSkuSearchDTO fetchSkuSource(Long skuId) {
        Result<ProductSkuSearchDTO> result = productFeignClient.getSkuSearchSource(skuId);
        if (result == null) {
            throw new RuntimeException("Product service returned null response");
        }
        if (!ResultCode.SUCCESS.getCode().equals(result.getCode())) {
            log.error("Product service returned non-success for search sync. skuId: {}, code: {}, message: {}",
                    skuId, result.getCode(), result.getMessage());
            return null;
        }
        return result.getData();
    }

    private PageVO<ProductSkuSearchDTO> fetchPublishedSkuSourcePage(Long page, Long size) {
        Result<PageVO<ProductSkuSearchDTO>> result = productFeignClient.listPublishedSearchSources(page, size);
        if (result == null || !ResultCode.SUCCESS.getCode().equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取商品索引源数据失败");
        }
        return result.getData();
    }

    private void validateSearchPage(Long page, Long size) {
        if (page == null || page <= 0 || size == null || size <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索分页参数非法");
        }
        if (page * size > MAX_SEARCH_WINDOW) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索分页范围过大");
        }
    }

    private boolean isValidSource(ProductSkuSearchDTO source) {
        return source.getSkuId() != null && source.getSkuId() > 0 && StringUtils.hasText(source.getSkuName());
    }

    private Sort resolveSort(String sort) {
        if (SORT_PRICE_ASC.equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }
        if (SORT_PRICE_DESC.equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return Sort.by(Sort.Direction.DESC, "saleCount");
    }

    private String buildSearchQuery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return """
                    {
                      "bool": {
                        "filter": [
                          {"term": {"status": 1}}
                        ]
                      }
                    }
                    """;
        }
        try {
            String trimmedKeyword = keyword.trim();
            String escapedKeyword = objectMapper.writeValueAsString(keyword);
            if (trimmedKeyword.matches("\\d+")) {
                Long skuId = Long.valueOf(trimmedKeyword);
                return """
                        {
                          "bool": {
                            "should": [
                              {"term": {"skuId": %d}},
                              {"match_phrase": {"skuName": %s}}
                            ],
                            "minimum_should_match": 1,
                            "filter": [
                              {"term": {"status": 1}}
                            ]
                          }
                        }
                        """.formatted(skuId, escapedKeyword);
            }
            return """
                    {
                      "bool": {
                        "must": [
                          {"match_phrase": {"skuName": %s}}
                        ],
                        "filter": [
                          {"term": {"status": 1}}
                        ]
                      }
                    }
                    """.formatted(escapedKeyword);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索关键词非法");
        }
    }

    private EsSkuDocument toDocument(ProductSkuSearchDTO source) {
        return EsSkuDocument.builder()
                .skuId(source.getSkuId())
                .skuName(source.getSkuName())
                .skuPic(source.getSkuPic())
                .price(source.getPrice())
                .saleCount(source.getSaleCount() == null ? 0 : source.getSaleCount())
                .status(source.getStatus())
                .build();
    }

    private SearchSkuVO toSearchSkuVO(EsSkuDocument document) {
        return SearchSkuVO.builder()
                .skuId(document.getSkuId())
                .skuName(document.getSkuName())
                .skuPic(document.getSkuPic())
                .price(document.getPrice())
                .saleCount(document.getSaleCount())
                .build();
    }
}
