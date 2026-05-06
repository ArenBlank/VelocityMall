package com.velocitymall.order.client;

import com.velocitymall.common.result.Result;
import com.velocitymall.order.model.vo.UserAddressVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "velocity-mall-user")
public interface UserFeignClient {

    @GetMapping("/api/v1/users/inner/addresses/{id}")
    Result<UserAddressVO> getAddressById(@PathVariable("id") Long id, @RequestParam("userId") Long userId);
}
