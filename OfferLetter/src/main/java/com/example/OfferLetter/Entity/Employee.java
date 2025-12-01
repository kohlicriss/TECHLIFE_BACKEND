package com.example.OfferLetter.Entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "employees", schema = "payroll")
@JsonInclude(JsonInclude.Include.ALWAYS)

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    private String empName;
    private String email;
    private String phoneNumber;

    @JsonProperty("stipend")
    private Double stipend;

    private Double annualSalary;
    private Double monthlySalary;

    // Bank Details
    private Long accountNumber;
    private String ifsccode;
    private String bankName;


    private String pfnum;
    private String panNumber;
    private Long aadharNumber;
    private String uanNumber;


    private String department;
    private String designation;

    private String jobType;
    private String level;
    private LocalDate startDate;

    // Standard Deduction Rates
    private Double pfDeductionRate = 12.0;
    private Double professionalTax = 200.0;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference

    private List<Payroll> payrolls;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobDetails> jobDetailsList;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Attendance> attendanceRecords;

    // Calculate monthly salary from LPA
    public void calculateMonthlySalary() {
        if (this.annualSalary != null) {
            this.monthlySalary = (this.annualSalary * 100000) / 12;
        }
    }
}
