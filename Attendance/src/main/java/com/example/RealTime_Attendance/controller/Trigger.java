package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Dto.ScheduleAttendance;
import com.example.RealTime_Attendance.Schedule.Daily;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance/trigger")
@RequiredArgsConstructor
public class Trigger {

    private final Daily dailyService;

    @PostMapping("/add")
    public ResponseEntity<String> addJob(@RequestBody ScheduleAttendance scheduleAttendance)
            throws SchedulerException, ParseException {
        dailyService.scheduleAttendanceJob(scheduleAttendance);
        return ResponseEntity.ok("Job Scheduled Successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateJob(@RequestBody ScheduleAttendance scheduleAttendance)
            throws SchedulerException, ParseException {
        dailyService.updateAttendanceJob(scheduleAttendance);
        return ResponseEntity.ok("Job Updated Successfully");
    }

    @DeleteMapping("/delete/{shiftName}/{section}")
    public ResponseEntity<String> deleteJob(@PathVariable String shiftName, @PathVariable String section)
            throws SchedulerException {
        dailyService.deleteJob(shiftName, section);
        return ResponseEntity.ok("Job Deleted Successfully");
    }

    @GetMapping("/get/{shiftName}")
    public ResponseEntity<List<Map<String, Object>>> getJob(@PathVariable String shiftName)
            throws SchedulerException {
        return ResponseEntity.ok(dailyService.getJobByIdentity(shiftName));
    }

    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<String>> getJobsByGroup(@PathVariable String groupName)
            throws SchedulerException {
        return ResponseEntity.ok(dailyService.getJobsByGroup(groupName));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, String>>> getAllJobs() throws SchedulerException {
        return ResponseEntity.ok(dailyService.getAllJobs());
    }
}
