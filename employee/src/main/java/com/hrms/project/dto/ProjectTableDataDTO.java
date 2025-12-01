package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTableDataDTO {

    private String project_id;
    private String project_name;
    private String status;
    private LocalDate start_date;
    private LocalDate end_date;
    private List<String> employee_team;
    private String priority;
    private String Details;//image
    


}