package com.velocitymall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 极速商城商品微服务启动类。
 */
@SpringBootApplication(scanBasePackages = "com.velocitymall")
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
