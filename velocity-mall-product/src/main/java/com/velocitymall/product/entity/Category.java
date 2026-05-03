package com.velocitymall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 商品分类实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("pms_category")
public class Category extends BaseEntity {

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("sort")
    private Integer sort;

    @TableField("icon")
    private String icon;

    @TableField("level")
    private Integer level;
}
