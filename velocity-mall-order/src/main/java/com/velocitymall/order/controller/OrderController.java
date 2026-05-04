package com.velocitymall.order.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.common.model.vo.OrderDetailVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.vo.OrderVO;
import com.velocitymall.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Query current user's order list by page.
     *
     * @param page page number
     * @param size page size
     * @param status optional order status
     * @return paged order details
     */
    @GetMapping
    public Result<PageVO<OrderDetailVO>> listMyOrders(
            @Min(value = 1, message = "page must be greater than 0")
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @Min(value = 1, message = "size must be greater than 0")
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(orderService.listMyOrders(page, size, status));
    }

    /**
     * Query current user's order detail.
     *
     * @param orderSn order number
     * @return order detail
     */
    @GetMapping("/{order-sn}")
    public Result<OrderDetailVO> getOrderDetail(
            @NotBlank(message = "orderSn cannot be blank")
            @PathVariable("order-sn") String orderSn
    ) {
        return Result.success(orderService.getOrderDetail(orderSn));
    }

    /**
     * Cancel a waiting-pay order.
     *
     * @param orderSn order number
     * @return success result
     */
    @PutMapping("/{order-sn}/cancel")
    public Result<Void> cancelOrder(
            @NotBlank(message = "orderSn cannot be blank")
            @PathVariable("order-sn") String orderSn
    ) {
        orderService.cancelOrder(orderSn);
        return Result.success();
    }

    /**
     * Mock payment success callback.
     *
     * @param orderSn order number
     * @param payType payment type
     * @return success result
     */
    @PostMapping("/pay/mock")
    public Result<Void> mockPaySuccess(
            @NotBlank(message = "orderSn cannot be blank") @RequestParam("orderSn") String orderSn,
            @NotNull(message = "payType cannot be null") @RequestParam("payType") Integer payType
    ) {
        orderService.mockPaySuccess(orderSn, payType);
        return Result.success();
    }

    /**
     * Mock refund for a paid order.
     *
     * @param orderSn order number
     * @return success result
     */
    @PostMapping("/{order-sn}/refund/mock")
    public Result<Void> mockRefund(
            @NotBlank(message = "orderSn cannot be blank")
            @PathVariable("order-sn") String orderSn
    ) {
        orderService.mockRefund(orderSn);
        return Result.success();
    }
}
