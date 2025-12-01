package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSimpleDTO {
    private String employeeImage;
    private String employeeId;
    private String displayName;
}
