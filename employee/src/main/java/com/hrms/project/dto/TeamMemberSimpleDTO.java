package com.hrms.project.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberSimpleDTO {
    private String employeeId;
    private String displayName;
    private String role;
}