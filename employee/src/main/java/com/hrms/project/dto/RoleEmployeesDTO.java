package com.hrms.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEmployeesDTO {
    private String role;
    private int count;
    private List<EmployeeSimpleDTO> employees;
}