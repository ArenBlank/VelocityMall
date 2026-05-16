package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminSeckillActivityRequest {

    @NotNull(message = "skuId不能为空")
    @Min(value = 1, message = "skuId必须大于0")
    private Long skuId;

    @NotNull(message = "spuId不能为空")
    @Min(value = 1, message = "spuId必须大于0")
    private Long spuId;

    @NotBlank(message = "活动名称不能为空")
    private String activityName;

    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.00", message = "秒杀价不能小于0")
    private BigDecimal seckillPrice;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.00", message = "原价不能小于0")
    private BigDecimal originalPrice;

    @NotNull(message = "活动库存不能为空")
    @Min(value = 0, message = "活动库存不能小于0")
    private Integer seckillStock;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Integer status;
}
