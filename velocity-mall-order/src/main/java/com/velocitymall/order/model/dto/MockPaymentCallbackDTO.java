package com.velocitymall.order.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Mock third-party payment/refund callback payload.
 */
@Data
public class MockPaymentCallbackDTO {

    @NotNull(message = "transactionType cannot be null")
    private Integer transactionType;

    @NotBlank(message = "orderSn cannot be blank")
    private String orderSn;

    @NotBlank(message = "requestNo cannot be blank")
    private String requestNo;

    @NotBlank(message = "tradeNo cannot be blank")
    private String tradeNo;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.00", message = "amount cannot be negative")
    private BigDecimal amount;

    @NotNull(message = "payType cannot be null")
    private Integer payType;

    @NotBlank(message = "status cannot be blank")
    private String status;

    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;

    @NotBlank(message = "nonce cannot be blank")
    private String nonce;

    @NotBlank(message = "sign cannot be blank")
    private String sign;
}
