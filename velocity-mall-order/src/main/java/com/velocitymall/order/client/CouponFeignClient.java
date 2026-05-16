package com.velocitymall.order.client;

import com.velocitymall.common.model.dto.CouponUseDTO;
import com.velocitymall.common.model.vo.CouponUseVO;
import com.velocitymall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Internal coupon service client for order settlement.
 */
@FeignClient(name = "velocity-mall-coupon")
public interface CouponFeignClient {

    @PostMapping("/api/v1/coupons/inner/use")
    Result<CouponUseVO> useCoupon(@RequestBody CouponUseDTO dto);

    @PostMapping("/api/v1/coupons/inner/release")
    Result<Void> releaseCoupon(@RequestParam("orderSn") String orderSn);
}
