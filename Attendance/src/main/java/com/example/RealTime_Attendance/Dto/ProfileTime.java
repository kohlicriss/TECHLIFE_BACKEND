package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTime {
  String mode;
  String shift;
  Long onTime;
  Long avgWorkingHours;
  LocalDateTime loginTime;
  LocalDateTime logoutTime;
  Duration grossTimeDay;
  Duration effectiveTimeDay;
}
