package com.velocitymall.admin.client;

import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "velocity-mall-order")
public interface OrderFeignClient {

    @PostMapping("/api/v1/orders/inner/{order-sn}/deliver")
    Result<Void> deliver(
            @PathVariable("order-sn") String orderSn,
            @RequestParam("deliveryCompany") String deliveryCompany,
            @RequestParam("deliverySn") String deliverySn
    );
}
