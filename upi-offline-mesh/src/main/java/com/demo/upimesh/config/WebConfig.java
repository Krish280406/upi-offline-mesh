package com.demo.upimesh.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.demo.upimesh.interceptor.BridgeRateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private BridgeRateLimitInterceptor rateLimitInterceptor;

    @Autowired(required = false)
    public WebConfig(BridgeRateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitInterceptor != null) {
            registry.addInterceptor(rateLimitInterceptor);
        }
    }
}