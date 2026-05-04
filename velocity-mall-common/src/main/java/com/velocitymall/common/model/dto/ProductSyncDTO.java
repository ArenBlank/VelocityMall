package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product search index synchronization event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncDTO {

    /**
     * Upsert product document into Elasticsearch.
     */
    public static final int ACTION_UPSERT = 1;

    /**
     * Delete product document from Elasticsearch.
     */
    public static final int ACTION_DELETE = 2;

    private Long skuId;

    private Integer action;
}
