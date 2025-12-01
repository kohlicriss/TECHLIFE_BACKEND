package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Dto.AttendanceDTO;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.Security.CheckPermission;
import com.example.RealTime_Attendance.Security.TypeVar;
import com.example.RealTime_Attendance.service.AdminAttendanceService;
import com.example.RealTime_Attendance.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/attendance/admin")
public class AdminController {
    @Autowired
    private AdminAttendanceService attendanceService;
    @Autowired
    private LeaveService leaveService;
    @GetMapping("/present/{year}/{month}/{day}")

    public ResponseEntity<Page<AttendanceDTO>> getPresentEmployees(
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable int day,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(attendanceService.getPresentEmployees(year, month, day, page, size));
    }


    @GetMapping("/absent/{year}/{month}/{day}")
    public ResponseEntity<List<AttendanceDTO>> getAbsentEmployees(
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable int day,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(attendanceService.getAbsentEmployees(year, month, day, page, size));
    }
    @GetMapping("/employee/{employeeId}")
    @CheckPermission(
            value = "ATTENDANCE_EMPLOYEE_READ",
            MatchParmName = "employeeId",
            MatchParmFromType = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"},
            type = TypeVar.VARIABLE
    )
    public ResponseEntity<CompletableFuture<List<AttendanceDTO>>> getAttendanceByEmployee(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(employeeId, page, size));
    }

    @GetMapping("/employee/{employeeId}/month/{year}/{month}")
    public ResponseEntity<CompletableFuture<List<AttendanceDTO>>> getEmployeeAttendanceForMonth(
            @PathVariable String employeeId,
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(attendanceService.getEmployeeAttendanceForMonth(employeeId, year, month, page, size));
    }

@GetMapping("/month/{year}/{month}")
public ResponseEntity<CompletableFuture<List<AttendanceDTO>>> getMonthlyReport(
        @PathVariable int year,
        @PathVariable int month,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    return ResponseEntity.ok(attendanceService.getMonthlyReport(year, month, page, size));
}
@GetMapping("/all")
public ResponseEntity<CompletableFuture<List<AttendanceDTO>>> getAllAttendance(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    return ResponseEntity.ok(attendanceService.getAllAttendance(page, size));
}
    @GetMapping("/pendingLeaveRequests")
    public CompletableFuture<ResponseEntity<List<LeaveEntity>>> getPendingLeave() {
        return leaveService.getPendingLeaves()
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/leave/{leaveId}/approve")
    public ResponseEntity<LeaveEntity> approveLeave(@PathVariable Long leaveId) {
        LeaveEntity updatedLeave = leaveService.approveLeave(leaveId);
        return ResponseEntity.ok(updatedLeave);
    }
    @PutMapping("/leave/{leaveId}/reject")
    public ResponseEntity<LeaveEntity> rejectLeave(@PathVariable Long leaveId,
                                                   @RequestParam String reason) {
        LeaveEntity updatedLeave = leaveService.rejectLeave(leaveId, reason);
        return ResponseEntity.ok(updatedLeave);
    }
}
