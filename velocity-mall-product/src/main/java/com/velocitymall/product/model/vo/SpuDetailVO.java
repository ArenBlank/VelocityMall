package com.velocitymall.product.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SPU 详情视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpuDetailVO {

    private Long spuId;

    private Long categoryId;

    private String name;

    private String description;

    private Integer publishStatus;

    private List<SkuVO> skuList;
}
