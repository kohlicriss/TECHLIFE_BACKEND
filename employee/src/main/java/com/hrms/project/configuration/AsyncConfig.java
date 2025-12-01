package com.hrms.project.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("imageTaskExecutor")
    public Executor imageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(Math.max(2, cores / 2));
        executor.setMaxPoolSize(Math.max(4, cores));
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ImgProc-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "employeeTaskExecutor")
    public Executor employeeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("Employee-Async-");
        executor.initialize();
        return executor;
    }
}
