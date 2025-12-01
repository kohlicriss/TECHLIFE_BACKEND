package com.hrms.project.entity;

import com.nimbusds.jose.shaded.gson.stream.JsonWriter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "employee_project",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "project_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String role;



}
