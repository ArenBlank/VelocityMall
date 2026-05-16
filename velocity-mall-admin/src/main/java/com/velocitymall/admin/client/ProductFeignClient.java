package com.velocitymall.admin.client;

import com.velocitymall.admin.model.dto.ProductSkuUpdateRequest;
import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "velocity-mall-product")
public interface ProductFeignClient {

    @PutMapping("/api/v1/products/inner/spus/{spu-id}/publish")
    Result<Void> publishSpu(@PathVariable("spu-id") Long spuId);

    @PutMapping("/api/v1/products/inner/spus/{spu-id}/unpublish")
    Result<Void> unpublishSpu(@PathVariable("spu-id") Long spuId);

    @PutMapping("/api/v1/products/inner/skus/{sku-id}")
    Result<Void> updateSkuBasicInfo(
            @PathVariable("sku-id") Long skuId,
            @RequestBody ProductSkuUpdateRequest request
    );
}
