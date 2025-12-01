package com.hrms.project.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @Column
    private String departmentId;

    private String departmentName;
    private String departmentDescription;

    @OneToMany(mappedBy = "department")
    @JsonManagedReference
    private List<Employee> employee;
}
