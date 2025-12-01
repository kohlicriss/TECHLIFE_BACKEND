package com.hrms.project.dto;

public record EmployeePerformanceDTO(
        String employeeImage,
        String employeeId,

        String employeeName,
        double percentageCompleted,
        String status,
        String role
       ) {}
