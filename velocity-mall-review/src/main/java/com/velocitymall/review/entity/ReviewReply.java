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
 * Product review reply entity.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_review_reply")
public class ReviewReply extends BaseEntity {

    @TableField("review_id")
    private Long reviewId;

    @TableField("user_id")
    private Long userId;

    @TableField("content")
    private String content;
}
