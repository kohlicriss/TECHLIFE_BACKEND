
package com.example.OfferLetter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "offer_letters")
public class OfferLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    private String empName;
    private String jobType;
    private String department;
    private String designation;
    private Double annualSalary;

    private Double stipend;
    private Double totalSalaryPerMonth;
    private double monthlySalary;

    private Double basicSalary;
    private Double hraAmount;
    private Double conveyanceAllowance;
    private Double medicalAllowance;
    private Double specialAllowance;
    private Double bonusAmount;
    private Double otherAllowances;
    private Double providentFund;
    private Double professionalTax;
    private Double incomeTax;
    private Double otherDeductions;
    private Double grossEarnings;
    private Double totalDeductions;
    private Double netSalary;

    private LocalDate startDate;


}
