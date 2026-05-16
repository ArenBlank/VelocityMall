package com.velocitymall.admin.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminCouponRequest {

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", message = "优惠金额不能小于0")
    private BigDecimal amount;

    @NotNull(message = "使用门槛不能为空")
    @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
    private BigDecimal minPoint;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    @NotNull(message = "每人限领不能为空")
    @Min(value = 1, message = "每人限领必须大于0")
    private Integer limitPerUser;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Integer status;
}
