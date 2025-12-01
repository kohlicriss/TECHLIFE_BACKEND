package com.hrms.project.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
public class AadhaarCardDetails {

    @Id
    private String aadhaarNumber;
    private String enrollmentNumber;

    private LocalDate dateOfBirth;
    private String aadhaarName;
    private String address;
    private String gender;
    private String uploadAadhaar;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference("aadhaar-ref")
    private Employee employee;

}