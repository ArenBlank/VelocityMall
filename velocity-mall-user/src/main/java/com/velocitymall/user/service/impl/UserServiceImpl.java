package com.velocitymall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.common.context.UserContext;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.user.entity.User;
import com.velocitymall.user.mapper.UserMapper;
import com.velocitymall.user.model.dto.UserLoginDTO;
import com.velocitymall.user.model.dto.UserRegisterDTO;
import com.velocitymall.user.model.vo.LoginVO;
import com.velocitymall.user.model.vo.UserInfoVO;
import com.velocitymall.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int USER_ENABLED = 1;

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public Result<Void> register(UserRegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        User user = User.builder()
                .username(dto.getUsername())
                .password(encodedPassword)
                .nickname(dto.getUsername())
                .status(USER_ENABLED)
                .build();
        userMapper.insert(user);

        log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());
        return Result.success();
    }

    @Override
    public Result<LoginVO> login(UserLoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != USER_ENABLED) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = buildJwt(user);

        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(userInfo);

        log.info("用户登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
        return Result.success(loginVO);
    }

    @Override
    public Result<UserInfoVO> getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());

        return Result.success(vo);
    }

    private String buildJwt(User user) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
