package com.velocitymall.seckill.service;

import com.velocitymall.seckill.entity.SeckillActivity;
import com.velocitymall.seckill.model.vo.SeckillActivityVO;
import java.util.List;

/**
 * Flash-sale activity service.
 */
public interface SeckillActivityService {

    List<SeckillActivityVO> listDisplayActivities();

    SeckillActivityVO getDisplayActivityBySkuId(Long skuId);

    SeckillActivity requireActiveActivity(Long skuId);

    List<SeckillActivity> listPreheatActivities();
}
