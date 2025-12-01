package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.*;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @Column
    private String teamId;

    @Column(unique = true)
    private String teamName;
    private String teamDescription;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "team_employee",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> employees = new HashSet<>();


    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<Project> projects = new ArrayList<>();

}
