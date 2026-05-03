package com.velocitymall.order.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.vo.OrderVO;
import com.velocitymall.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 提交订单。
     *
     * @param dto 提交订单参数
     * @return 订单信息
     */
    @PostMapping
    public Result<OrderVO> submitOrder(@Valid @RequestBody SubmitOrderDTO dto) {
        return Result.success(orderService.submitOrder(dto));
    }
}
