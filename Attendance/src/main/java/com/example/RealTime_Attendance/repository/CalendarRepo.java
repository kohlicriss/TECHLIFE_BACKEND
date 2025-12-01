package com.example.RealTime_Attendance.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.Entity.CalendarEvent;

@Repository
public interface CalendarRepo extends JpaRepository<CalendarEvent, Long> {

  @Query("SELECT c FROM CalendarEvent c WHERE c.event IS NOT NULL AND c.date BETWEEN :start AND :end")
  List<CalendarEvent> findEventsBetweenDates(LocalDate start, LocalDate end);

  @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId AND a.date BETWEEN :start AND :end")
  List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate start, LocalDate end);

  

  @Query("SELECT c FROM CalendarEvent c WHERE c.isHoliday = true AND c.date BETWEEN :start AND :end")
  List<CalendarEvent> findHolidaysBetweenDates(LocalDate start, LocalDate end);

  CalendarEvent findByDate(LocalDate date);

  @Query(nativeQuery = true, value = "SELECT * FROM attendance.calendar_event ce WHERE date BETWEEN :start AND :end ORDER BY date")
  List<CalendarEvent> findByDateBetweeen(@Param("start") LocalDate start, @Param("end") LocalDate end);


}
