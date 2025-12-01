package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleBreakDto {
  private String startTime;
  private String endTime;
}
