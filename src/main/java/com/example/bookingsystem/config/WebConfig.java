package com.example.bookingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    private static final int MAX_PAGE_SIZE = 100;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer
    pageableHandlerMethodArgumentResolverCustomizer() {

        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}