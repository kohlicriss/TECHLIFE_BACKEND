package com.example.RealTime_Attendance.repository;

import com.example.RealTime_Attendance.Entity.Attendance;

import io.lettuce.core.Limit;
import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        List<Attendance> findByEmployeeId(String employeeId);
        // Optional<List<Attendance>> findByAttendanceIdList(String attendanceId);

        Optional<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);

        Optional<Attendance> findByAttendanceId(String attendanceId);

        @Query(nativeQuery = true, value = "SELECT * FROM attendance.attendance a WHERE a.employee_id = :employeeId AND a.date BETWEEN :start AND :end")
        List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate start, LocalDate end);

        @Query("SELECT DISTINCT a.employeeId FROM Attendance a")
        List<String> findAllEmployeeIds();

        List<Attendance> findAllByDate(LocalDate date);

        @Query(value = "SELECT * FROM attendance.attendance WHERE employee_id = ?1", nativeQuery = true)
        List<Attendance> getByEmployeeId(String empId);

        Page<Attendance> findByEmployeeId(String employeeId, Pageable pageable);

        List<Attendance> findByDateAndIsAttendedTrue(LocalDate date);

        @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.isAttended = true")
        Page<Attendance> findPresentEmployeesOnDate(LocalDate date, Pageable pageable);

        @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.isAttended = false")
        Page<Attendance> findAbsentEmployeesOnDate(LocalDate date, Pageable pageable);

        @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId AND YEAR(a.date) = :year AND MONTH(a.date) = :month")
        Page<Attendance> findAttendanceByEmployeeAndMonthAndYear(String employeeId, int year, int month,
                        Pageable pageable);

        @Query("SELECT a FROM Attendance a WHERE YEAR(a.date) = :year AND MONTH(a.date) = :month")
        Page<Attendance> findAttendanceByMonthAndYear(int year, int month, Pageable pageable);

        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :date AND a.isAttended = true")
        Long countPresentEmployees(LocalDate date);

        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :date AND a.isAttended = false")
        Long countAbsentEmployees(LocalDate date);

        @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId ORDER BY a.date DESC")
        List<Attendance> findLatestByEmployeeId(String employeeId, Pageable pageable);

        @Query(nativeQuery = true, value = "SELECT * FROM attendance.attendance a " +
                        "WHERE a.\"date\" BETWEEN :start AND :end " +
                        "ORDER BY a.\"date\"")
        List<Attendance> findByAllEmployeesBetweenDates(@Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query(nativeQuery = true, value = "SELECT a.date AS attendance_date, COUNT(*) AS on_time_count " +
                        "FROM attendance.attendance a " +
                        "JOIN attendance.personal_leaves pl ON a.employee_id = pl.employee_id " +
                        "JOIN attendance.shifts s ON pl.shifts = s.shift_name " +
                        "WHERE a.is_attended = TRUE " +
                        "AND a.date = :on_date " +
                        "AND a.login_time::time <= s.start_time::time " +
                        "GROUP BY a.date")
        List<Object[]> findOnTimeEmployees(@Param("on_date") LocalDate on_date);

        @Query(nativeQuery = true, value = "SELECT a.date AS attendance_date, COUNT(*) AS on_time_count " +
                        "FROM attendance.attendance a " +
                        "JOIN attendance.personal_leaves pl ON a.employee_id = pl.employee_id " +
                        "JOIN attendance.shifts s ON pl.shifts = s.shift_name " +
                        "WHERE a.is_attended = TRUE " +
                        "AND a.date = :on_date " +
                        "AND a.effective_hours > EXTRACT(EPOCH FROM (s.end_time - s.start_time)) / 3600 " +
                        "GROUP BY a.date")
        List<Object[]> findEmployeesMeetingShiftHours(@Param("on_date") LocalDate on_date);

        @Query(value = "SELECT a.employee_id, " +
                        "SUM(a.effective_hours - EXTRACT(EPOCH FROM (s.end_time - s.start_time))) / 3600 AS total_overtime_seconds "
                        +
                        "FROM attendance.attendance a " +
                        "JOIN attendance.personal_leaves pl ON a.employee_id = pl.employee_id " +
                        "JOIN attendance.shifts s ON pl.shifts = s.shift_name " +
                        "WHERE a.is_attended = TRUE " +
                        "AND a.date BETWEEN :start AND :end " +
                        "AND a.effective_hours > EXTRACT(EPOCH FROM (s.end_time - s.start_time)) / 3600 " +
                        "GROUP BY a.employee_id " +
                        "ORDER BY total_overtime_seconds DESC " +
                        "LIMIT :limit", nativeQuery = true)
        List<Object[]> overtimeEmployeesBetweenDates(LocalDate start, LocalDate end, Long limit);

        @Query(value = "SELECT a.employee_id, a.effective_hours, a.login_time, s.start_time, s.end_time " +
                        "FROM attendance.attendance a " +
                        "JOIN attendance.personal_leaves pl ON a.employee_id = pl.employee_id " +
                        "JOIN attendance.shifts s ON pl.shifts = s.shift_name " +
                        "WHERE a.is_attended = TRUE " +
                        "AND a.date BETWEEN :start AND :end", nativeQuery = true)
        List<Object[]> findEmployeeEffectiveAndShiftTimes(@Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query(value = "SELECT date, " +
                        "COUNT(DISTINCT CASE WHEN is_attended = true THEN employee_id END) AS present, " +
                        "COUNT(DISTINCT CASE WHEN is_attended = false THEN employee_id END) AS absent " +
                        "FROM attendance.attendance " +
                        "WHERE date BETWEEN :start AND :end " +
                        "GROUP BY date", nativeQuery = true)
        List<Object[]> getPresentAndAbsentEmployees(@Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query(value = """
                  SELECT 
                    employee_id,
                    COUNT(CASE WHEN is_attended = true THEN 1 END) AS present,
                    COUNT(CASE WHEN is_attended = false THEN 1 END) AS absent,
                    COUNT(CASE WHEN first_section = false THEN 1 END) AS first_half_absent,
                    COUNT(CASE WHEN second_section = false THEN 1 END) AS second_half_absent,
                    COUNT(CASE WHEN consider_present = true THEN 1 END) AS actual_present
                  FROM attendance.attendance 
                  WHERE date BETWEEN :start AND :end
                    AND employee_id = :employeeId 
                  GROUP BY employee_id
                """, nativeQuery = true)
        List<Object[]> getPresentAndAbsentByEmployee(@Param("employeeId") String employeeId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

}
