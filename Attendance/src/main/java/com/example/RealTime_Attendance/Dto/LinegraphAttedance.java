package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinegraphAttedance {
    String Date;
    String Month;
    Integer Year;
    Double Working_hour;
    Double Break_hour;
}