package com.example.RealTime_Attendance.Dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Data
public class BarGraphDTO {
   String EmployeeId;
   String Date;
   String Month;
   String Year;
   String Start_time;
   String End_time;

   List<Breaksdto> breaks;
}
