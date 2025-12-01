package com.hrms.project.controller;


import com.hrms.project.dto.SprintDTO;
import com.hrms.project.entity.Sprint;
import com.hrms.project.service.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/employee")
public class SprintsController {

    @Autowired
    private SprintService sprintService;

    @PostMapping("/sprints/project/{projectId}")
    public ResponseEntity<SprintDTO> createSprint(@PathVariable String projectId,
                                                  @RequestBody SprintDTO sprintDTO) {
        return new ResponseEntity<>(sprintService.createSprint(projectId, sprintDTO), HttpStatus.CREATED);
    }

    @GetMapping("/sprints/project/{projectId}")
    public ResponseEntity<List<SprintDTO>> getSprintsByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectId));
    }

    @PutMapping("/sprints/{projectId}/{sprintId}")
    public ResponseEntity<SprintDTO> updateSprint(@PathVariable String projectId,@PathVariable String sprintId,
                                                  @RequestBody SprintDTO sprintDTO) {
        return ResponseEntity.ok(sprintService.updateSprint(projectId,sprintId, sprintDTO));
    }

    @DeleteMapping("/sprints/{projectId}/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable String projectId,@PathVariable String sprintId) {
        sprintService.deleteSprint(projectId,sprintId);
        return ResponseEntity.noContent().build();
    }

}
