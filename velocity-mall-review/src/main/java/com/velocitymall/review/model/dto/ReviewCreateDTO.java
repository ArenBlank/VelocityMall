package com.velocitymall.review.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布商品评价请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateDTO {

    @NotBlank(message = "订单编号不能为空")
    private String orderSn;

    @NotNull(message = "SKU ID不能为空")
    @Min(value = 1, message = "SKU ID必须大于0")
    private Long skuId;

    @NotNull(message = "SPU ID不能为空")
    @Min(value = 1, message = "SPU ID必须大于0")
    private Long spuId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1星")
    @Max(value = 5, message = "评分不能高于5星")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 1000, message = "评价内容不能超过1000个字符")
    private String content;
}
