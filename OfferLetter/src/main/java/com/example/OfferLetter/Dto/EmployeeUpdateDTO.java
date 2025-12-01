package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeUpdateDTO {
    private String empName;
    private String email;
    private String phoneNumber;
    private Long accountNumber;
    private String bankName;
    private String panNumber;
    private Long aadharNumber;
    private String ifsccode;
    private String pfnum;
    private String uanNumber;
    private String department;
    private String designation;
}