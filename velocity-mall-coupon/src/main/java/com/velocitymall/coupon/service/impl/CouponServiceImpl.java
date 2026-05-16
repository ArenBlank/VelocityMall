package com.velocitymall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.CouponUseDTO;
import com.velocitymall.common.model.vo.CouponUseVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.coupon.entity.Coupon;
import com.velocitymall.coupon.entity.CouponHistory;
import com.velocitymall.coupon.mapper.CouponHistoryMapper;
import com.velocitymall.coupon.mapper.CouponMapper;
import com.velocitymall.coupon.model.vo.CouponVO;
import com.velocitymall.coupon.model.vo.UserCouponVO;
import com.velocitymall.coupon.service.CouponService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 优惠券业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String CLAIM_KEY_PREFIX = "velocitymall:coupon:claim:";

    private static final int ENABLED_STATUS = 1;

    private static final int UNUSED_STATUS = 0;

    private static final int MAX_STOCK_DEDUCT_RETRY = 3;

    private final CouponMapper couponMapper;

    private final CouponHistoryMapper couponHistoryMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void claimCoupon(Long couponId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Coupon coupon = couponMapper.selectById(couponId);
        validateCouponAvailable(coupon);

        String claimKey = buildClaimKey(couponId, userId);
        Long claimCount = incrementClaimCount(claimKey, coupon);
        if (claimCount > coupon.getLimitPerUser()) {
            rollbackClaimCount(claimKey);
            throw new BusinessException("已达到限领数量");
        }

        try {
            deductStockAndSaveHistory(couponId, userId);
        } catch (BusinessException exception) {
            rollbackClaimCount(claimKey);
            throw exception;
        } catch (DataAccessException exception) {
            rollbackClaimCount(claimKey);
            log.error("优惠券领取数据库异常，已回滚 Redis 计数. couponId: {}, userId: {}", couponId, userId, exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "优惠券领取失败，请稍后重试");
        } catch (RuntimeException exception) {
            rollbackClaimCount(claimKey);
            log.error("优惠券领取未知异常，已回滚 Redis 计数. couponId: {}, userId: {}", couponId, userId, exception);
            throw exception;
        }
    }

    @Override
    public PageVO<CouponVO> listAvailableCoupons(Long page, Long size) {
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> couponPage = couponMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)),
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, ENABLED_STATUS)
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now)
                        .gt(Coupon::getStock, 0)
                        .orderByAsc(Coupon::getEndTime)
        );
        List<CouponVO> records = couponPage.getRecords().stream()
                .map(this::toCouponVO)
                .toList();
        return new PageVO<>(
                couponPage.getCurrent(),
                couponPage.getSize(),
                couponPage.getTotal(),
                couponPage.getPages(),
                records
        );
    }

    @Override
    public PageVO<UserCouponVO> listMyCoupons(Long page, Long size, Integer useStatus) {
        Long userId = getRequiredUserId();
        Page<CouponHistory> historyPage = couponHistoryMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)),
                new LambdaQueryWrapper<CouponHistory>()
                        .eq(CouponHistory::getUserId, userId)
                        .eq(useStatus != null, CouponHistory::getUseStatus, useStatus)
                        .orderByDesc(CouponHistory::getClaimTime)
        );
        List<CouponHistory> histories = historyPage.getRecords();
        if (CollectionUtils.isEmpty(histories)) {
            return new PageVO<>(historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal(),
                    historyPage.getPages(), List.of());
        }

        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(
                        histories.stream().map(CouponHistory::getCouponId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        List<UserCouponVO> records = histories.stream()
                .map(history -> toUserCouponVO(history, couponMap.get(history.getCouponId())))
                .toList();
        return new PageVO<>(historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal(),
                historyPage.getPages(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponUseVO useCoupon(CouponUseDTO dto) {
        validateUseRequest(dto);
        CouponHistory history = couponHistoryMapper.selectOne(new LambdaQueryWrapper<CouponHistory>()
                .eq(CouponHistory::getId, dto.getCouponHistoryId())
                .eq(CouponHistory::getUserId, dto.getUserId())
                .last("LIMIT 1"));
        if (history == null) {
            throw new BusinessException("Coupon not found for current user");
        }
        if (!Integer.valueOf(UNUSED_STATUS).equals(history.getUseStatus())) {
            throw new BusinessException("Coupon has already been used");
        }

        Coupon coupon = couponMapper.selectById(history.getCouponId());
        validateCouponUsableForOrder(coupon, dto.getOrderAmount());

        BigDecimal discountAmount = coupon.getAmount().min(dto.getOrderAmount());
        BigDecimal payAmount = dto.getOrderAmount().subtract(discountAmount);
        int affectedRows = couponHistoryMapper.markUsed(dto.getCouponHistoryId(), dto.getUserId(), dto.getOrderSn());
        if (affectedRows == 0) {
            throw new BusinessException("Coupon status changed, please refresh and retry");
        }
        return new CouponUseVO(history.getId(), history.getCouponId(), discountAmount, payAmount);
    }

    @Override
    public void releaseUsedCoupon(String orderSn) {
        if (!StringUtils.hasText(orderSn)) {
            return;
        }
        int affectedRows = couponHistoryMapper.releaseByOrderSn(orderSn);
        if (affectedRows > 0) {
            log.info("Released used coupon for order. orderSn: {}, rows: {}", orderSn, affectedRows);
        }
    }

    private void deductStockAndSaveHistory(Long couponId, Long userId) {
        for (int retry = 1; retry <= MAX_STOCK_DEDUCT_RETRY; retry++) {
            Coupon latestCoupon = couponMapper.selectById(couponId);
            validateCouponAvailable(latestCoupon);

            int affectedRows = couponMapper.decreaseStock(couponId, latestCoupon.getVersion());
            if (affectedRows == 1) {
                CouponHistory history = CouponHistory.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .claimTime(LocalDateTime.now())
                        .useStatus(UNUSED_STATUS)
                        .build();
                couponHistoryMapper.insert(history);
                return;
            }

            log.warn("优惠券库存乐观锁冲突，准备重试. couponId: {}, userId: {}, retry: {}",
                    couponId, userId, retry);
        }
        throw new BusinessException("优惠券已被抢空或系统繁忙");
    }

    private Long incrementClaimCount(String claimKey, Coupon coupon) {
        Long count = stringRedisTemplate.opsForValue().increment(claimKey);
        if (count == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "优惠券领取计数失败");
        }

        if (count == 1L) {
            Duration ttl = Duration.between(LocalDateTime.now(), coupon.getEndTime().plusDays(1));
            if (ttl.isZero() || ttl.isNegative()) {
                rollbackClaimCount(claimKey);
                throw new BusinessException("优惠券已过期");
            }
            Boolean expireResult = stringRedisTemplate.expire(claimKey, ttl);
            if (Boolean.FALSE.equals(expireResult)) {
                log.warn("优惠券领取计数 TTL 设置失败. key: {}", claimKey);
            }
        }
        return count;
    }

    private void rollbackClaimCount(String claimKey) {
        try {
            Long count = stringRedisTemplate.opsForValue().decrement(claimKey);
            if (count != null && count <= 0) {
                stringRedisTemplate.delete(claimKey);
            }
        } catch (RuntimeException exception) {
            log.error("回滚优惠券 Redis 领取计数失败，需关注用户短期少领风险. key: {}", claimKey, exception);
        }
    }

    private void validateCouponAvailable(Coupon coupon) {
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != ENABLED_STATUS) {
            throw new BusinessException("优惠券未启用");
        }
        if (coupon.getLimitPerUser() == null || coupon.getLimitPerUser() < 1) {
            throw new BusinessException("优惠券限领配置异常");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() == null || coupon.getEndTime() == null
                || now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("不在优惠券领取时间内");
        }
        if (coupon.getStock() == null || coupon.getStock() <= 0) {
            throw new BusinessException("优惠券已被抢空");
        }
        if (coupon.getVersion() == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "优惠券版本号异常");
        }
    }

    private void validateCouponUsableForOrder(Coupon coupon, BigDecimal orderAmount) {
        if (coupon == null) {
            throw new BusinessException("Coupon definition does not exist");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != ENABLED_STATUS) {
            throw new BusinessException("Coupon is disabled");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() == null || coupon.getEndTime() == null
                || now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("Coupon is not in valid time range");
        }
        BigDecimal minPoint = coupon.getMinPoint() == null ? BigDecimal.ZERO : coupon.getMinPoint();
        if (orderAmount.compareTo(minPoint) < 0) {
            throw new BusinessException("Order amount does not meet coupon threshold");
        }
    }

    private void validateUseRequest(CouponUseDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getUserId() <= 0
                || dto.getCouponHistoryId() == null || dto.getCouponHistoryId() <= 0
                || !StringUtils.hasText(dto.getOrderSn())
                || dto.getOrderAmount() == null || dto.getOrderAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Invalid coupon usage request");
        }
    }

    private Long getRequiredUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private long normalizePage(Long page) {
        return page == null || page < 1 ? 1L : page;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, 50L);
    }

    private CouponVO toCouponVO(Coupon coupon) {
        return new CouponVO(
                coupon.getId(),
                coupon.getName(),
                coupon.getAmount(),
                coupon.getMinPoint(),
                coupon.getStock(),
                coupon.getLimitPerUser(),
                coupon.getStartTime(),
                coupon.getEndTime(),
                coupon.getStatus()
        );
    }

    private UserCouponVO toUserCouponVO(CouponHistory history, Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        boolean available = coupon != null
                && Integer.valueOf(UNUSED_STATUS).equals(history.getUseStatus())
                && Integer.valueOf(ENABLED_STATUS).equals(coupon.getStatus())
                && coupon.getStartTime() != null
                && coupon.getEndTime() != null
                && !now.isBefore(coupon.getStartTime())
                && !now.isAfter(coupon.getEndTime());
        return new UserCouponVO(
                history.getId(),
                history.getCouponId(),
                coupon == null ? "Unknown coupon" : coupon.getName(),
                coupon == null ? BigDecimal.ZERO : coupon.getAmount(),
                coupon == null ? BigDecimal.ZERO : coupon.getMinPoint(),
                history.getUseStatus(),
                history.getClaimTime(),
                history.getUseTime(),
                history.getOrderSn(),
                coupon == null ? null : coupon.getStartTime(),
                coupon == null ? null : coupon.getEndTime(),
                coupon == null ? null : coupon.getStatus(),
                available
        );
    }

    private String buildClaimKey(Long couponId, Long userId) {
        return CLAIM_KEY_PREFIX + couponId + ":" + userId;
    }
}
