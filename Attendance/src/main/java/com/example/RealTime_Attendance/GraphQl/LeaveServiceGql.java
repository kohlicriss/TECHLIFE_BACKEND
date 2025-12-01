package com.example.RealTime_Attendance.GraphQl;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.RealTime_Attendance.Dto.LeavesDto;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.repository.LeaveEntityRepository;

@Controller
public class LeaveServiceGql {

  @Autowired
  private LeaveEntityRepository leaveEntityRepository;
  
  @QueryMapping
  public List<LeavesDto> getAllLeavesByDate(@Argument LocalDate Date) {
      List<LeaveEntity> leaves = leaveEntityRepository.findByReqToOrderByReqOn(Date);
      List<LeavesDto> returnDto = new ArrayList<>();
      for (LeaveEntity leave: leaves){
        returnDto.add(LeaveEntity.fromEntity(leave));
      }
      return returnDto;
  }
}
