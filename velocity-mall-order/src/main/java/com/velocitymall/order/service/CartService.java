package com.velocitymall.order.service;

import com.velocitymall.common.model.dto.OrderItemDTO;
import com.velocitymall.order.model.dto.CartItemDTO;
import com.velocitymall.order.model.vo.CartItemVO;
import java.util.List;

/**
 * Cart service.
 */
public interface CartService {

    /**
     * Add a SKU to cart.
     *
     * @param dto cart item request
     */
    void addItem(CartItemDTO dto);

    /**
     * List current user's cart items.
     *
     * @return cart item list
     */
    List<CartItemVO> listItems();

    /**
     * Remove a SKU from current user's cart.
     *
     * @param skuId SKU ID
     */
    void removeItem(Long skuId);

    /**
     * Get checked order items from cart.
     *
     * @param userId user ID
     * @param skuIds checked SKU IDs
     * @return order item list
     */
    List<OrderItemDTO> getCheckedItems(Long userId, List<Long> skuIds);

    /**
     * Remove checked cart items after order committed.
     *
     * @param userId user ID
     * @param skuIds checked SKU IDs
     */
    void removeItems(Long userId, List<Long> skuIds);
}
