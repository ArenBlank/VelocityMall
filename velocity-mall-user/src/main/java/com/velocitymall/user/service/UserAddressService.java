package com.velocitymall.user.service;

import com.velocitymall.common.result.Result;
import com.velocitymall.user.model.dto.UserAddressSaveDTO;
import com.velocitymall.user.model.vo.UserAddressVO;
import java.util.List;

public interface UserAddressService {

    Result<UserAddressVO> create(UserAddressSaveDTO dto);

    Result<UserAddressVO> update(Long id, UserAddressSaveDTO dto);

    Result<Void> delete(Long id);

    Result<List<UserAddressVO>> listByUser();

    Result<UserAddressVO> getById(Long id, Long userId);
}
