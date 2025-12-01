package com.hrms.project.dto;

import com.hrms.project.configuration.TaskId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskSimpleDTO {
    private String taskId;
    private String title;
    private String status;
    private String description;
}
