package com.velocitymall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.VersionedEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("pms_sku")
public class AdminSku extends VersionedEntity {

    @TableField("spu_id")
    private Long spuId;

    @TableField("sku_name")
    private String skuName;

    @TableField("sku_code")
    private String skuCode;

    @TableField("price")
    private BigDecimal price;

    @TableField("stock")
    private Integer stock;

    @TableField("lock_stock")
    private Integer lockStock;

    @TableField("sale_count")
    private Integer saleCount;

    @TableField("cover_img")
    private String coverImg;
}
