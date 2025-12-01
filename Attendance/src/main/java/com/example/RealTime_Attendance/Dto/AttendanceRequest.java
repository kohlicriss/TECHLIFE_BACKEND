package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {
    private String employeeId;
    private Integer month;
    private Integer year;
    private Float totalWorkingDays;
    private Float daysPresent;
    private Float unpaidLeaves;
}