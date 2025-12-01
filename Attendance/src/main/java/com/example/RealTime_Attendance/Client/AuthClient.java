package com.example.RealTime_Attendance.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "authentication-service",url = "http://hrms.anasolconsultancyservices.com")
public interface AuthClient {
        @GetMapping("/api/auth/role-access/{role}/check/{permission}")
    boolean checkPermission(@PathVariable("role") String role, @PathVariable("permission") String permission);
}
