package com.hrms.project.dto;

public record EmployeeTaskDetailsDTO(
        long tasksDone,
        long totalTasks,
        double percentageCompleted
) {}
