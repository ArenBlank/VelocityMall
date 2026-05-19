package com.velocitymall.admin.controller;

import com.velocitymall.admin.annotation.RequireAdminPermission;
import com.velocitymall.admin.constant.AdminPermissionCodes;
import com.velocitymall.admin.model.dto.AdminCouponRequest;
import com.velocitymall.admin.model.dto.AdminLoginDTO;
import com.velocitymall.admin.model.dto.AdminSeckillActivityRequest;
import com.velocitymall.admin.model.dto.AdminSkuRequest;
import com.velocitymall.admin.model.dto.AdminSpuRequest;
import com.velocitymall.admin.model.dto.AdminStatusRequest;
import com.velocitymall.admin.model.dto.SeckillTestRequest;
import com.velocitymall.admin.model.vo.AdminCouponVO;
import com.velocitymall.admin.model.vo.AdminLoginVO;
import com.velocitymall.admin.model.vo.AdminOrderVO;
import com.velocitymall.admin.model.vo.AdminProfileVO;
import com.velocitymall.admin.model.vo.AdminRebuildIndexVO;
import com.velocitymall.admin.model.vo.AdminReviewVO;
import com.velocitymall.admin.model.vo.AdminSeckillActivityVO;
import com.velocitymall.admin.model.vo.AdminSkuVO;
import com.velocitymall.admin.model.vo.AdminSpuVO;
import com.velocitymall.admin.model.vo.FileUploadVO;
import com.velocitymall.admin.service.AdminService;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/me")
    public Result<AdminProfileVO> getCurrentAdminProfile() {
        return Result.success(adminService.getCurrentAdminProfile());
    }

    @GetMapping("/products/spus")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_READ)
    public Result<PageVO<AdminSpuVO>> listSpus(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(adminService.listSpus(page, size, keyword, status));
    }

    @GetMapping("/products/spus/{spu-id}")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_READ)
    public Result<AdminSpuVO> getSpu(@PathVariable("spu-id") @Min(1) Long spuId) {
        return Result.success(adminService.getSpu(spuId));
    }

    @PostMapping("/products/spus")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
    public Result<AdminSpuVO> createSpu(@Valid @RequestBody AdminSpuRequest request) {
        return Result.success(adminService.createSpu(request));
    }

    @PutMapping("/products/spus/{spu-id}")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
    public Result<AdminSpuVO> updateSpu(
            @PathVariable("spu-id") @Min(1) Long spuId,
            @Valid @RequestBody AdminSpuRequest request
    ) {
        return Result.success(adminService.updateSpu(spuId, request));
    }

    @PutMapping("/products/spus/{spu-id}/status")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
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

    @PostMapping("/products/skus")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
    public Result<AdminSkuVO> createSku(@Valid @RequestBody AdminSkuRequest request) {
        return Result.success(adminService.createSku(request));
    }

    @PutMapping("/products/skus/{sku-id}")
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
    public Result<AdminSkuVO> updateSku(
            @PathVariable("sku-id") @Min(1) Long skuId,
            @Valid @RequestBody AdminSkuRequest request
    ) {
        return Result.success(adminService.updateSku(skuId, request));
    }

    @PostMapping(
            value = "/products/skus/{sku-id}/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @RequireAdminPermission(AdminPermissionCodes.PRODUCT_WRITE)
    public Result<FileUploadVO> uploadSkuCover(
            @NotNull @Min(1) @PathVariable("sku-id") Long skuId,
            @RequestParam("file") MultipartFile file
    ) {
        return Result.success(adminService.uploadSkuCover(skuId, file));
    }

    @GetMapping("/orders")
    @RequireAdminPermission(AdminPermissionCodes.ORDER_READ)
    public Result<PageVO<AdminOrderVO>> listOrders(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "orderSn", required = false) String orderSn,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "orderType", required = false) Integer orderType
    ) {
        return Result.success(adminService.listOrders(page, size, status, orderSn, userId, orderType));
    }

    @GetMapping("/orders/{order-sn}")
    @RequireAdminPermission(AdminPermissionCodes.ORDER_READ)
    public Result<AdminOrderVO> getOrder(@PathVariable("order-sn") @NotBlank String orderSn) {
        return Result.success(adminService.getAdminOrder(orderSn));
    }

    @PostMapping("/orders/{order-sn}/deliver")
    @RequireAdminPermission(AdminPermissionCodes.ORDER_DELIVER)
    public Result<Void> deliverOrder(
            @NotBlank @PathVariable("order-sn") String orderSn,
            @NotBlank @RequestParam("deliveryCompany") String deliveryCompany,
            @NotBlank @RequestParam("deliverySn") String deliverySn
    ) {
        adminService.deliverOrder(orderSn, deliveryCompany, deliverySn);
        return Result.success();
    }

    @GetMapping("/seckill/activities")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_READ)
    public Result<PageVO<AdminSeckillActivityVO>> listSeckillActivities(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "skuId", required = false) Long skuId
    ) {
        return Result.success(adminService.listSeckillActivities(page, size, state, skuId));
    }

    @PostMapping("/seckill/activities")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_WRITE)
    public Result<AdminSeckillActivityVO> createSeckillActivity(
            @Valid @RequestBody AdminSeckillActivityRequest request
    ) {
        return Result.success(adminService.createSeckillActivity(request));
    }

    @PutMapping("/seckill/activities/{id}")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_WRITE)
    public Result<AdminSeckillActivityVO> updateSeckillActivity(
            @PathVariable("id") @Min(1) Long id,
            @Valid @RequestBody AdminSeckillActivityRequest request
    ) {
        return Result.success(adminService.updateSeckillActivity(id, request));
    }

    @PutMapping("/seckill/activities/{id}/status")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_WRITE)
    public Result<AdminSeckillActivityVO> updateSeckillActivityStatus(
            @PathVariable("id") @Min(1) Long id,
            @Valid @RequestBody AdminStatusRequest request
    ) {
        return Result.success(adminService.updateSeckillActivityStatus(id, request.getStatus()));
    }

    @PostMapping("/seckill/activities/{id}/preheat")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_PREHEAT)
    public Result<AdminSeckillActivityVO> preheatSeckillActivity(@PathVariable("id") @Min(1) Long id) {
        return Result.success(adminService.preheatSeckillActivity(id));
    }

    @PostMapping("/seckill/activities/test/init")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_PREHEAT)
    public Result<Map<String, Object>> initSeckillTest(@Valid @RequestBody SeckillTestRequest request) {
        return Result.success(adminService.initSeckillTest(request));
    }

    @PostMapping("/seckill/activities/test/cleanup/{skuId}")
    @RequireAdminPermission(AdminPermissionCodes.SECKILL_PREHEAT)
    public Result<Map<String, Object>> cleanupSeckillTest(@PathVariable("skuId") @Min(1) Long skuId) {
        return Result.success(adminService.cleanupSeckillTest(skuId));
    }

    @GetMapping("/coupons")
    @RequireAdminPermission(AdminPermissionCodes.COUPON_READ)
    public Result<PageVO<AdminCouponVO>> listCoupons(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(adminService.listCoupons(page, size, status));
    }

    @PostMapping("/coupons")
    @RequireAdminPermission(AdminPermissionCodes.COUPON_WRITE)
    public Result<AdminCouponVO> createCoupon(@Valid @RequestBody AdminCouponRequest request) {
        return Result.success(adminService.createCoupon(request));
    }

    @PutMapping("/coupons/{id}")
    @RequireAdminPermission(AdminPermissionCodes.COUPON_WRITE)
    public Result<AdminCouponVO> updateCoupon(
            @PathVariable("id") @Min(1) Long id,
            @Valid @RequestBody AdminCouponRequest request
    ) {
        return Result.success(adminService.updateCoupon(id, request));
    }

    @PutMapping("/coupons/{id}/status")
    @RequireAdminPermission(AdminPermissionCodes.COUPON_WRITE)
    public Result<AdminCouponVO> updateCouponStatus(
            @PathVariable("id") @Min(1) Long id,
            @Valid @RequestBody AdminStatusRequest request
    ) {
        return Result.success(adminService.updateCouponStatus(id, request.getStatus()));
    }

    @GetMapping("/reviews")
    @RequireAdminPermission(AdminPermissionCodes.REVIEW_READ)
    public Result<PageVO<AdminReviewVO>> listReviews(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) Long size,
            @RequestParam(value = "spuId", required = false) Long spuId,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return Result.success(adminService.listReviews(page, size, spuId, keyword));
    }

    @DeleteMapping("/reviews/{id}")
    @RequireAdminPermission(AdminPermissionCodes.REVIEW_DELETE)
    public Result<Void> deleteReview(@PathVariable("id") @Min(1) Long id) {
        adminService.deleteReview(id);
        return Result.success();
    }

    @PostMapping("/search/skus/rebuild-index")
    @RequireAdminPermission(AdminPermissionCodes.SYSTEM_REBUILD)
    public Result<AdminRebuildIndexVO> rebuildSkuIndex() {
        return Result.success(adminService.rebuildSkuIndex());
    }
}
