package com.hrms.project.controller;

import com.hrms.project.dto.*;
import com.hrms.project.entity.Project;
import com.hrms.project.security.CheckEmployeeAccess;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.ProjectService;
import jakarta.validation.Valid;
import org.jose4j.mac.MacUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employee")

public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping("/project")
    @CheckPermission("CREATE_PROJECT")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestPart ProjectDTO projectDTO, @RequestPart(value = "details",required = false) MultipartFile details) throws IOException {
       ProjectDTO savedDTO= projectService.saveProject(projectDTO,details);
        return new ResponseEntity<>(savedDTO, HttpStatus.CREATED);
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/projects")
    @CheckPermission("FETCH_PROJECTS")
    public ResponseEntity<PaginatedDTO<ProjectDTO>> getAllProjects(@PathVariable Integer pageNumber,
                                                                   @PathVariable Integer pageSize,
                                                                   @PathVariable String sortBy,
                                                                   @PathVariable String sortOrder) {
        PaginatedDTO<ProjectDTO> response=projectService.getAllProjects(pageNumber,pageSize,sortBy,sortOrder);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable String id) {
        ProjectDTO projectResponse= projectService.getProjectById(id);
        return ResponseEntity.ok().body(projectResponse);
    }

    @PutMapping("/project/{id}")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable String id,
                                                    @Valid @RequestPart ProjectDTO projectDTO,
                                                    @RequestPart MultipartFile details) throws IOException {
        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO,details);
        return new ResponseEntity<>(updatedProject, HttpStatus.CREATED);
    }

    @DeleteMapping("/project/{id}")
    @CheckPermission("DELETE_PROJECT")
    public ResponseEntity<ProjectDTO> deleteProject(@PathVariable String id) {
        ProjectDTO deletedProject=projectService.deleteProject(id);
        return new ResponseEntity<>(deletedProject, HttpStatus.OK);
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/projectData/{employeeId}")
    @CheckPermission("ALL_PROJECT_DATA")
    public ResponseEntity<PaginatedDTO<ProjectTableDataDTO>>  getAllProjectData(@PathVariable Integer pageNumber,
                                                                        @PathVariable Integer pageSize,
                                                                        @PathVariable String sortBy,
                                                                        @PathVariable String sortOrder,@PathVariable String  employeeId) {
        return new ResponseEntity<>(projectService.getAllProjectsData(pageNumber,pageSize,sortBy,sortOrder,employeeId),HttpStatus.OK);
    }
    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/projectStatus/{employeeId}")
    @CheckPermission("ALL_PROJECT_STATUS")
    public ResponseEntity<PaginatedDTO<ProjectStatusDataDTO>> getAllProjectStatus(@PathVariable Integer pageNumber,
                                                                          @PathVariable Integer pageSize,
                                                                          @PathVariable String sortBy,
                                                                          @PathVariable String sortOrder,
                                                                          @PathVariable String employeeId) {
        return new ResponseEntity<>(projectService.getProjectStatusData(pageNumber,pageSize,sortBy,sortOrder,employeeId),HttpStatus.OK);
    }





    @GetMapping("projects/{employeeId}")
    public ResponseEntity<List<ProjectDTO>>projectByEmploye(@PathVariable String employeeId){
        List<ProjectDTO>projectDTOS=projectService.getProjectByEmployee(employeeId);
        return new ResponseEntity<>(projectDTOS,HttpStatus.OK);
    }
    @GetMapping("/{projectId}/status")
    public double getProjectStatus(@PathVariable String projectId) {
        Project project = projectService.getProjectByIdEntity(projectId);
        return projectService.calculateStatusPercentage(project);
    }

    @GetMapping("/{projectId}/duration")
    public String getProjectDuration(@PathVariable String projectId) {
        Project project = projectService.getProjectByIdEntity(projectId);
        return projectService.calculateDuration(project);
    }

    @GetMapping("/{projectId}/team-performance")
    public ResponseEntity<List<EmployeePerformanceDTO>> getTeamPerformance(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getTeamPerformance(projectId));
    }

    @GetMapping("/{projectId}/team-performance/{employeeId}")
    public ResponseEntity<EmployeeDetailsDTO> getEmployeeDetailsInProject(
            @PathVariable String projectId,
            @PathVariable String employeeId) {

        EmployeeDetailsDTO details = projectService.getEmployeeDetailsInProject(projectId, employeeId);
        return ResponseEntity.ok(details);
    }



}
