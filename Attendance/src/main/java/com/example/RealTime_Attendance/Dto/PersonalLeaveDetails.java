package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalLeaveDetails {
    private String employee;
    private String leaveType;
    private Double days;
}
