package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDetailsDTO {
    private String employeeImage;
    private String employeeId;
    private String employeeName;
    private String role;
    private String email;
    private String contactNumber;
    private String description;
    private String status;
}
