package com.velocitymall.admin.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSpuVO {

    private Long spuId;

    private Long categoryId;

    private String name;

    private String description;

    private Integer publishStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<AdminSkuVO> skuList;
}
