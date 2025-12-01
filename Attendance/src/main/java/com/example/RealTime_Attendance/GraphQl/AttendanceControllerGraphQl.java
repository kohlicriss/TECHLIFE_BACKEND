package com.example.RealTime_Attendance.GraphQl;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.RealTime_Attendance.Dto.SimpleAttendanceDto;
import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.repository.AttendanceRepository;

import lombok.extern.slf4j.Slf4j;



@Controller
@Slf4j
public class AttendanceControllerGraphQl {

  @Autowired
  private AttendanceRepository attendanceRepository;
  @QueryMapping
  public CompletableFuture<SimpleAttendanceDto> getEmployeeAttendanceDay(@Argument String employeeId,@Argument int day, @Argument int month, @Argument int year){
        Optional<Attendance> result = attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.of(year, month, day));
        if(!result.isPresent()){
            log.error("Attendance not fount for employee {} on date {}", employeeId, LocalDate.of(year, month, day));
            return null;
        }
        return CompletableFuture.completedFuture(result.get().toSimpleAttendanceDto());
    }
}
