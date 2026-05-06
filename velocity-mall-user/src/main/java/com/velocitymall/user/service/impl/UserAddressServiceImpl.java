package com.velocitymall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.user.entity.UserAddress;
import com.velocitymall.user.mapper.UserAddressMapper;
import com.velocitymall.user.model.dto.UserAddressSaveDTO;
import com.velocitymall.user.model.vo.UserAddressVO;
import com.velocitymall.user.service.UserAddressService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private static final int IS_DEFAULT = 1;

    private static final int NOT_DEFAULT = 0;

    private final UserAddressMapper userAddressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UserAddressVO> create(UserAddressSaveDTO dto) {
        Long userId = getCurrentUserId();

        clearDefaultIfNeeded(userId, dto.getIsDefault());

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .province(dto.getProvince())
                .city(dto.getCity())
                .region(dto.getRegion())
                .detailAddress(dto.getDetailAddress())
                .isDefault(dto.getIsDefault() != null && dto.getIsDefault() == IS_DEFAULT ? IS_DEFAULT : NOT_DEFAULT)
                .build();
        userAddressMapper.insert(address);

        log.info("用户新增收货地址成功, userId: {}, addressId: {}", userId, address.getId());
        return Result.success(toVO(address));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UserAddressVO> update(Long id, UserAddressSaveDTO dto) {
        Long userId = getCurrentUserId();
        UserAddress address = getUserAddressOrThrow(id, userId);

        clearDefaultIfNeeded(userId, dto.getIsDefault());

        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setRegion(dto.getRegion());
        address.setDetailAddress(dto.getDetailAddress());
        address.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() == IS_DEFAULT ? IS_DEFAULT : NOT_DEFAULT);
        userAddressMapper.updateById(address);

        log.info("用户修改收货地址成功, userId: {}, addressId: {}", userId, id);
        return Result.success(toVO(address));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        Long userId = getCurrentUserId();
        getUserAddressOrThrow(id, userId);
        userAddressMapper.deleteById(id);

        log.info("用户删除收货地址成功, userId: {}, addressId: {}", userId, id);
        return Result.success();
    }

    @Override
    public Result<List<UserAddressVO>> listByUser() {
        Long userId = getCurrentUserId();
        List<UserAddress> addresses = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getCreateTime)
        );
        List<UserAddressVO> voList = addresses.stream().map(this::toVO).toList();
        return Result.success(voList);
    }

    @Override
    public Result<UserAddressVO> getById(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectOne(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
        );
        if (address == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "收货地址不存在");
        }
        return Result.success(toVO(address));
    }

    private UserAddress getUserAddressOrThrow(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectOne(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
        );
        if (address == null) {
            throw new BusinessException(ResultCode.BIZ_WARNING, "收货地址不存在");
        }
        return address;
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void clearDefaultIfNeeded(Long userId, Integer isDefault) {
        if (isDefault != null && isDefault == IS_DEFAULT) {
            userAddressMapper.update(null,
                    new LambdaUpdateWrapper<UserAddress>()
                            .eq(UserAddress::getUserId, userId)
                            .eq(UserAddress::getIsDefault, IS_DEFAULT)
                            .set(UserAddress::getIsDefault, NOT_DEFAULT)
            );
        }
    }

    private UserAddressVO toVO(UserAddress address) {
        UserAddressVO vo = new UserAddressVO();
        vo.setId(address.getId());
        vo.setUserId(address.getUserId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setRegion(address.getRegion());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setIsDefault(address.getIsDefault());
        return vo;
    }
}
