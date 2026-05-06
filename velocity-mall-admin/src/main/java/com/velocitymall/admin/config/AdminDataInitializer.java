package com.velocitymall.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velocitymall.admin.entity.Admin;
import com.velocitymall.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时检查并创建默认管理员账号（admin / 123456）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Long count = adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin")
        );
        if (count != null && count > 0) {
            log.info("默认管理员账号已存在，跳过初始化");
            return;
        }

        Admin admin = Admin.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .realName("系统管理员")
                .status(1)
                .build();
        adminMapper.insert(admin);
        log.info("默认管理员账号创建成功: admin / 123456");
    }
}
