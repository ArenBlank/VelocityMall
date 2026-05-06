package com.velocitymall.user.service;

import com.velocitymall.common.result.Result;
import com.velocitymall.user.model.dto.UserLoginDTO;
import com.velocitymall.user.model.dto.UserRegisterDTO;
import com.velocitymall.user.model.vo.LoginVO;
import com.velocitymall.user.model.vo.UserInfoVO;

public interface UserService {

    Result<Void> register(UserRegisterDTO dto);

    Result<LoginVO> login(UserLoginDTO dto);

    Result<UserInfoVO> getCurrentUser();
}
