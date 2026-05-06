package com.velocitymall.user.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.user.model.dto.UserLoginDTO;
import com.velocitymall.user.model.dto.UserRegisterDTO;
import com.velocitymall.user.model.vo.LoginVO;
import com.velocitymall.user.model.vo.UserInfoVO;
import com.velocitymall.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return userService.login(dto);
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser() {
        return userService.getCurrentUser();
    }
}
