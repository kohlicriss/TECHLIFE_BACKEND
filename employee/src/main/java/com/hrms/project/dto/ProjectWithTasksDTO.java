// ProjectWithTasksDTO.java
package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWithTasksDTO {
    private String projectId;
    private String projectTitle;
    private List<TaskWithUpdatesDTO> tasks;
}
