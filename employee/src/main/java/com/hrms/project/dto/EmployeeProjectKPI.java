package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeProjectKPI {
    private String projectId;
    private String projectName;
    private String ProjectDescription;
    private Map<String, String> kpis; // Task Completion, Sprint Completion, Overall Performance
    private String status;            // On Track / Needs Improvement
}
