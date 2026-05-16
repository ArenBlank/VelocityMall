package com.velocitymall.admin.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminOrderVO {

    private Long userId;

    private String orderSn;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer payType;

    private LocalDateTime payTime;

    private Integer orderType;

    private Integer status;

    private String remark;

    private String receiverName;

    private String receiverPhone;

    private String receiverProvince;

    private String receiverCity;

    private String receiverRegion;

    private String receiverDetailAddress;

    private String deliveryCompany;

    private String deliverySn;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime createTime;

    private List<AdminOrderItemVO> items;
}
