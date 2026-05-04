package com.velocitymall.order.service.impl;

import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.dto.OrderItemDTO;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.order.client.ProductFeignClient;
import com.velocitymall.order.model.dto.CartItemDTO;
import com.velocitymall.order.model.vo.CartItemVO;
import com.velocitymall.order.model.vo.SkuVO;
import com.velocitymall.order.service.CartService;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Redis Hash backed cart service implementation.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "velocitymall:cart:";

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;

    private final StringRedisTemplate stringRedisTemplate;

    private final ProductFeignClient productFeignClient;

    @Override
    public void addItem(CartItemDTO dto) {
        Long userId = getCurrentUserId();
        getSkuSnapshot(dto.getSkuId());
        stringRedisTemplate.opsForHash().increment(cartKey(userId), String.valueOf(dto.getSkuId()), dto.getQuantity());
    }

    @Override
    public List<CartItemVO> listItems() {
        Long userId = getCurrentUserId();
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) {
            return List.of();
        }
        return entries.entrySet().stream()
                .map(entry -> toCartItemVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public void removeItem(Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID非法");
        }
        Long userId = getCurrentUserId();
        stringRedisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(skuId));
    }

    @Override
    public List<OrderItemDTO> getCheckedItems(Long userId, List<Long> skuIds) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户上下文不存在");
        }
        if (CollectionUtils.isEmpty(skuIds)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结算 SKU 列表不能为空");
        }
        Set<Long> distinctSkuIds = new LinkedHashSet<>(skuIds);
        return distinctSkuIds.stream()
                .map(skuId -> {
                    Integer quantity = getCartQuantity(userId, skuId);
                    return new OrderItemDTO(skuId, quantity);
                })
                .toList();
    }

    @Override
    public void removeItems(Long userId, List<Long> skuIds) {
        if (userId == null || userId <= 0 || CollectionUtils.isEmpty(skuIds)) {
            return;
        }
        Object[] hashKeys = skuIds.stream()
                .distinct()
                .map(String::valueOf)
                .toArray();
        if (hashKeys.length > 0) {
            stringRedisTemplate.opsForHash().delete(cartKey(userId), hashKeys);
        }
    }

    private CartItemVO toCartItemVO(Object rawSkuId, Object rawQuantity) {
        Long skuId = Long.valueOf(String.valueOf(rawSkuId));
        Integer quantity = Integer.valueOf(String.valueOf(rawQuantity));
        SkuVO skuVO = getSkuSnapshot(skuId);
        BigDecimal price = skuVO.getPrice() == null ? DEFAULT_AMOUNT : skuVO.getPrice();
        return CartItemVO.builder()
                .skuId(skuId)
                .skuName(skuVO.getSkuName())
                .price(price)
                .quantity(quantity)
                .availableStock(skuVO.getAvailableStock())
                .totalAmount(price.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private Integer getCartQuantity(Long userId, Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID非法");
        }
        Object quantityValue = stringRedisTemplate.opsForHash().get(cartKey(userId), String.valueOf(skuId));
        if (quantityValue == null || !StringUtils.hasText(String.valueOf(quantityValue))) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "购物车商品不存在，skuId: " + skuId);
        }
        Integer quantity = Integer.valueOf(String.valueOf(quantityValue));
        if (quantity <= 0) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "购物车商品数量异常，skuId: " + skuId);
        }
        return quantity;
    }

    private SkuVO getSkuSnapshot(Long skuId) {
        Result<SkuVO> skuResult = productFeignClient.getSkuById(skuId);
        if (skuResult == null || !ResultCode.SUCCESS.getCode().equals(skuResult.getCode()) || skuResult.getData() == null) {
            String message = skuResult == null || !StringUtils.hasText(skuResult.getMessage())
                    ? "获取商品快照失败"
                    : skuResult.getMessage();
            throw new BusinessException(ResultCode.BIZ_WARNING, message);
        }
        return skuResult.getData();
    }

    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户上下文不存在");
        }
        return currentUserId;
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
}
