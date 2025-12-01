package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatEmployeeDTO {
    private String employeeId;
    private String displayName;
    private String employeeImage;
}

