package com.example.OfferLetter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "attendance", schema = "payroll")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer month;
    private Integer year;

    private Integer totalWorkingDays;
    private Float daysPresent;
    private Float unpaidLeaves;

    private LocalDate createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @PrePersist
    public void setCreatedDate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDate.now();
        }
    }
}
