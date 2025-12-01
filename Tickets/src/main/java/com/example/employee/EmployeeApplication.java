package com.example.employee;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.example.employee.client")
public class EmployeeApplication {

	public static void main(String[] args) {

		SpringApplication.run(EmployeeApplication.class, args);

	}


}

