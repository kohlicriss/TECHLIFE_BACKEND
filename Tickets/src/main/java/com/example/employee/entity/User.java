package com.example.employee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users" , schema = "ticket") 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
     @Column(name = "employee_id")
    private String employeeId;

    @Enumerated(EnumType.STRING)
    private Roles roles;


}

