package com.velocitymall.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 极速商城秒杀微服务启动类。
 */
@EnableScheduling
@MapperScan("com.velocitymall.seckill.mapper")
@SpringBootApplication(scanBasePackages = "com.velocitymall")
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
