package com.velocitymall.review.client;

import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Order service internal APIs.
 */
@FeignClient(name = "velocity-mall-order")
public interface OrderFeignClient {

    /**
     * Check whether a user has paid for a SKU.
     *
     * @param userId user ID
     * @param orderSn order number
     * @param skuId SKU ID
     * @return true if the user has paid for the SKU
     */
    @GetMapping("/api/v1/orders/inner/check-purchase")
    Result<Boolean> checkPurchase(
            @RequestParam("userId") Long userId,
            @RequestParam("orderSn") String orderSn,
            @RequestParam("skuId") Long skuId
    );
}
