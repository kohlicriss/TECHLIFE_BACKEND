package com.hrms.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class EmployeeRoleDTO {
    private String projectId;
    private Map<String, String> employeeRoles;

}
