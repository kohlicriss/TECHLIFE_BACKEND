package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberPerformanceDTO {
    private String employeeId;
    private String employeeName;
    private String projectRole; 
    private double performancePercentage;
    private String status;
    private int openTasks;
    private int closedTasks;// e.g., "3/5"
}
