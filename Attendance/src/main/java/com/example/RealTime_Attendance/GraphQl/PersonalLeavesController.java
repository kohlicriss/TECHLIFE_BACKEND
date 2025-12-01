package com.example.RealTime_Attendance.GraphQl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.RealTime_Attendance.Dto.PersonDetailsAddDto;
import com.example.RealTime_Attendance.service.PersonalLeavesService;

@Controller
public class PersonalLeavesController {

  @Autowired
  private PersonalLeavesService personalLeavesService;
  
  @QueryMapping
  public PersonDetailsAddDto getPersonalLeavesByEmployeeIdAndDateAndMonth(@Argument String employeeId, @Argument int month, @Argument int year) {
      PersonDetailsAddDto leaves = personalLeavesService.getLeaves(employeeId, month, year);
      return leaves;
  }
}
