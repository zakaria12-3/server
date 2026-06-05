package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final ActionLogInterceptor actionLogInterceptor;

    public WebMvcConfiguration(ActionLogInterceptor actionLogInterceptor) {
        this.actionLogInterceptor = actionLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(actionLogInterceptor).addPathPatterns("/**");
    }
}
