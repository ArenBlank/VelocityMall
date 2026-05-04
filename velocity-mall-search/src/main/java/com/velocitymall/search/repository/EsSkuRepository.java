package com.velocitymall.search.repository;

import com.velocitymall.search.entity.EsSkuDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * SKU Elasticsearch repository.
 */
public interface EsSkuRepository extends ElasticsearchRepository<EsSkuDocument, Long> {
}
