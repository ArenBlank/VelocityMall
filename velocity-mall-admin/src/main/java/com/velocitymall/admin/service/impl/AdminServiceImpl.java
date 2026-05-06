package com.velocitymall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.admin.client.OrderFeignClient;
import com.velocitymall.admin.client.ProductFeignClient;
import com.velocitymall.admin.entity.Admin;
import com.velocitymall.admin.mapper.AdminMapper;
import com.velocitymall.admin.model.vo.AdminLoginVO;
import com.velocitymall.admin.service.AdminService;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public AdminLoginVO login(String username, String password) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, username)
        );
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = generateAdminJwt(admin);
        log.info("管理员登录成功: {}", admin.getUsername());
        return AdminLoginVO.builder()
                .token(token)
                .adminId(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .build();
    }

    @Override
    public void deliverOrder(String orderSn, String deliveryCompany, String deliverySn) {
        var result = orderFeignClient.deliver(orderSn, deliveryCompany, deliverySn);
        if (!isSuccess(result)) {
            throw buildDownstreamException("发货", result);
        }
        log.info("管理员发货成功: orderSn={}, company={}, tracking={}", orderSn, deliveryCompany, deliverySn);
    }

    @Override
    public void publishSpu(Long spuId) {
        var result = productFeignClient.publishSpu(spuId);
        if (!isSuccess(result)) {
            throw buildDownstreamException("上架", result);
        }
        log.info("管理员上架 SPU: {}", spuId);
    }

    @Override
    public void unpublishSpu(Long spuId) {
        var result = productFeignClient.unpublishSpu(spuId);
        if (!isSuccess(result)) {
            throw buildDownstreamException("下架", result);
        }
        log.info("管理员下架 SPU: {}", spuId);
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && ResultCode.SUCCESS.getCode().equals(result.getCode());
    }

    private BusinessException buildDownstreamException(String action, Result<?> result) {
        if (result == null) {
            return new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), action + "失败: 下游服务无响应");
        }
        return new BusinessException(result.getCode(), action + "失败: " + result.getMessage());
    }

    private String generateAdminJwt(Admin admin) {
        Map<String, Object> claims = new HashMap<>(2);
        claims.put("adminId", admin.getId());
        claims.put("username", admin.getUsername());

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(admin.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
