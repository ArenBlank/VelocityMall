package com.velocitymall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.VersionedEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 商品 SPU 实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("pms_spu")
public class Spu extends VersionedEntity {

    @TableField("category_id")
    private Long categoryId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("publish_status")
    private Integer publishStatus;
}
