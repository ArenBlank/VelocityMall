package com.velocitymall.coupon.service.impl;

import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.coupon.entity.Coupon;
import com.velocitymall.coupon.entity.CouponHistory;
import com.velocitymall.coupon.mapper.CouponHistoryMapper;
import com.velocitymall.coupon.mapper.CouponMapper;
import com.velocitymall.coupon.service.CouponService;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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

    private String buildClaimKey(Long couponId, Long userId) {
        return CLAIM_KEY_PREFIX + couponId + ":" + userId;
    }
}
