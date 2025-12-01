package com.hrms.project.dto;


import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamController {
   // @Pattern(regexp="^TEAM\\d{3}$",message = "Team id must start with TEAM followed by 3 digits")
    private String teamId;
    private String teamName;
    private String teamDescription;
    private Map<String,String> employeeRoles;
    private String projectId;

}
