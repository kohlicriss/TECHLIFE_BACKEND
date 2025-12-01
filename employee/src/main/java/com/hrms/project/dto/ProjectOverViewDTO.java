package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOverViewDTO {
    private String timeline_progress;
    private String client;
    private String total_cost;
    private String days_to_work;
    private String priority;
    private LocalDate startedOn;
    private LocalDate endDate;
    private ManagerDTO manager;
    private int dueAlert;
}
