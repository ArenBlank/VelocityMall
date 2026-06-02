package com.velocitymall.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.seckill.entity.SeckillActivity;
import com.velocitymall.seckill.mapper.SeckillActivityMapper;
import com.velocitymall.seckill.model.vo.SeckillActivityVO;
import com.velocitymall.seckill.service.SeckillActivityService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Database-backed flash-sale activity service.
 */
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private static final int STATUS_ENABLED = 1;

    private static final String STATE_NOT_STARTED = "NOT_STARTED";

    private static final String STATE_ACTIVE = "ACTIVE";

    private static final String STATE_ENDED = "ENDED";

    private static final String STATE_DISABLED = "DISABLED";

    private static String stockKey(Object skuId) { return "velocitymall:seckill:{" + skuId + "}:stock"; }

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SeckillActivityMapper seckillActivityMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<SeckillActivityVO> listDisplayActivities() {
        LocalDateTime now = LocalDateTime.now();
        return seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, STATUS_ENABLED)
                        .gt(SeckillActivity::getEndTime, now)
                        .orderByAsc(SeckillActivity::getStartTime)
                        .orderByAsc(SeckillActivity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SeckillActivityVO getDisplayActivityBySkuId(Long skuId) {
        return toVO(requireDisplayActivity(skuId));
    }

    @Override
    public SeckillActivity requireActiveActivity(Long skuId) {
        SeckillActivity activity = requireDisplayActivity(skuId);
        LocalDateTime now = LocalDateTime.now();
        if (!Integer.valueOf(STATUS_ENABLED).equals(activity.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "活动未启用");
        }
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "活动未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "活动已结束");
        }
        if (activity.getSeckillStock() == null || activity.getSeckillStock() <= 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "活动库存不足");
        }
        return activity;
    }

    @Override
    public List<SeckillActivity> listPreheatActivities() {
        LocalDateTime now = LocalDateTime.now();
        return seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, STATUS_ENABLED)
                .gt(SeckillActivity::getEndTime, now)
                .gt(SeckillActivity::getSeckillStock, 0)
                .orderByAsc(SeckillActivity::getStartTime));
    }

    private SeckillActivity requireDisplayActivity(Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID不能为空");
        }
        SeckillActivity activity = seckillActivityMapper.selectOne(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getSkuId, skuId)
                .orderByDesc(SeckillActivity::getStartTime)
                .last("LIMIT 1"));
        if (activity == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "秒杀活动不存在");
        }
        return activity;
    }

    private SeckillActivityVO toVO(SeckillActivity activity) {
        return SeckillActivityVO.builder()
                .activityId(activity.getId())
                .skuId(activity.getSkuId())
                .spuId(activity.getSpuId())
                .activityName(activity.getActivityName())
                .seckillPrice(activity.getSeckillPrice())
                .originalPrice(activity.getOriginalPrice())
                .seckillStock(activity.getSeckillStock())
                .remainingStock(resolveRemainingStock(activity))
                .startTime(format(activity.getStartTime()))
                .endTime(format(activity.getEndTime()))
                .status(activity.getStatus())
                .state(resolveState(activity))
                .build();
    }

    private Integer resolveRemainingStock(SeckillActivity activity) {
        String stockValue = stringRedisTemplate.opsForValue().get(stockKey(activity.getSkuId()));
        if (!StringUtils.hasText(stockValue)) {
            return activity.getSeckillStock();
        }
        try {
            return Integer.valueOf(stockValue);
        } catch (NumberFormatException exception) {
            return activity.getSeckillStock();
        }
    }

    private String resolveState(SeckillActivity activity) {
        if (!Integer.valueOf(STATUS_ENABLED).equals(activity.getStatus())) {
            return STATE_DISABLED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            return STATE_NOT_STARTED;
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            return STATE_ENDED;
        }
        return STATE_ACTIVE;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : ISO_DATE_TIME.format(value);
    }
}
