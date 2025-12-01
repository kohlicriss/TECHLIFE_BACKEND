package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"teamLeads", "employeeProjects", "team"})
public class Project {

    @Id
    @Column
    private String projectId;

    private String title;
   // private String client;
    private String description;
    private String projectPriority;
    private String projectStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer openTask=0;
    private Integer closedTask=0;
    private String details;

    @ManyToMany
    @JoinTable(
            name = "project_team_lead",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private List<Employee> teamLeads;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeProject> employeeProjects = new ArrayList<>();

    @OneToMany(mappedBy = "project",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> assignments;




//    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<KeyMetric> keyMetrics;
}
