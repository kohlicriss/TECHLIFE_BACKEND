package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeKPIResponse {
    private String employeeId;
    private String employeeName;
    private List<EmployeeProjectKPI> projects;
}
