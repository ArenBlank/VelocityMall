package com.velocitymall.admin.service;

import com.velocitymall.admin.model.dto.AdminCouponRequest;
import com.velocitymall.admin.model.dto.AdminSeckillActivityRequest;
import com.velocitymall.admin.model.dto.AdminSkuRequest;
import com.velocitymall.admin.model.dto.AdminSpuRequest;
import com.velocitymall.admin.model.vo.AdminLoginVO;
import com.velocitymall.admin.model.vo.AdminCouponVO;
import com.velocitymall.admin.model.vo.AdminOrderVO;
import com.velocitymall.admin.model.vo.AdminRebuildIndexVO;
import com.velocitymall.admin.model.vo.AdminReviewVO;
import com.velocitymall.admin.model.vo.AdminSeckillActivityVO;
import com.velocitymall.admin.model.vo.AdminSkuVO;
import com.velocitymall.admin.model.vo.AdminSpuVO;
import com.velocitymall.admin.model.vo.FileUploadVO;
import com.velocitymall.common.model.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {

    AdminLoginVO login(String username, String password);

    void deliverOrder(String orderSn, String deliveryCompany, String deliverySn);

    void publishSpu(Long spuId);

    void unpublishSpu(Long spuId);

    FileUploadVO uploadSkuCover(Long skuId, MultipartFile file);

    PageVO<AdminSpuVO> listSpus(Long page, Long size, String keyword, Integer status);

    AdminSpuVO getSpu(Long spuId);

    AdminSpuVO createSpu(AdminSpuRequest request);

    AdminSpuVO updateSpu(Long spuId, AdminSpuRequest request);

    AdminSkuVO createSku(AdminSkuRequest request);

    AdminSkuVO updateSku(Long skuId, AdminSkuRequest request);

    PageVO<AdminOrderVO> listOrders(Long page, Long size, Integer status, String orderSn, Long userId, Integer orderType);

    AdminOrderVO getAdminOrder(String orderSn);

    PageVO<AdminSeckillActivityVO> listSeckillActivities(Long page, Long size, String state, Long skuId);

    AdminSeckillActivityVO createSeckillActivity(AdminSeckillActivityRequest request);

    AdminSeckillActivityVO updateSeckillActivity(Long id, AdminSeckillActivityRequest request);

    AdminSeckillActivityVO updateSeckillActivityStatus(Long id, Integer status);

    AdminSeckillActivityVO preheatSeckillActivity(Long id);

    PageVO<AdminCouponVO> listCoupons(Long page, Long size, Integer status);

    AdminCouponVO createCoupon(AdminCouponRequest request);

    AdminCouponVO updateCoupon(Long id, AdminCouponRequest request);

    AdminCouponVO updateCouponStatus(Long id, Integer status);

    PageVO<AdminReviewVO> listReviews(Long page, Long size, Long spuId, String keyword);

    void deleteReview(Long id);

    AdminRebuildIndexVO rebuildSkuIndex();
}
