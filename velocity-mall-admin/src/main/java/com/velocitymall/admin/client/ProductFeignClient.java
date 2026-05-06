package com.velocitymall.admin.client;

import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "velocity-mall-product")
public interface ProductFeignClient {

    @PutMapping("/api/v1/products/inner/spus/{spu-id}/publish")
    Result<Void> publishSpu(@PathVariable("spu-id") Long spuId);

    @PutMapping("/api/v1/products/inner/spus/{spu-id}/unpublish")
    Result<Void> unpublishSpu(@PathVariable("spu-id") Long spuId);
}
