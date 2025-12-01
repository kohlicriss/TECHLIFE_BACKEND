package com.example.RealTime_Attendance.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleAttendanceDto {
  private String empId;
  private String date;
  private String loginTime;
  private String logoutTime;
  private String attendanceId;
  private List<SimpleBreakDto> breaks;
}
