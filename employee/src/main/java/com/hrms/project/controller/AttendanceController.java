package com.hrms.project.controller;


import com.hrms.project.dto.AttendanceDTO;
import com.hrms.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/employee")
public class AttendanceController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/{projectId}/attendance")
    public ResponseEntity<List<AttendanceDTO>>getAttendance(@PathVariable String projectId){
        List<AttendanceDTO> attendance=projectService.getAttendance(projectId);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }

}
