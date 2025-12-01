package com.hrms.project.controller;

import com.hrms.project.dto.EmployeeKPIResponse;
import com.hrms.project.dto.EmployeeProjectKPI;
import com.hrms.project.service.KPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class KPIController {

    private final KPIService kpiService;

    @GetMapping("/kpi/employee/{employeeId}")
    public ResponseEntity<EmployeeKPIResponse> getEmployeeKPIs(@PathVariable String employeeId) {
        EmployeeKPIResponse response = kpiService.getEmployeeKPIs(employeeId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/kpi/{employeeId}/{projectId}")
    public ResponseEntity<EmployeeProjectKPI> getEmployeeProjectKPI(
            @PathVariable String employeeId,
            @PathVariable String projectId) {
        EmployeeProjectKPI response = kpiService.getEmployeeProjectKPI(employeeId, projectId);
        return ResponseEntity.ok(response);
    }


}
