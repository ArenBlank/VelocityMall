package com.velocitymall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 极速商城订单微服务启动类。
 */
@EnableFeignClients(basePackages = "com.velocitymall.order.client")
@SpringBootApplication(scanBasePackages = "com.velocitymall")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
