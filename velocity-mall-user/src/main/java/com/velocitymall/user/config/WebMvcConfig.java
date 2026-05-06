package com.velocitymall.user.config;

import com.velocitymall.common.interceptor.UserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserInterceptor userInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/api/v1/users/**")
                .excludePathPatterns(
                        "/api/v1/users/register",
                        "/api/v1/users/login",
                        "/api/v1/users/inner/**"
                );
    }
}
