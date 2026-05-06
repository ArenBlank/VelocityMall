package com.velocitymall.review.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价互动请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewInteractionDTO {

    @NotNull(message = "互动类型不能为空")
    @Min(value = 1, message = "互动类型非法")
    @Max(value = 2, message = "互动类型非法")
    private Integer interactionType;
}
