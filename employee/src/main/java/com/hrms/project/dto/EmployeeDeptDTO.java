package com.hrms.project.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDeptDTO {
    private String employeeId;
    private String displayName;
    private String jobTitlePrimary;
    private String workEmail;
    private String workNumber;
    private String role;
    private String employeeImage;

}
