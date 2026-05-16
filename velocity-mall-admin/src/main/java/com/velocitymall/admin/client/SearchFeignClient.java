package com.velocitymall.admin.client;

import com.velocitymall.admin.model.vo.AdminRebuildIndexVO;
import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "velocity-mall-search")
public interface SearchFeignClient {

    @PostMapping("/api/v1/search/inner/skus/rebuild-index")
    Result<AdminRebuildIndexVO> rebuildSkuIndex();
}
