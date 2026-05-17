package com.velocitymall.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.velocitymall.common.entity.VersionedEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Payment and refund transaction log.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("oms_payment_transaction")
public class PaymentTransaction extends VersionedEntity {

    @TableField("order_sn")
    private String orderSn;

    @TableField("user_id")
    private Long userId;

    @TableField("transaction_type")
    private Integer transactionType;

    @TableField("pay_type")
    private Integer payType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("request_no")
    private String requestNo;

    @TableField("trade_no")
    private String tradeNo;

    @TableField("status")
    private Integer status;

    @TableField("callback_payload")
    private String callbackPayload;

    @TableField("fail_reason")
    private String failReason;

    @TableField("success_time")
    private LocalDateTime successTime;
}
