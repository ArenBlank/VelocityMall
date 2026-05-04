package com.velocitymall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.product.entity.MqConsumeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ consumer idempotency log mapper.
 */
@Mapper
public interface MqConsumeLogMapper extends BaseMapper<MqConsumeLog> {
}
