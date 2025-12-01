package com.hrms.project.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicEmployeeDetails {

    private String employeeId;
    private String name;
    private String jobTitlePrimary;
    private String department;

    private String employeeImage;
    private String location;
    private String workEmail;
    private String contact;

    private List<String> skills;
    private List<PublicProjectDTO> projects;
    private List<AchievementsDTO> achievements;



}
