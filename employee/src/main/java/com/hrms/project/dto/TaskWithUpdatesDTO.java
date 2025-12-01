// TaskWithUpdatesDTO.java
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
public class TaskWithUpdatesDTO {
    private String taskId;
    private String title;
    private String status;
    private List<TaskUpdateSimpleDTO> updates;
}
