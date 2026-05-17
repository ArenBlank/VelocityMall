package com.velocitymall.review.config;

import com.velocitymall.common.interceptor.UserInterceptor;
import com.velocitymall.review.interceptor.OptionalUserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 商品评价服务 Web MVC 配置。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserInterceptor userInterceptor;

    private final OptionalUserInterceptor optionalUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(optionalUserInterceptor)
                .addPathPatterns(
                        "/api/v1/reviews/products/**",
                        "/api/v1/reviews/*/replies",
                        "/api/v1/reviews/*/replies/**"
                );

        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/api/v1/reviews/**")
                .excludePathPatterns(
                        "/api/v1/reviews/products/**",
                        "/api/v1/reviews/*/replies",
                        "/api/v1/reviews/*/replies/**",
                        "/error"
                );
    }
}
