package com.hrms.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTasksDTO {
    private String projectId;
    private String title;


    private List<TaskSimpleDTO> tasks;


}