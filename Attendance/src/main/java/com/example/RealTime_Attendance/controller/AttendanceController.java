package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Dto.*;
import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.service.AttendanceService;
import com.example.RealTime_Attendance.service.LeaveService;
import com.example.RealTime_Attendance.service.ShedulingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final ShedulingService shedulingService;

    /*
     * Get the past attendance of an employee
     * according to the page and size, it gives number of times the user logged in
     */
    @GetMapping("/employee/{employeeId}/attendance")
    public CompletableFuture<ResponseEntity<List<AttendanceDTO>>> getAttendanceDTOs(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Giving the attendances of employee {} for last {} days", employeeId, size);
        return attendanceService.getAllAttendanceDTO(employeeId, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/schedule/absent")
    public List<String> markAbsent() {
        return shedulingService.markAbsent();
    }

    @PutMapping("/employee/{employeeId}/{attendanceId}/disconnected")
    public String disconnected(@PathVariable String attendanceId, @PathVariable String employeeId, @RequestBody Clock clock) {
        return attendanceService.disconnected(attendanceId, clock);
    }

    @PutMapping("/employee/{employeeId}/{attendanceId}/connected")
    public String connected(@PathVariable String attendanceId, @PathVariable String employeeId, @RequestBody Clock clock) {
        return attendanceService.connected(attendanceId, clock);
    }

    @PostMapping("/employee/leaveRequest/{employeeId}")
    public ResponseEntity<String> addLeave(@RequestBody LeaveRequestDTO dto, @PathVariable String employeeId) {
        if (dto.getEmployeeId() == null)
            dto.setEmployeeId(employeeId);
        String savedLeave = leaveService.createLeaveRequest(dto);
        return ResponseEntity.ok(savedLeave);
    }

    @GetMapping("/employee/{employeeId}/leaves")
    public CompletableFuture<ResponseEntity<List<LeavesDto>>> totalLeaves(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return leaveService.getAllLeaves(employeeId, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/employee/{employeeId}/clock-in")
    public ResponseEntity<CompletableFuture<SimpleAttendanceDto>> clockIn(@PathVariable String employeeId,
            @RequestBody Clock clock) {
        CompletableFuture<SimpleAttendanceDto> result = attendanceService.clockIn(employeeId, clock);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/employee/{employeeId}/clock-out")
    public ResponseEntity<CompletableFuture<SimpleAttendanceDto>> clockOut(@PathVariable String employeeId, @RequestBody Clock clock) {
        CompletableFuture<SimpleAttendanceDto> result = attendanceService.clockOut(employeeId, clock);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{Emp_id}/{year}/{month}/{day}")
    public ResponseEntity<CompletableFuture<SimpleAttendanceDto>> getPresentEmployees(
            @PathVariable String Emp_id,
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable int day) {

        return ResponseEntity.ok(attendanceService.getEmployeeAttendanceDay(Emp_id, day, month, year));
    }

    @GetMapping("/employee/{employeeId}/profile/{day}/{month}/{year}")
    public ResponseEntity<ProfileTime> getProfile(@PathVariable String employeeId, @PathVariable int year,
            @PathVariable int month, @PathVariable int day) {

        ProfileTime result = attendanceService.getProfile(employeeId, year, month, day);
        log.info("Employee {} attendance profile is {}", employeeId, result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/{employeeId}/leave-summary")
    public ResponseEntity<List<PersonalLeaveDetailsDTO>> getLeaveSummary(@PathVariable String employeeId) {
        List<PersonalLeaveDetailsDTO> summary = leaveService.getLeaveSummary(employeeId);
        log.info("Getting all types of leaves that are consumed, available and total for employee {}", employeeId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/employee/{emp_id}/pie-chart")
    public ResponseEntity<List<PieChartDTO>> piechart(@PathVariable String emp_id) {
        List<PieChartDTO> result = attendanceService.pieChart(emp_id);
        log.info("Getting last 5 days attendance summery for employee {}", emp_id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/{emp_id}/line-graph")
    public ResponseEntity<List<BarGraphDTO>> bargraph(@PathVariable String emp_id,
            @RequestParam int page, @RequestParam int size) {
        List<BarGraphDTO> result = attendanceService.bargraph(emp_id, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/{emp_id}/bar-chart")
    public ResponseEntity<List<LinegraphAttedance>> linechart(@PathVariable String emp_id,
            @RequestParam int page, @RequestParam int size) {
        List<LinegraphAttedance> result = attendanceService.lineGraphAttendance(emp_id, page, size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/attendances/addAll")
    public void allAttendances(@RequestBody List<Attendance> attendances) {
        System.out.println(attendances);
        for (Attendance attendance : attendances) {
            attendanceService.addAttendance(attendance);
        }
    }

    @GetMapping("/employee/{emp_id}/leavesbar-graph")
    public ResponseEntity<List<LeavesBarGraphDTO>> leavesBar(@PathVariable String emp_id) {
        List<LeavesBarGraphDTO> result = attendanceService.leavesGraph(emp_id);
        return ResponseEntity.ok(result);

    }

    @PostMapping("/personalleaves/addAll")
    public void allLeaves(@RequestBody List<PersonalLeaves> leavs) {
        for (PersonalLeaves leav : leavs) {
            leaveService.addLeaves(leav);
        }
    }

    @GetMapping("/employee/{empId}/personalLeavesData")
    public ResponseEntity<List<PersonalLeaveDetails>> get(@PathVariable String empId) {
        List<PersonalLeaveDetails> res = leaveService.initialLeaveTypeData(empId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/attendance/leaves/dashboard/{empId}/{timeZone}")
    public CompletableFuture<List<DashboardMetric>> getDashboard(@PathVariable String empId, @PathVariable String timeZone) {
        return attendanceService.getDashboardMetrics(empId, timeZone);
    }

    @GetMapping("/present/{year}/{month}/{day}")
    public ResponseEntity<List<AttendanceDTO>> getPresentEmployees(
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable int day) {

        List<AttendanceDTO> presentEmployees = attendanceService.getPresentEmployees(year, month, day);
        return ResponseEntity.ok(presentEmployees);
    }

    @GetMapping("/numberOfOnTime")
    public Map<LocalDate, Integer> getMethodName(@RequestParam LocalDate stDate, @RequestParam LocalDate enDate) {
        Map<LocalDate, Integer> res = attendanceService.getOnTimeEmployees(stDate, enDate);
        return res;
    }

    @GetMapping("/numberOfOvertime")
    public Map<LocalDate, Integer> getEmployeesMeetingShiftHours(
            @RequestParam LocalDate stDate,
            @RequestParam LocalDate enDate) {
        return attendanceService.getEmployeesMeetingShiftHours(stDate, enDate);
    }

    @PutMapping("/init")
    public String init() {
        leaveService.initializeMonthlyLeaveBalances();
        return "done";
    }

    // @GetMapping("/overtime")
    // public ResponseEntity<HashMap<String, Duration>> getOvertimeBetweenDates(
    //         @RequestParam LocalDate start,
    //         @RequestParam LocalDate end,
    //         @RequestParam(defaultValue = "10") Long limit) {

    //     HashMap<String, Duration> response = attendanceService.getOvertimeBetweenDates(start, end, limit);
    //     return ResponseEntity.ok(response);
    // }


    @GetMapping("/top-overtime")
    public ResponseEntity<Map<String, Duration>> getTopOvertimeEmployees(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(defaultValue = "5") int limit) {

        Map<String, Duration> result = attendanceService.getTopOvertimeEmployees(start, end, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-ontime")
    public ResponseEntity<Map<String, Integer>> getTopOnTime(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(defaultValue = "5") int limit) {

        Map<String, Integer> result = attendanceService.getTopOntimeEmployees(start, end, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attendance-summary-between-dates")
    public ResponseEntity<List<AttendanceSummaryDTO>> getAttendanceSummary(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<AttendanceSummaryDTO> summary = attendanceService.getCombinedSummary(start, end);
        return ResponseEntity.ok(summary);
    }
}
