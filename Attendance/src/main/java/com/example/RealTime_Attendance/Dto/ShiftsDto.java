package com.example.RealTime_Attendance.Dto;

import java.time.LocalTime;
import java.util.List;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ShiftsDto {

  private String Name;
  private LocalTime Start;
  private LocalTime End;
  private List<String> employeeIds;

}
