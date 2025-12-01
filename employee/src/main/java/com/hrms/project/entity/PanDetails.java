package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
public class PanDetails {

    @Id
    private String panNumber;

    private String panName;
    private String dateOfBirth;
    private String parentsName;
    private String panImage;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference("pan")
    private Employee employee;
}
