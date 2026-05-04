package com.velocitymall.order.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Submit normal order request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitOrderDTO {

    @NotEmpty(message = "SKU ID列表不能为空")
    private List<@NotNull(message = "SKU ID不能为空") @Min(value = 1, message = "SKU ID必须大于0") Long> skuIds;
}
