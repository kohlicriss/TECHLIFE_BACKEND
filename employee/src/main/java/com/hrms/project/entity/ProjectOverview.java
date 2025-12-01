package com.hrms.project.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProjectOverview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String timeline_progress;
    private String client;
    private String total_cost;
    private String days_to_work;
    private String priority;
    private LocalDate startedOn;
    private LocalDate endDate;
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(nullable = true)
    private Integer dueAlert;

    @OneToOne
    @JoinColumn(name = "project_id")
    private Project project;
}
