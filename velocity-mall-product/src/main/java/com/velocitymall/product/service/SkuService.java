package com.velocitymall.product.service;

import com.velocitymall.common.model.dto.OrderRefundDTO;
import com.velocitymall.common.model.dto.PaymentSuccessDTO;
import com.velocitymall.common.model.dto.ProductSkuSearchDTO;
import com.velocitymall.common.model.dto.StockLockDTO;
import com.velocitymall.common.model.vo.PageVO;
import com.velocitymall.product.model.dto.LockStockDTO;
import com.velocitymall.product.model.dto.UnlockStockDTO;
import com.velocitymall.product.model.dto.UpdateSkuDTO;
import com.velocitymall.product.model.vo.SkuVO;

/**
 * SKU service.
 */
public interface SkuService {

    /**
     * Query SKU detail.
     *
     * @param skuId SKU ID
     * @return SKU detail
     */
    SkuVO getSkuById(Long skuId);

    /**
     * Query SKU source data for search synchronization.
     *
     * @param skuId SKU ID
     * @return SKU search source data
     */
    ProductSkuSearchDTO getSkuSearchSource(Long skuId);

    /**
     * Page query published SKU source data for search index rebuild.
     *
     * @param page page number
     * @param size page size
     * @return paged SKU source data
     */
    PageVO<ProductSkuSearchDTO> listPublishedSearchSources(Long page, Long size);

    /**
     * Update SKU basic information and synchronize search index after commit.
     *
     * @param skuId SKU ID
     * @param dto update request
     */
    void updateSkuBasicInfo(Long skuId, UpdateSkuDTO dto);

    /**
     * Lock stock for a normal order.
     *
     * @param dto lock stock request
     */
    void lockStock(LockStockDTO dto);

    /**
     * Unlock stock for a canceled or timeout normal order.
     *
     * @param dto unlock stock request
     */
    void unlockStock(UnlockStockDTO dto);

    /**
     * Deduct physical stock after payment success.
     *
     * @param dto payment success event
     * @return true if the message is consumed successfully
     */
    boolean deductPhysicalStock(PaymentSuccessDTO dto);

    /**
     * Roll back physical stock after order refund.
     *
     * @param dto order refund event
     * @return true if the message is consumed successfully
     */
    boolean refundPhysicalStock(OrderRefundDTO dto);

    /**
     * Batch lock physical stock for normal order checkout.
     *
     * @param dto batch stock lock request
     */
    void lockPhysicalStock(StockLockDTO dto);

    /**
     * Batch unlock physical stock for normal order cancellation or compensation.
     *
     * @param dto batch stock unlock request
     */
    void unlockPhysicalStock(StockLockDTO dto);
}
