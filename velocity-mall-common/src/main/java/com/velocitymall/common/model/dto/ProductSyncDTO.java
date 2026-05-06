package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Product search index synchronization event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductSyncDTO extends BaseMessageDTO {

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
