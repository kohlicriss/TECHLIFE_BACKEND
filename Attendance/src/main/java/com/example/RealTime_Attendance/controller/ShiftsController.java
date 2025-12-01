package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Dto.ScheduleAttendance;
import com.example.RealTime_Attendance.Dto.ShiftsDto;
import com.example.RealTime_Attendance.Entity.Shifts;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.Schedule.Daily;
import com.example.RealTime_Attendance.service.*;
import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance/shifts")
public class ShiftsController {

    private final ShiftsService shiftsService;
    private final Daily daily;

    @PostMapping
    public ResponseEntity<Shifts> createShift(@RequestBody Shifts shift) {
        return ResponseEntity.ok(shiftsService.saveShift(shift));
    }

    @GetMapping("/employees/{shift_name}")
    public ResponseEntity<List<AttendanceRequest>> getEmployees(@PathVariable String shift_name, @RequestParam int page, @RequestParam int size) {
        return ResponseEntity.ok(shiftsService.getEmployeesInShift(shift_name, page, size));
    }

    @GetMapping("/by/{employee_id}")
    public ResponseEntity<Shifts> getShiftByEmployeeId(@PathVariable String employee_id, @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(shiftsService.getShiftOfEmployee(employee_id, month, year));
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerAttendanceCheck(@RequestBody ScheduleAttendance scheduleAttendance) {
        try {
            daily.scheduleAttendanceJob(scheduleAttendance);
            return ResponseEntity.ok("Attendance check scheduled for shift: " + scheduleAttendance.getShift() + "for section"+ scheduleAttendance.getSection() + " with cron: " + scheduleAttendance.getCronExpression());
        } catch (Exception e) {
            throw new CustomException( "Error scheduling attendance check: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<ShiftsDto>> getAllShifts() {
        return ResponseEntity.ok(shiftsService.getAllShifts());
    }

    @GetMapping("/{shiftName}")
    public ResponseEntity<Shifts> getShiftById(@PathVariable String shiftName) {
        return ResponseEntity.ok(shiftsService.getShiftById(shiftName));
    }

    @PutMapping("/{shiftName}")
    public ResponseEntity<Shifts> updateShift(@PathVariable String shiftName, @RequestBody Shifts shift) {
        return ResponseEntity.ok(shiftsService.updateShift(shiftName, shift));
    }

    @DeleteMapping("/{shiftName}")
    public ResponseEntity<Void> deleteShift(@PathVariable String shiftName) {
        shiftsService.deleteShift(shiftName);
        return ResponseEntity.noContent().build();
    }

}
