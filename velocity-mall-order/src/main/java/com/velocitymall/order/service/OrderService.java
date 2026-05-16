package com.velocitymall.order.service;

import com.velocitymall.common.model.dto.SeckillOrderDTO;
import com.velocitymall.common.model.vo.OrderDetailVO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.common.model.vo.SeckillResultVO;
import com.velocitymall.order.model.dto.SubmitOrderDTO;
import com.velocitymall.order.model.vo.OrderVO;

/**
 * 订单服务。
 */
public interface OrderService {

    /**
     * 提交订单。
     *
     * @param dto 提交订单参数
     * @return 订单信息
     */
    OrderVO submitOrder(SubmitOrderDTO dto);

    /**
     * Query current user's orders by page.
     *
     * @param page page number
     * @param size page size
     * @param status optional order status
     * @return paged order details
     */
    PageVO<OrderDetailVO> listMyOrders(Long page, Long size, Integer status);

    /**
     * Query current user's order detail.
     *
     * @param orderSn order number
     * @return order detail
     */
    OrderDetailVO getOrderDetail(String orderSn);

    /**
     * Query current user's seckill order generation result by SKU.
     *
     * @param skuId SKU ID
     * @return processing, success, or failed result
     */
    SeckillResultVO getSeckillResult(Long skuId);

    /**
     * Cancel a waiting-pay order.
     *
     * @param orderSn order number
     */
    void cancelOrder(String orderSn);

    /**
     * Mock a refund for a paid order.
     *
     * @param orderSn order number
     */
    void mockRefund(String orderSn);

    /**
     * Create an order from a successful seckill message.
     *
     * @param dto seckill order message
     */
    void createSeckillOrder(SeckillOrderDTO dto);

    /**
     * Mock a payment success callback.
     *
     * @param orderSn order number
     * @param payType payment type
     */
    void mockPaySuccess(String orderSn, Integer payType);

    /**
     * Check whether a user has completed (received) an order containing the SKU.
     *
     * @param userId user ID
     * @param orderSn order number
     * @param skuId SKU ID
     * @return true if the user has a completed order containing the SKU
     */
    Boolean checkPurchase(Long userId, String orderSn, Long skuId);

    /**
     * Mark a paid order as delivered (internal admin simulation).
     *
     * @param orderSn order number
     * @param deliveryCompany logistics company name
     * @param deliverySn delivery tracking number
     */
    void deliver(String orderSn, String deliveryCompany, String deliverySn);

    /**
     * Confirm receipt of a delivered order.
     *
     * @param orderSn order number
     */
    void confirmReceipt(String orderSn);
}
