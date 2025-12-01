package com.example.OfferLetter.Dto;

import com.example.OfferLetter.Entity.Attendance;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.Payroll;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeFullDetailsResponse {
    private Employee employee;
    private List<Attendance> attendanceRecords;
    private List<Payroll> payrolls;
}