package com.example.RealTime_Attendance.Client;


import com.example.RealTime_Attendance.Dto.ApiResponse;
import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payroll-service", url = "http://192.168.0.112:8087")
public interface PayRoll {
    @PostMapping("/api/payroll/attendance")
    ResponseEntity<ApiResponse> generatePaySlips(@RequestBody AttendanceRequest attendanceRequest, @RequestHeader String PassKey);
}
