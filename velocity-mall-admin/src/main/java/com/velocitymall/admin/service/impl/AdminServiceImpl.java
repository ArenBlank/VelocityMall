package com.velocitymall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.velocitymall.admin.client.OrderFeignClient;
import com.velocitymall.admin.client.ProductFeignClient;
import com.velocitymall.admin.client.SearchFeignClient;
import com.velocitymall.admin.entity.Admin;
import com.velocitymall.admin.entity.AdminCoupon;
import com.velocitymall.admin.entity.AdminOrder;
import com.velocitymall.admin.entity.AdminOrderItem;
import com.velocitymall.admin.entity.AdminReview;
import com.velocitymall.admin.entity.AdminSeckillActivity;
import com.velocitymall.admin.entity.AdminSku;
import com.velocitymall.admin.entity.AdminSpu;
import com.velocitymall.admin.mapper.AdminCouponMapper;
import com.velocitymall.admin.mapper.AdminMapper;
import com.velocitymall.admin.mapper.AdminOrderItemMapper;
import com.velocitymall.admin.mapper.AdminOrderMapper;
import com.velocitymall.admin.mapper.AdminReviewMapper;
import com.velocitymall.admin.mapper.AdminSeckillActivityMapper;
import com.velocitymall.admin.mapper.AdminSkuMapper;
import com.velocitymall.admin.mapper.AdminSpuMapper;
import com.velocitymall.admin.model.dto.AdminCouponRequest;
import com.velocitymall.admin.model.dto.AdminSeckillActivityRequest;
import com.velocitymall.admin.model.dto.AdminSkuRequest;
import com.velocitymall.admin.model.dto.AdminSpuRequest;
import com.velocitymall.admin.model.dto.ProductSkuUpdateRequest;
import com.velocitymall.admin.model.vo.AdminCouponVO;
import com.velocitymall.admin.model.vo.AdminLoginVO;
import com.velocitymall.admin.model.vo.AdminOrderItemVO;
import com.velocitymall.admin.model.vo.AdminOrderVO;
import com.velocitymall.admin.model.vo.AdminRebuildIndexVO;
import com.velocitymall.admin.model.vo.AdminReviewVO;
import com.velocitymall.admin.model.vo.AdminSeckillActivityVO;
import com.velocitymall.admin.model.vo.AdminSkuVO;
import com.velocitymall.admin.model.vo.AdminSpuVO;
import com.velocitymall.admin.model.vo.FileUploadVO;
import com.velocitymall.admin.service.AdminService;
import com.velocitymall.admin.service.ProductImageStorageService;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final String PRODUCT_SPU_CACHE_PREFIX = "velocitymall:product:spu:";
    private static final String PRODUCT_SKU_CACHE_PREFIX = "velocitymall:product:sku:";
    private static final String SECKILL_STOCK_PREFIX = "velocitymall:seckill:stock:";
    private static final String SECKILL_BOUGHT_PREFIX = "velocitymall:seckill:bought:";

    private final AdminMapper adminMapper;
    private final AdminSpuMapper adminSpuMapper;
    private final AdminSkuMapper adminSkuMapper;
    private final AdminOrderMapper adminOrderMapper;
    private final AdminOrderItemMapper adminOrderItemMapper;
    private final AdminCouponMapper adminCouponMapper;
    private final AdminReviewMapper adminReviewMapper;
    private final AdminSeckillActivityMapper adminSeckillActivityMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final SearchFeignClient searchFeignClient;
    private final ProductImageStorageService productImageStorageService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public AdminLoginVO login(String username, String password) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        String token = generateAdminJwt(admin);
        log.info("管理员登录成功: {}", admin.getUsername());
        return AdminLoginVO.builder()
                .token(token)
                .adminId(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .build();
    }

    @Override
    public void deliverOrder(String orderSn, String deliveryCompany, String deliverySn) {
        Result<Void> result = orderFeignClient.deliver(orderSn, deliveryCompany, deliverySn);
        if (!isSuccess(result)) {
            throw buildDownstreamException("发货", result);
        }
        log.info("管理员发货成功: orderSn={}, company={}, tracking={}", orderSn, deliveryCompany, deliverySn);
    }

    @Override
    public void publishSpu(Long spuId) {
        Result<Void> result = productFeignClient.publishSpu(spuId);
        if (!isSuccess(result)) {
            throw buildDownstreamException("上架", result);
        }
        cleanSpuCache(spuId);
        log.info("管理员上架 SPU: {}", spuId);
    }

    @Override
    public void unpublishSpu(Long spuId) {
        Result<Void> result = productFeignClient.unpublishSpu(spuId);
        if (!isSuccess(result)) {
            throw buildDownstreamException("下架", result);
        }
        cleanSpuCache(spuId);
        log.info("管理员下架 SPU: {}", spuId);
    }

    @Override
    public FileUploadVO uploadSkuCover(Long skuId, MultipartFile file) {
        AdminSku sku = requireSku(skuId);
        FileUploadVO uploaded = productImageStorageService.uploadSkuCover(skuId, file);
        Result<Void> result = productFeignClient.updateSkuBasicInfo(
                skuId,
                new ProductSkuUpdateRequest(null, null, uploaded.getUrl())
        );
        if (!isSuccess(result)) {
            throw buildDownstreamException("更新商品图片", result);
        }
        cleanProductCache(sku.getSpuId(), skuId);
        log.info("管理员更新 SKU 封面图: skuId={}, url={}", skuId, uploaded.getUrl());
        return uploaded;
    }

    @Override
    public PageVO<AdminSpuVO> listSpus(Long page, Long size, String keyword, Integer status) {
        Page<AdminSpu> spuPage = adminSpuMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AdminSpu>()
                        .like(StringUtils.hasText(keyword), AdminSpu::getName, keyword)
                        .eq(status != null, AdminSpu::getPublishStatus, status)
                        .orderByDesc(AdminSpu::getUpdateTime)
        );
        List<Long> spuIds = spuPage.getRecords().stream().map(AdminSpu::getId).toList();
        Map<Long, List<AdminSku>> skuMap = listSkuMap(spuIds);
        List<AdminSpuVO> records = spuPage.getRecords().stream()
                .map(spu -> toSpuVO(spu, skuMap.getOrDefault(spu.getId(), List.of())))
                .toList();
        return toPageVO(spuPage, records);
    }

    @Override
    public AdminSpuVO getSpu(Long spuId) {
        AdminSpu spu = requireSpu(spuId);
        List<AdminSku> skus = adminSkuMapper.selectList(
                new LambdaQueryWrapper<AdminSku>().eq(AdminSku::getSpuId, spuId).orderByAsc(AdminSku::getId)
        );
        return toSpuVO(spu, skus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSpuVO createSpu(AdminSpuRequest request) {
        AdminSpu spu = AdminSpu.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .publishStatus(normalizeBinaryStatus(request.getPublishStatus(), STATUS_DISABLED))
                .version(0)
                .build();
        adminSpuMapper.insert(spu);
        return toSpuVO(spu, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSpuVO updateSpu(Long spuId, AdminSpuRequest request) {
        AdminSpu existing = requireSpu(spuId);
        AdminSpu update = AdminSpu.builder()
                .id(spuId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .publishStatus(normalizeBinaryStatus(request.getPublishStatus(), existing.getPublishStatus()))
                .build();
        adminSpuMapper.updateById(update);
        cleanSpuCache(spuId);
        return getSpu(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSkuVO createSku(AdminSkuRequest request) {
        requireSpu(request.getSpuId());
        AdminSku sku = AdminSku.builder()
                .spuId(request.getSpuId())
                .skuName(request.getSkuName())
                .skuCode(request.getSkuCode())
                .price(request.getPrice())
                .stock(request.getStock())
                .lockStock(0)
                .saleCount(0)
                .coverImg(request.getCoverImg())
                .version(0)
                .build();
        insertSku(sku);
        cleanProductCache(sku.getSpuId(), sku.getId());
        rebuildSkuIndexQuietly();
        return toSkuVO(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSkuVO updateSku(Long skuId, AdminSkuRequest request) {
        AdminSku existing = requireSku(skuId);
        requireSpu(request.getSpuId());
        AdminSku update = AdminSku.builder()
                .id(skuId)
                .spuId(request.getSpuId())
                .skuName(request.getSkuName())
                .skuCode(request.getSkuCode())
                .price(request.getPrice())
                .stock(request.getStock())
                .coverImg(request.getCoverImg())
                .build();
        try {
            adminSkuMapper.updateById(update);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU编码已存在");
        }
        cleanProductCache(existing.getSpuId(), skuId);
        cleanProductCache(request.getSpuId(), skuId);
        rebuildSkuIndexQuietly();
        return toSkuVO(requireSku(skuId));
    }

    @Override
    public PageVO<AdminOrderVO> listOrders(
            Long page, Long size, Integer status, String orderSn, Long userId, Integer orderType
    ) {
        Page<AdminOrder> orderPage = adminOrderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AdminOrder>()
                        .eq(status != null, AdminOrder::getStatus, status)
                        .eq(userId != null, AdminOrder::getUserId, userId)
                        .eq(orderType != null, AdminOrder::getOrderType, orderType)
                        .like(StringUtils.hasText(orderSn), AdminOrder::getOrderSn, orderSn)
                        .orderByDesc(AdminOrder::getCreateTime)
        );
        Map<String, List<AdminOrderItem>> itemMap = listOrderItems(orderPage.getRecords());
        List<AdminOrderVO> records = orderPage.getRecords().stream()
                .map(order -> toOrderVO(order, itemMap.getOrDefault(order.getOrderSn(), List.of())))
                .toList();
        return toPageVO(orderPage, records);
    }

    @Override
    public AdminOrderVO getAdminOrder(String orderSn) {
        AdminOrder order = adminOrderMapper.selectOne(
                new LambdaQueryWrapper<AdminOrder>().eq(AdminOrder::getOrderSn, orderSn)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "订单不存在");
        }
        List<AdminOrderItem> items = adminOrderItemMapper.selectList(
                new LambdaQueryWrapper<AdminOrderItem>().eq(AdminOrderItem::getOrderSn, orderSn)
        );
        return toOrderVO(order, items);
    }

    @Override
    public PageVO<AdminSeckillActivityVO> listSeckillActivities(Long page, Long size, String state, Long skuId) {
        Page<AdminSeckillActivity> activityPage = adminSeckillActivityMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AdminSeckillActivity>()
                        .eq(skuId != null, AdminSeckillActivity::getSkuId, skuId)
                        .orderByDesc(AdminSeckillActivity::getStartTime)
        );
        List<AdminSeckillActivityVO> records = activityPage.getRecords().stream()
                .map(this::toSeckillActivityVO)
                .filter(vo -> !StringUtils.hasText(state) || state.equals(vo.getState()))
                .toList();
        return new PageVO<>(activityPage.getCurrent(), activityPage.getSize(), activityPage.getTotal(),
                activityPage.getPages(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO createSeckillActivity(AdminSeckillActivityRequest request) {
        validateActivityTime(request.getStartTime(), request.getEndTime());
        requireSku(request.getSkuId());
        requireSpu(request.getSpuId());
        AdminSeckillActivity activity = AdminSeckillActivity.builder()
                .skuId(request.getSkuId())
                .spuId(request.getSpuId())
                .activityName(request.getActivityName())
                .seckillPrice(request.getSeckillPrice())
                .originalPrice(request.getOriginalPrice())
                .seckillStock(request.getSeckillStock())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_DISABLED))
                .version(0)
                .build();
        adminSeckillActivityMapper.insert(activity);
        return toSeckillActivityVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO updateSeckillActivity(Long id, AdminSeckillActivityRequest request) {
        requireSeckillActivity(id);
        validateActivityTime(request.getStartTime(), request.getEndTime());
        requireSku(request.getSkuId());
        requireSpu(request.getSpuId());
        AdminSeckillActivity update = AdminSeckillActivity.builder()
                .id(id)
                .skuId(request.getSkuId())
                .spuId(request.getSpuId())
                .activityName(request.getActivityName())
                .seckillPrice(request.getSeckillPrice())
                .originalPrice(request.getOriginalPrice())
                .seckillStock(request.getSeckillStock())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_DISABLED))
                .build();
        adminSeckillActivityMapper.updateById(update);
        clearSeckillRedis(request.getSkuId());
        return toSeckillActivityVO(requireSeckillActivity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO updateSeckillActivityStatus(Long id, Integer status) {
        AdminSeckillActivity existing = requireSeckillActivity(id);
        AdminSeckillActivity update = AdminSeckillActivity.builder()
                .id(id)
                .status(normalizeBinaryStatus(status, STATUS_DISABLED))
                .build();
        adminSeckillActivityMapper.updateById(update);
        if (STATUS_DISABLED == update.getStatus()) {
            clearSeckillRedis(existing.getSkuId());
        }
        return toSeckillActivityVO(requireSeckillActivity(id));
    }

    @Override
    public AdminSeckillActivityVO preheatSeckillActivity(Long id) {
        AdminSeckillActivity activity = requireSeckillActivity(id);
        if (activity.getStatus() == null || activity.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "活动未启用，不能预热");
        }
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_PREFIX + activity.getSkuId(),
                String.valueOf(activity.getSeckillStock()));
        stringRedisTemplate.delete(SECKILL_BOUGHT_PREFIX + activity.getSkuId());
        return toSeckillActivityVO(activity);
    }

    @Override
    public PageVO<AdminCouponVO> listCoupons(Long page, Long size, Integer status) {
        Page<AdminCoupon> couponPage = adminCouponMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AdminCoupon>()
                        .eq(status != null, AdminCoupon::getStatus, status)
                        .orderByDesc(AdminCoupon::getCreateTime)
        );
        List<AdminCouponVO> records = couponPage.getRecords().stream().map(this::toCouponVO).toList();
        return toPageVO(couponPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCouponVO createCoupon(AdminCouponRequest request) {
        validateActivityTime(request.getStartTime(), request.getEndTime());
        AdminCoupon coupon = AdminCoupon.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .minPoint(request.getMinPoint())
                .stock(request.getStock())
                .limitPerUser(request.getLimitPerUser())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_DISABLED))
                .version(0)
                .build();
        adminCouponMapper.insert(coupon);
        return toCouponVO(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCouponVO updateCoupon(Long id, AdminCouponRequest request) {
        requireCoupon(id);
        validateActivityTime(request.getStartTime(), request.getEndTime());
        AdminCoupon update = AdminCoupon.builder()
                .id(id)
                .name(request.getName())
                .amount(request.getAmount())
                .minPoint(request.getMinPoint())
                .stock(request.getStock())
                .limitPerUser(request.getLimitPerUser())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(normalizeBinaryStatus(request.getStatus(), STATUS_DISABLED))
                .build();
        adminCouponMapper.updateById(update);
        return toCouponVO(requireCoupon(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCouponVO updateCouponStatus(Long id, Integer status) {
        requireCoupon(id);
        AdminCoupon update = AdminCoupon.builder()
                .id(id)
                .status(normalizeBinaryStatus(status, STATUS_DISABLED))
                .build();
        adminCouponMapper.updateById(update);
        return toCouponVO(requireCoupon(id));
    }

    @Override
    public PageVO<AdminReviewVO> listReviews(Long page, Long size, Long spuId, String keyword) {
        Page<AdminReview> reviewPage = adminReviewMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AdminReview>()
                        .eq(spuId != null, AdminReview::getSpuId, spuId)
                        .like(StringUtils.hasText(keyword), AdminReview::getContent, keyword)
                        .orderByDesc(AdminReview::getCreateTime)
        );
        List<AdminReviewVO> records = reviewPage.getRecords().stream().map(this::toReviewVO).toList();
        return toPageVO(reviewPage, records);
    }

    @Override
    public void deleteReview(Long id) {
        if (adminReviewMapper.deleteById(id) <= 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "评价不存在或已删除");
        }
    }

    @Override
    public AdminRebuildIndexVO rebuildSkuIndex() {
        Result<AdminRebuildIndexVO> result = searchFeignClient.rebuildSkuIndex();
        if (!isSuccess(result)) {
            throw buildDownstreamException("重建搜索索引", result);
        }
        return result.getData();
    }

    private AdminSpu requireSpu(Long spuId) {
        AdminSpu spu = adminSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SPU不存在");
        }
        return spu;
    }

    private AdminSku requireSku(Long skuId) {
        AdminSku sku = adminSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU不存在");
        }
        return sku;
    }

    private AdminSeckillActivity requireSeckillActivity(Long id) {
        AdminSeckillActivity activity = adminSeckillActivityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "秒杀活动不存在");
        }
        return activity;
    }

    private AdminCoupon requireCoupon(Long id) {
        AdminCoupon coupon = adminCouponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "优惠券不存在");
        }
        return coupon;
    }

    private void insertSku(AdminSku sku) {
        try {
            adminSkuMapper.insert(sku);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "SKU编码已存在");
        }
    }

    private Map<Long, List<AdminSku>> listSkuMap(List<Long> spuIds) {
        if (CollectionUtils.isEmpty(spuIds)) {
            return Collections.emptyMap();
        }
        return adminSkuMapper.selectList(new LambdaQueryWrapper<AdminSku>()
                        .in(AdminSku::getSpuId, spuIds)
                        .orderByAsc(AdminSku::getId))
                .stream()
                .collect(Collectors.groupingBy(AdminSku::getSpuId));
    }

    private Map<String, List<AdminOrderItem>> listOrderItems(List<AdminOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        Set<String> orderSns = orders.stream().map(AdminOrder::getOrderSn).collect(Collectors.toSet());
        return adminOrderItemMapper.selectList(new LambdaQueryWrapper<AdminOrderItem>()
                        .in(AdminOrderItem::getOrderSn, orderSns)
                        .orderByAsc(AdminOrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(AdminOrderItem::getOrderSn));
    }

    private void validateActivityTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
    }

    private int normalizeBinaryStatus(Integer status, Integer defaultStatus) {
        int resolved = status == null ? defaultStatus : status;
        if (resolved != STATUS_DISABLED && resolved != STATUS_ENABLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status只能为0或1");
        }
        return resolved;
    }

    private void cleanProductCache(Long spuId, Long skuId) {
        cleanSpuCache(spuId);
        if (skuId != null) {
            stringRedisTemplate.delete(PRODUCT_SKU_CACHE_PREFIX + skuId);
        }
    }

    private void cleanSpuCache(Long spuId) {
        if (spuId != null) {
            stringRedisTemplate.delete(PRODUCT_SPU_CACHE_PREFIX + spuId);
        }
    }

    private void clearSeckillRedis(Long skuId) {
        if (skuId != null) {
            stringRedisTemplate.delete(SECKILL_STOCK_PREFIX + skuId);
            stringRedisTemplate.delete(SECKILL_BOUGHT_PREFIX + skuId);
        }
    }

    private void rebuildSkuIndexQuietly() {
        try {
            rebuildSkuIndex();
        } catch (Exception exception) {
            log.warn("商品变更后自动重建搜索索引失败，可在管理端系统工具中手动重建: {}", exception.getMessage());
        }
    }

    private AdminSpuVO toSpuVO(AdminSpu spu, List<AdminSku> skus) {
        return AdminSpuVO.builder()
                .spuId(spu.getId())
                .categoryId(spu.getCategoryId())
                .name(spu.getName())
                .description(spu.getDescription())
                .publishStatus(spu.getPublishStatus())
                .createTime(spu.getCreateTime())
                .updateTime(spu.getUpdateTime())
                .skuList(skus.stream().map(this::toSkuVO).toList())
                .build();
    }

    private AdminSkuVO toSkuVO(AdminSku sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int lockStock = sku.getLockStock() == null ? 0 : sku.getLockStock();
        return AdminSkuVO.builder()
                .skuId(sku.getId())
                .spuId(sku.getSpuId())
                .skuName(sku.getSkuName())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .stock(stock)
                .lockStock(lockStock)
                .availableStock(Math.max(stock - lockStock, 0))
                .saleCount(sku.getSaleCount())
                .coverImg(sku.getCoverImg())
                .createTime(sku.getCreateTime())
                .updateTime(sku.getUpdateTime())
                .build();
    }

    private AdminOrderVO toOrderVO(AdminOrder order, List<AdminOrderItem> items) {
        return AdminOrderVO.builder()
                .userId(order.getUserId())
                .orderSn(order.getOrderSn())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .payType(order.getPayType())
                .payTime(order.getPayTime())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .remark(order.getRemark())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverProvince(order.getReceiverProvince())
                .receiverCity(order.getReceiverCity())
                .receiverRegion(order.getReceiverRegion())
                .receiverDetailAddress(order.getReceiverDetailAddress())
                .deliveryCompany(order.getDeliveryCompany())
                .deliverySn(order.getDeliverySn())
                .deliveryTime(order.getDeliveryTime())
                .receiveTime(order.getReceiveTime())
                .createTime(order.getCreateTime())
                .items(items.stream().map(this::toOrderItemVO).toList())
                .build();
    }

    private AdminOrderItemVO toOrderItemVO(AdminOrderItem item) {
        return AdminOrderItemVO.builder()
                .skuId(item.getSkuId())
                .spuId(item.getSpuId())
                .skuName(item.getSkuName())
                .skuPic(item.getSkuPic())
                .skuPrice(item.getSkuPrice())
                .quantity(item.getSkuQuantity())
                .build();
    }

    private AdminSeckillActivityVO toSeckillActivityVO(AdminSeckillActivity activity) {
        return AdminSeckillActivityVO.builder()
                .id(activity.getId())
                .skuId(activity.getSkuId())
                .spuId(activity.getSpuId())
                .activityName(activity.getActivityName())
                .seckillPrice(activity.getSeckillPrice())
                .originalPrice(activity.getOriginalPrice())
                .seckillStock(activity.getSeckillStock())
                .remainingStock(resolveRemainingStock(activity))
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .status(activity.getStatus())
                .state(resolveActivityState(activity))
                .createTime(activity.getCreateTime())
                .build();
    }

    private Integer resolveRemainingStock(AdminSeckillActivity activity) {
        String stock = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_PREFIX + activity.getSkuId());
        if (StringUtils.hasText(stock)) {
            try {
                return Integer.valueOf(stock);
            } catch (NumberFormatException ignored) {
                log.warn("秒杀 Redis 库存值异常: skuId={}, value={}", activity.getSkuId(), stock);
            }
        }
        return activity.getSeckillStock();
    }

    private String resolveActivityState(AdminSeckillActivity activity) {
        if (activity.getStatus() == null || activity.getStatus() != STATUS_ENABLED) {
            return "DISABLED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return "NOT_STARTED";
        }
        if (now.isAfter(activity.getEndTime())) {
            return "ENDED";
        }
        return "ACTIVE";
    }

    private AdminCouponVO toCouponVO(AdminCoupon coupon) {
        return AdminCouponVO.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .amount(coupon.getAmount())
                .minPoint(coupon.getMinPoint())
                .stock(coupon.getStock())
                .limitPerUser(coupon.getLimitPerUser())
                .startTime(coupon.getStartTime())
                .endTime(coupon.getEndTime())
                .status(coupon.getStatus())
                .createTime(coupon.getCreateTime())
                .build();
    }

    private AdminReviewVO toReviewVO(AdminReview review) {
        return AdminReviewVO.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .orderSn(review.getOrderSn())
                .skuId(review.getSkuId())
                .spuId(review.getSpuId())
                .rating(review.getRating())
                .content(review.getContent())
                .likeCount(review.getLikeCount())
                .dislikeCount(review.getDislikeCount())
                .createTime(review.getCreateTime())
                .build();
    }

    private <E, V> PageVO<V> toPageVO(Page<E> page, List<V> records) {
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && ResultCode.SUCCESS.getCode().equals(result.getCode());
    }

    private BusinessException buildDownstreamException(String action, Result<?> result) {
        if (result == null) {
            return new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), action + "失败: 下游服务无响应");
        }
        return new BusinessException(result.getCode(), action + "失败: " + result.getMessage());
    }

    private String generateAdminJwt(Admin admin) {
        Map<String, Object> claims = new HashMap<>(2);
        claims.put("adminId", admin.getId());
        claims.put("username", admin.getUsername());

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(admin.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
