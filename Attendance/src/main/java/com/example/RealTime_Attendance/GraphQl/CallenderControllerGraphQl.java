package com.example.RealTime_Attendance.GraphQl;

import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.RealTime_Attendance.Dto.CalendarDto;
import com.example.RealTime_Attendance.Dto.CalendarInputDto;
import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.Entity.CalendarEvent;
import com.example.RealTime_Attendance.Security.CheckPermission;
import com.example.RealTime_Attendance.Security.TypeVar;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.CalendarRepo;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class CallenderControllerGraphQl {

    @Autowired
    CalendarRepo calendarRepo;

    @Autowired
    AttendanceRepository attendanceRepo;

    @QueryMapping
    public List<CalendarDto> getEventsBetweenDates(@Argument LocalDate startDate, @Argument LocalDate endDate) {
        List<CalendarEvent> calendarEvent = calendarRepo.findEventsBetweenDates(startDate, endDate);
        List<CalendarDto> cd = new ArrayList<>();
        for (CalendarEvent c : calendarEvent) {
            CalendarDto cdt = new CalendarDto();
            cdt.setEvent(c.getEvent());
            cdt.setDate(c.getDate());
            cdt.setDescription(c.getDescription());
            cd.add(cdt);
        }
        return cd;
    }

    @QueryMapping
    public List<CalendarDto> getAllEmployeeDetailsOnDates(@Argument LocalDate date) {
        
        List<CalendarEvent> details = calendarRepo.findByDateBetweeen(date, date);
        List<Attendance> attendanceList = attendanceRepo.findByAllEmployeesBetweenDates(date, date);

        List<CalendarDto> cd = new ArrayList<>();

        // Optional: Pick first calendar event or leave null if none exist
        CalendarEvent calendarEvent = (details != null && !details.isEmpty()) ? details.get(0) : null;

        System.out.println("Calendar Details: " + details);
        System.out.println("Attendance List: " + attendanceList);

        if (attendanceList != null && !attendanceList.isEmpty()) {
            for (Attendance a : attendanceList) {
                CalendarDto dto = new CalendarDto();

                // If there's at least one calendar event, copy common info
                if (calendarEvent != null) {
                    dto = calendarEvent.toDto(calendarEvent);
                }

                dto.setEmployeeId(a.getEmployeeId());
                dto.setDate(a.getDate());
                dto.setEffectiveHours(a.getEffectiveHours());
                dto.setIsPresent(a.isAttended() ? "Present" : "Absent");
                dto.setDescription("<p>Attendance recorded for " + a.getEmployeeId() + "</p>");
                dto.setLogin(a.getLoginTime() != null ? a.getLoginTime().toLocalTime() : null);
                dto.setLogout(a.getLogoutTime() != null ? a.getLogoutTime().toLocalTime() : null);
                dto.setMode(a.getMode() != null ? a.getMode() : null);
                dto.setHoliday(calendarEvent != null && calendarEvent.isHoliday());
                dto.setFirstSection(a.isFirstSection());
                dto.setSecondSection(a.isSecondSection());
                dto.setConsiderPresent(a.isConsiderPresent());
                cd.add(dto);
            }
        }
        // If no attendance found but calendar event exists
        else if (calendarEvent != null) {
            CalendarDto dto = calendarEvent.toDto(calendarEvent);
            dto.setDescription("<p>No attendance record found for this date</p>");
            dto.setIsPresent("Absent");
            cd.add(dto);
        }

        return cd;
    }

    @QueryMapping
//    @CheckPermission(
//            value = "ATTENDANCE_EMPLOYEE_READ",
//            MatchParmName = "employeeId",
//            MatchParmFromType = "employeeId",
//            MatchParmForRoles = {"ROLE_EMPLOYEE"},
//            type = TypeVar.VARIABLE
//    )
    public List<CalendarDto> getDetailsBetweenDates(@Argument String employeeId,
        @Argument LocalDate startDate,
        @Argument LocalDate endDate) {
        log.info("Received to getDetailsBetween dates method for employee id {}", employeeId);
        List<CalendarEvent> details = calendarRepo.findByDateBetweeen(startDate, endDate);
        List<Attendance> attendanceList = attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        List<CalendarDto> cd = new ArrayList<>();

        Map<LocalDate, Attendance> attendanceMap = attendanceList != null
                ? attendanceList.stream().collect(Collectors.toMap(Attendance::getDate, a -> a))
                : new HashMap<>();
        System.out.println(details);
        System.out.println(attendanceList);
        if (details != null && !details.isEmpty()) {
            for (CalendarEvent c : details) {
                CalendarDto dto = c.toDto(c);
                dto.setHoliday(true); // default assumption for calendar events

                Attendance a = attendanceMap.get(c.getDate());
                if (a != null) {
                    dto.setEmployeeId(a.getEmployeeId());
                    dto.setDate(a.getDate());
                    dto.setEffectiveHours(a.calcEffectiveHours());
                    dto.setGrossHours(a.calcGrossHours());
                    dto.setIsPresent(a.isAttended() ? "Present" : "Absent");
                    dto.setDescription("<p>Attendance recorded for " + a.getEmployeeId() + "</p>");
                    dto.setLogin(a.getLoginTime() != null ? a.getLoginTime().toLocalTime() : null);
                    dto.setLogout(a.getLogoutTime() != null ? a.getLogoutTime().toLocalTime() : null);
                    dto.setMode(a.getMode() != null ? a.getMode() : null);
                    dto.setHoliday(false);
                    dto.setFirstSection(a.isFirstSection());
                    dto.setSecondSection(a.isSecondSection());
                    dto.setConsiderPresent(a.isConsiderPresent());
                } else {
                    dto.setIsPresent("Absent");
                }

                cd.add(dto);
            }
        }

        // If no calendar events but attendance exists
        if ((details == null || details.isEmpty()) && attendanceList != null && !attendanceList.isEmpty()) {
            for (Attendance a : attendanceList) {
                CalendarDto dto = new CalendarDto();
                dto.setEmployeeId(a.getEmployeeId());
                dto.setDate(a.getDate());
                dto.setEffectiveHours(a.calcEffectiveHours());
                dto.setIsPresent(a.isAttended() ? "Present" : "Absent");
                dto.setLogin(a.getLoginTime() != null ? a.getLoginTime().toLocalTime() : null);
                dto.setLogout(a.getLogoutTime() != null ? a.getLogoutTime().toLocalTime() : null);
                dto.setFirstSection(a.isFirstSection());
                dto.setSecondSection(a.isSecondSection());
                dto.setConsiderPresent(a.isConsiderPresent());
                dto.setHoliday(false);
                cd.add(dto);
            }
        }

        // If both lists are empty, return an empty list safely
        return cd;
    }

    @QueryMapping
//    @CheckPermission(value = "ADD_ATTENDANCE_DETAILS")
    public CalendarEvent addHoliday(LocalDate date, String description) {
        CalendarEvent ce = calendarRepo.findByDate(date);
        ce.setHoliday(true);
        ce.setDescription(description);
        return ce;
    }

    @MutationMapping
//    @CheckPermission(value = "ADD_ATTENDANCE_DETAILS")

    public List<CalendarEvent> addCalendarEntries(@Argument CalendarInputDto input) {
        // ✅ Handle null input
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        LocalDate start = input.getStartDate();
        LocalDate end = input.getEndDate();

        // ✅ Validate date fields
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        // ✅ Prevent invalid range
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<CalendarEvent> entries = new ArrayList<>();

        LocalDate current = start;
        while (!current.isAfter(end)) {
            CalendarEvent ev = calendarRepo.findByDate(current);
            CalendarEvent event = ev != null ? ev : new CalendarEvent();
            event.setDate(current);
            event.setEvent(input.getEvent() != null ? input.getEvent() : "No event title");
            event.setDescription(input.getDescription() != null ? input.getDescription() : "No description provided");
            event.setHoliday(input.isHoliday());
            entries.add(event);

            current = current.plusDays(1);
        }

        try {
            // ✅ Save all safely
            return calendarRepo.saveAll(entries);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save calendar entries: " + e.getMessage());
        }
    }

}
