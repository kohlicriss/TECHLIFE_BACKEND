package com.hrms.project.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class About{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "employeeId")
    private Employee employee;

    @Column(columnDefinition = "TEXT")
    private String jobLove;

    @Column(columnDefinition = "TEXT")
    private String hobbies;

    @Column(columnDefinition = "TEXT")
    private String about;



}


