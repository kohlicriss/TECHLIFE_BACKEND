package com.app.chat_service.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.chat_service.dto.NotificationRequest;

@FeignClient(name="notifications-service",url = "https://hrms.anasolconsultancyservices.com")

public interface NotificationClient {
	
    @PostMapping("/api/notification/send")
    String send(@RequestBody NotificationRequest notification);

   
}