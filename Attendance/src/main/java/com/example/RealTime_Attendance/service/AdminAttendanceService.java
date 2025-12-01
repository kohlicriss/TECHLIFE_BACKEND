package com.example.RealTime_Attendance.service;

import com.example.RealTime_Attendance.Dto.AttendanceDTO;
import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.LeaveEntityRepository;

import org.hibernate.query.results.complete.CompleteFetchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AdminAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveEntityRepository leaveRepository;

    private AttendanceDTO convertToDTO(Attendance attendance) {
        return AttendanceDTO.builder()
                .employee_id(attendance.getEmployeeId())
                .date(attendance.getDate().toString())
                .login_time(attendance.getLoginTime() != null ? attendance.getLoginTime().toString() : null)
                .logout_time(attendance.getLogoutTime() != null ? attendance.getLogoutTime().toString() : null)
                .build();
    }

    // @Cacheable(value = "presentEmployees", key = "#year + '-' + #month + '-' + #day")
    public Page<AttendanceDTO> getPresentEmployees(int year, int month, int day, int page, int size) {
        LocalDate date = LocalDate.of(year, month, day);
        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeId").ascending());
        Page<Attendance> result = attendanceRepository.findPresentEmployeesOnDate(date, pageable);
        return result.map(this::convertToDTO);
    }

    // @Cacheable(value = "absentEmployees", key = "#year + '-' + #month + '-' + #day")
    public List<AttendanceDTO> getAbsentEmployees(int year, int month, int day, int page, int size) {
        LocalDate date = LocalDate.of(year, month, day);
        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeId").ascending());
        Page<Attendance> result = attendanceRepository.findAbsentEmployeesOnDate(date, pageable);
        return result.map(this::convertToDTO).getContent();
    }
    @Async("attendanceExecutor")
    public CompletableFuture<List<AttendanceDTO>> getAttendanceByEmployee(String employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Attendance> result = attendanceRepository.findByEmployeeId(employeeId, pageable);
        return CompletableFuture.completedFuture(result.map(this::convertToDTO).getContent());
    }
    @Async("attendanceExecutor")
    // @Cacheable(
    //         value = "employeeMonthlyAttendance",
    //         key = "#employeeId + '-' + #year + '-' + #month",
    //         condition = "!(#year == T(java.time.LocalDate).now().year && #month == T(java.time.LocalDate).now().monthValue)"
    // )
    public CompletableFuture<List<AttendanceDTO>> getEmployeeAttendanceForMonth(String employeeId, int year, int month, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Attendance> result = attendanceRepository.findAttendanceByEmployeeAndMonthAndYear(employeeId, year, month, pageable);
        return CompletableFuture.completedFuture(result.map(this::convertToDTO).getContent());
    }

    @Async("attendanceExecutor")
    // @Cacheable(
    //         value = "monthlyReport",
    //         key = "#year + '-' + #month",
    //         condition = "!(#year == T(java.time.LocalDate).now().year && #month == T(java.time.LocalDate).now().monthValue)"
    // )
    public CompletableFuture<List<AttendanceDTO>> getMonthlyReport(int year, int month, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeId").ascending());
        Page<Attendance> result = attendanceRepository.findAttendanceByMonthAndYear(year, month, pageable);
        return CompletableFuture.completedFuture(result.map(this::convertToDTO).getContent());
    }

    @Async("attendanceExecutor")
    public CompletableFuture<List<AttendanceDTO>> getAllAttendance(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Attendance> result = attendanceRepository.findAll(pageable);
        return CompletableFuture.completedFuture(result.map(this::convertToDTO).getContent());
    }


}
