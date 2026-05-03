package com.velocitymall.order.client;

import com.velocitymall.common.result.Result;
import com.velocitymall.order.model.dto.LockStockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 商品服务远程调用客户端。
 */
@FeignClient(name = "velocity-mall-product")
public interface ProductFeignClient {

    /**
     * 调用商品服务锁定库存。
     *
     * @param dto 锁定库存参数
     * @return 调用结果
     */
    @PutMapping("/api/v1/products/skus/lock-stock")
    Result<Void> lockStock(@RequestBody LockStockDTO dto);
}
