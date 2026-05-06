package com.velocitymall.review.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 评价互动实体。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_review_interaction")
public class ReviewInteraction extends BaseEntity {

    @TableField("review_id")
    private Long reviewId;

    @TableField("user_id")
    private Long userId;

    @TableField("interaction_type")
    private Integer interactionType;
}
