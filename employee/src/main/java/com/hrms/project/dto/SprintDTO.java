package com.hrms.project.dto;

import com.hrms.project.entity.SprintStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SprintDTO {
    private String sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;
    private String projectId;
}
