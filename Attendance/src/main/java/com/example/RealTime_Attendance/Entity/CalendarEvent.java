package com.example.RealTime_Attendance.Entity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.example.RealTime_Attendance.Dto.CalendarDto;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CalendarEvent", schema= "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private String event;
    private boolean isHoliday;

    @Column(columnDefinition = "TEXT")
    private String description;

    public CalendarDto toDto(CalendarEvent event) {
        if (event == null) return null;

        return CalendarDto.builder()
                .id(event.getId())
                .date(event.getDate())
                .event(event.getEvent())
                .description(event.getDescription())
                .holiday(event.isHoliday())
                // Fields not present in CalendarEvent
                .employeeId(null)
                .effectiveHours(null)
                .isPresent(null)
                .day(event.getDate() != null ? event.getDate().getDayOfWeek().name() : null)
                .build();
    }
    
    // Optional: convert a list
    public List<CalendarDto> toDtoList(List<CalendarEvent> events) {
        if (events == null) return Collections.emptyList();
        return events.stream()
                     .map(this::toDto)
                     .toList();
    }
}
