package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import org.springframework.web.bind.annotation.RestController;

import com.example.RealTime_Attendance.Dto.PersonDetailsAddDto;
import com.example.RealTime_Attendance.service.PersonalLeavesService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/attendance")
public class PersnalLeavesController {

  @Autowired
  private PersonalLeavesService personalLeavesService;

  @PostMapping("/personalleaves/add")
  public String postMethodName(@RequestBody PersonDetailsAddDto entity) {
      return personalLeavesService.addLeaves(entity);
  }
  
  @PostMapping("/updateLeaves/{month}/{year}")
  public String updateLeaves(@RequestBody PersonDetailsAddDto entity,@PathVariable Integer month,@PathVariable Integer year) {
      return personalLeavesService.updateLeaves(entity, month, year);
  }
  
  @DeleteMapping("/deleteLeaves/{employee_id}/{month}/{year}")
  public String deleteLeaves(@PathVariable String employee_id,@PathVariable Integer month,@PathVariable Integer year) {
      return personalLeavesService.deleteLeaves(employee_id, month, year);
  }

  @GetMapping("/getLeaves/{employee_id}/{month}/{year}")
  public PersonDetailsAddDto getLeaves(@PathVariable String employee_id,@PathVariable Integer month,@PathVariable Integer year) {
      return personalLeavesService.getLeaves(employee_id, month, year);
  }

  @GetMapping("/getLeaves/{employee_id}")
  public CompletableFuture<List<AttendanceRequest>> getLeaves(@PathVariable String employee_id, @RequestParam int page, @RequestParam int size) {
      return personalLeavesService.getLeavesByEmployee(employee_id, page, size);
  }

  @GetMapping("/getAllEmployeeLeaves/{month}/{year}")
  public CompletableFuture<List<AttendanceRequest>> getAllEmployeeLeaves(@PathVariable Integer month, @PathVariable Integer year, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
      return personalLeavesService.attendanceReport(month, year, page, size);
  }
  
}
