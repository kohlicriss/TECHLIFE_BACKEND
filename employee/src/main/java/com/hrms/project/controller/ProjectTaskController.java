package com.hrms.project.controller;

import com.hrms.project.dto.ProjectWithTasksDTO;
import com.hrms.project.service.TaskServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@Slf4j
@RequiredArgsConstructor
public class ProjectTaskController {

    private final TaskServiceImpl taskService;


    @GetMapping("/{projectId}/tasks-history")
    public ProjectWithTasksDTO getProjectTaskHistory(@PathVariable String projectId) {
        log.info("Fetching task history for projectId={}", projectId);
        return taskService.getProjectWithTaskHistory(projectId);
    }



}
