package com.example.RealTime_Attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.LeaveEntityRepository;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShedulingService {
  
  private final AttendanceRepository attendanceRepository;
  private final LeaveEntityRepository leaveRepo;
  private final PersonalLeavesRepository personalLeavesRepository;

  public List<String> getAllEmployees(){
    return personalLeavesRepository.getAllEmployees().get();
  }

  public List<String> markAbsent(){
    List<String> Present = attendanceRepository.findByDateAndIsAttendedTrue(LocalDate.now()).stream().map(Attendance::getEmployeeId).collect(Collectors.toList());
    List<String> Absent = personalLeavesRepository.getAllEmployees().get().stream().filter(e->!Present.contains(e)).collect(Collectors.toList());
    for(String employee: Absent){
      Attendance absent = Attendance.builder()
        .employeeId(employee)
        .date(LocalDate.now())
        .month(LocalDate.now().getMonth().toString())
        .year(LocalDate.now().getYear())
        .mode("Absent")
        .isAttended(false)
        .build();
      attendanceRepository.save(absent);
    }
    return Absent;
  }
}
