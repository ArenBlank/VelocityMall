package com.velocitymall.order.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.order.model.dto.CartItemDTO;
import com.velocitymall.order.model.vo.CartItemVO;
import com.velocitymall.order.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cart API.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody CartItemDTO dto) {
        cartService.addItem(dto);
        return Result.success();
    }

    @GetMapping("/items")
    public Result<List<CartItemVO>> listItems() {
        return Result.success(cartService.listItems());
    }

    @DeleteMapping("/items/{sku-id}")
    public Result<Void> removeItem(
            @PathVariable("sku-id")
            @NotNull(message = "SKU ID不能为空")
            @Min(value = 1, message = "SKU ID必须大于0") Long skuId) {
        cartService.removeItem(skuId);
        return Result.success();
    }
}
