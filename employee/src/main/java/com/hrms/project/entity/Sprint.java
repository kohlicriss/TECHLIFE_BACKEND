package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sprint {

    @Id
    private String  sprintId;

    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private SprintStatus status;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;


}
