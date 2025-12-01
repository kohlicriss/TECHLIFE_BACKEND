package com.hrms.project.client;

import com.hrms.project.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="notifications-service")//,url = "https://hrms.anasolconsultancyservices.com")
public interface NotificationClient {
    @PostMapping("/api/notification/send")
    String send(@RequestBody NotificationRequest notification);

    @PostMapping("/api/notification/sendList")
    String sendList(@RequestBody List<NotificationRequest> notifications);
}

