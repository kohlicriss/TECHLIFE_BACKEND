package com.hrms.project.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProjectTeamDto {

    private String projectName;
    private String projectId;
    private String description;
    private List<TeamMemberSimpleDTO>teamMembers;
    private ProjectPersonDTO projectManager;
    private List<ProjectPersonDTO> teamLeads;



}
