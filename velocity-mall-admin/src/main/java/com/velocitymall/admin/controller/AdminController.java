package com.velocitymall.admin.controller;

import com.velocitymall.admin.model.dto.AdminLoginDTO;
import com.velocitymall.admin.model.vo.AdminLoginVO;
import com.velocitymall.admin.service.AdminService;
import com.velocitymall.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminService.login(dto.getUsername(), dto.getPassword()));
    }

    @PostMapping("/orders/{order-sn}/deliver")
    public Result<Void> deliverOrder(
            @NotBlank @PathVariable("order-sn") String orderSn,
            @NotBlank @RequestParam("deliveryCompany") String deliveryCompany,
            @NotBlank @RequestParam("deliverySn") String deliverySn
    ) {
        adminService.deliverOrder(orderSn, deliveryCompany, deliverySn);
        return Result.success();
    }

    @PutMapping("/products/spus/{spu-id}/status")
    public Result<Void> updateSpuStatus(
            @NotNull @Min(1) @PathVariable("spu-id") Long spuId,
            @NotNull @RequestParam("action") String action
    ) {
        if ("publish".equals(action)) {
            adminService.publishSpu(spuId);
        } else if ("unpublish".equals(action)) {
            adminService.unpublishSpu(spuId);
        } else {
            return Result.failed(40000, "action参数必须为 publish 或 unpublish");
        }
        return Result.success();
    }
}
