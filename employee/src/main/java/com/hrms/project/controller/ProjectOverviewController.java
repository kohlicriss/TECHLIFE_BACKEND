package com.hrms.project.controller;

import com.hrms.project.dto.ProjectOverViewDTO;
import com.hrms.project.service.ProjectOverviewServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class ProjectOverviewController {

    @Autowired
    private ProjectOverviewServiceImpl projectOverviewService;

    @PostMapping("/create/ProjectOverview/{projectId}")
    public ResponseEntity<ProjectOverViewDTO> createProjectOverView(@PathVariable String projectId,@RequestBody ProjectOverViewDTO projectOverViewDTO){
        ProjectOverViewDTO project=projectOverviewService.createProjectOverView(projectId,projectOverViewDTO);
        return new ResponseEntity<>(project, HttpStatus.CREATED);
    }

    @PutMapping("/update/ProjectOverview/{projectId}")
    public ResponseEntity<ProjectOverViewDTO>updateProjectOverview(@RequestBody ProjectOverViewDTO projectOverViewDTO,@PathVariable String projectId){
        ProjectOverViewDTO projectOverView=projectOverviewService.updateOverview(projectOverViewDTO,projectId);
        return new ResponseEntity<>(projectOverView,HttpStatus.OK);
    }

    @DeleteMapping("/delete/{projectId}")
    public ResponseEntity<ProjectOverViewDTO> deleteOverview(@PathVariable String projectId) {
        ProjectOverViewDTO project = projectOverviewService.deleteOverview(projectId);
        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    @GetMapping("getAllProjectOverview")
    public ResponseEntity<List<ProjectOverViewDTO>>getAll(){
        List<ProjectOverViewDTO>project=projectOverviewService.getAll();
        return new ResponseEntity<>(project,HttpStatus.OK);
    }
    @GetMapping("overview/{projectId}")
    public ResponseEntity<ProjectOverViewDTO>getByProjectId(@PathVariable String projectId){
        ProjectOverViewDTO project=projectOverviewService.getOverViewByProject(projectId);
        return new ResponseEntity<>(project,HttpStatus.OK);
    }


}
