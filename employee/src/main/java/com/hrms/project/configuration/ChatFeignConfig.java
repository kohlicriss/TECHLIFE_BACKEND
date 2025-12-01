package com.hrms.project.configuration;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class ChatFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Get current HTTP request from RequestContextHolder
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String token = attributes.getRequest().getHeader("Authorization");
                if (token != null) {
                    template.header("Authorization", token); // forward token to Chat service
                } else {
                    log.warn("No Authorization header found in incoming request!");
                }
            } else {
                log.warn("RequestContextHolder returned null attributes!");
            }
        };
    }
}
