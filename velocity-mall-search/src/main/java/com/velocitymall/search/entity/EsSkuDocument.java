package com.velocitymall.search.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch SKU document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "velocity_mall_sku")
public class EsSkuDocument {

    @Id
    private Long skuId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String skuName;

    @Field(type = FieldType.Keyword, index = false)
    private String skuPic;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer saleCount;

    @Field(type = FieldType.Integer)
    private Integer status;
}
