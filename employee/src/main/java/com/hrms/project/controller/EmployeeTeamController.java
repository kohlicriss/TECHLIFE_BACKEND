package com.hrms.project.controller;

import com.hrms.project.dto.*;
import com.hrms.project.security.CheckEmployeeAccess;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.ProjectService;
import com.hrms.project.service.TeamServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")

public class EmployeeTeamController {

    @Autowired
    private TeamServiceImpl teamService;

    @Autowired
    private ProjectService projectService;

    @PostMapping("/team")
    @CheckPermission("MY_TEAM_MY_TEAMS_CREATE_TEAM")
    public ResponseEntity<TeamController> createTeam(@Valid @RequestBody TeamController teamController) {
        TeamController createdTeam = teamService.saveTeam(teamController);
        return new ResponseEntity<>(createdTeam, HttpStatus.CREATED);
    }

    @GetMapping("/team/{employeeId}")
    @CheckPermission(
            value = "MY_TEAM_MY_TEAMS_MY_TEAMS"
    )
    public ResponseEntity<List<TeamResponse>> getTeamAllEmployees(@PathVariable String employeeId) {
        List<TeamResponse> teamList = teamService.getTeamAllEmployees(employeeId);
        return ResponseEntity.ok(teamList);
    }

    @GetMapping("/team/employee/{teamId}")
    @CheckPermission("MY_TEAM_MY_TEAMS_GET_TEAM")
    public ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable String teamId) {
        List<TeamResponse> employeeList = teamService.getAllTeamEmployees(teamId);
        return ResponseEntity.ok(employeeList);
    }



    @PutMapping("/team/employee/{teamId}")
    @CheckPermission(
            value = "MY_TEAM_MY_TEAMS_EDIT_TEAM")

    // @CheckEmployeeAccess(param = "id", roles = {"ADMIN", "HR","TEAM_LEAD","MANAGER"})
    public ResponseEntity<String> updateTeam(@PathVariable String teamId,
                                             @Valid @RequestBody TeamController teamDTO) {
        return new ResponseEntity<>(teamService.updateTeam(teamId,teamDTO), HttpStatus.OK);
    }


    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/{teamId}/team/employee")
    @CheckPermission("MY_TEAM_MY_TEAMS_GET_TEAMS")
    public ResponseEntity<PaginatedDTO<EmployeeTeamDTO>>getEmployeeByTeamId(@PathVariable Integer pageNumber,
                                                                            @PathVariable Integer pageSize,
                                                                            @PathVariable String sortBy,
                                                                            @PathVariable String sortOrder,
                                                                            @PathVariable String teamId) {
        return new ResponseEntity<>(teamService.getEmployeeByTeamId(pageNumber,pageSize,sortBy,sortOrder,teamId),HttpStatus.OK);

    }
    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/teams")
    @CheckPermission("MY_TEAM_MY_TEAMS_ALL_TEAMS")
    public ResponseEntity<PaginatedDTO<TeamResponse>> getAllTeams(@PathVariable Integer pageNumber,
                                                                  @PathVariable Integer pageSize,
                                                                  @PathVariable String sortBy,
                                                                  @PathVariable String sortOrder ) {
        PaginatedDTO<TeamResponse>teams=teamService.getAllTeams(pageNumber,pageSize,sortBy,sortOrder);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/team/projects/{teamId}")
    public ResponseEntity<List<String>> getProjectsByTeam(@PathVariable String teamId) {
        return new ResponseEntity<>(teamService.getProjectsByTeam(teamId),HttpStatus.OK) ;
    }


    @DeleteMapping("/{teamId}/team")
    @CheckPermission("MY_TEAM_MY_TEAMS_DELETE_TEAM")
    public ResponseEntity<String> deleteTeam(@PathVariable String teamId) {
        return new ResponseEntity<>(teamService.deleteTeam(teamId),HttpStatus.OK);
    }

    @GetMapping("/{projectId}/details")//teamlead team project manager
    public Map<String, Object> getProjectDetails(@PathVariable String projectId) {
        return projectService.getProjectDetails(projectId);
    }




}