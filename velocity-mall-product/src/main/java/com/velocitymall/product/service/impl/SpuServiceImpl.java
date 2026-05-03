package com.velocitymall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.entity.Sku;
import com.velocitymall.product.entity.Spu;
import com.velocitymall.product.mapper.SkuMapper;
import com.velocitymall.product.mapper.SpuMapper;
import com.velocitymall.product.model.vo.SkuVO;
import com.velocitymall.product.model.vo.SpuDetailVO;
import com.velocitymall.product.service.SpuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商品 SPU 服务实现。
 */
@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {

    private final SpuMapper spuMapper;

    private final SkuMapper skuMapper;

    @Override
    public SpuDetailVO getSpuDetail(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "商品不存在或已下架");
        }

        List<SkuVO> skuList = skuMapper.selectList(
                        Wrappers.<Sku>lambdaQuery()
                                .eq(Sku::getSpuId, spuId)
                )
                .stream()
                .map(this::convertSkuVO)
                .toList();

        return SpuDetailVO.builder()
                .spuId(spu.getId())
                .categoryId(spu.getCategoryId())
                .name(spu.getName())
                .description(spu.getDescription())
                .publishStatus(spu.getPublishStatus())
                .skuList(skuList)
                .build();
    }

    private SkuVO convertSkuVO(Sku sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int lockStock = sku.getLockStock() == null ? 0 : sku.getLockStock();
        return SkuVO.builder()
                .skuId(sku.getId())
                .skuName(sku.getSkuName())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .availableStock(Math.max(stock - lockStock, 0))
                .coverImg(sku.getCoverImg())
                .build();
    }
}
