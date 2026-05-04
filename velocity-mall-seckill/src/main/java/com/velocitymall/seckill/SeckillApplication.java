package com.velocitymall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 极速商城秒杀微服务启动类。
 */
@EnableScheduling
@SpringBootApplication(
        scanBasePackages = "com.velocitymall",
        exclude = DataSourceAutoConfiguration.class
)
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
