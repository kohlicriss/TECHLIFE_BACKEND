package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.*;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.handlers.ProjectNotFoundException;
import com.hrms.project.handlers.TeamNotFoundException;
import com.hrms.project.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeamServiceImpl {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ProjectEmployeeRepository projectEmployeeRepository;

    @Autowired
    private ProjectOverViewRepository projectOverViewRepository;

    public TeamController saveTeam(TeamController teamController) {
        log.info("Creating new team with name '{}'", teamController.getTeamName());

        Optional<Team> existingTeamByName = teamRepository.findByTeamName(teamController.getTeamName());
        if (existingTeamByName.isPresent()) {
            log.warn("Team creation failed. Team with name '{}' already exists", teamController.getTeamName());
            throw new APIException("A team with name '" + teamController.getTeamName() + "' already exists.");
        }

        List<String> allIds = teamRepository.findAll().stream()
                .map(Team::getTeamId)
                .filter(id -> id.startsWith("TEAM"))
                .toList();

        int maxId = allIds.stream()
                .map(id -> Integer.parseInt(id.replace("TEAM", "")))
                .max(Integer::compareTo)
                .orElse(0);

        String newTeamId = "TEAM" + String.format("%03d", maxId + 1);
        log.debug("Assigned new teamId={}", newTeamId);

      //  log.debug("Assigned new teamId={}", newTeamId);

        Project project = projectRepository.findById(teamController.getProjectId())
                .orElseThrow(() -> {
                    log.error("Project with ID {} not found", teamController.getProjectId());
                    return new ProjectNotFoundException("Project with ID doesn't exist " + teamController.getProjectId());
                });

        if (project.getTeam() != null) {
            log.warn("Project {} is already assigned to team {}", project.getProjectId(), project.getTeam().getTeamId());
            throw new APIException("Project is already assigned to team with ID " + project.getTeam().getTeamId());
        }

        Set<Employee> employees = new HashSet<>();
        if (teamController.getEmployeeRoles() != null) {
            for (Map.Entry<String, String> entry : teamController.getEmployeeRoles().entrySet()) {
                String employeeId = entry.getKey();
                String role = entry.getValue();

                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> {
                            log.error("Employee not found with ID {}", employeeId);
                            return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                        });

                employees.add(employee);

                boolean alreadyAssigned = projectEmployeeRepository.findByEmployee(employee).stream()
                        .anyMatch(ep -> ep.getProject().equals(project));

                if (!alreadyAssigned) {
                    EmployeeProject ep = new EmployeeProject();
                    ep.setEmployee(employee);
                    ep.setProject(project);
                    ep.setRole(role);
                    projectEmployeeRepository.save(ep);
                }
            }
        }

        log.debug("Assigning {} employees to new team", employees.size());

        Team team = new Team();
        team.setTeamId(newTeamId);
        team.setTeamName(teamController.getTeamName());
        team.setTeamDescription(teamController.getTeamDescription());
        team.setEmployees(employees);

        Team savedTeam = teamRepository.save(team);
        log.info("Team '{}' created successfully with teamId={}", savedTeam.getTeamName(), savedTeam.getTeamId());

        project.setTeam(savedTeam);
        projectRepository.save(project);

        employeeRepository.saveAll(employees);
        log.debug("Project {} assigned to team {} and employee projects updated", project.getProjectId(), savedTeam.getTeamId());

        List<NotificationRequest> notifications = team.getEmployees().stream()
                .map(emp -> NotificationRequest.builder()
                        .receiver(emp.getEmployeeId())
                        .category("TeamRelated")
                        .message("You have been assigned to team: " + team.getTeamName())
                        .sender("ADMIN")
                        .type("my-teams/" + emp.getEmployeeId())
                        .kind("INFO")
                        .subject("Team Assignment")
                        .link("/teams/" + savedTeam.getTeamId())
                        .build())
                .collect(Collectors.toList());

        try {
            notificationClient.sendList(notifications);
            log.info("Notifications sent to {} employees for team {}", notifications.size(), savedTeam.getTeamId());
        } catch (Exception e) {
            log.error("Failed to send notifications to team {}: {}", savedTeam.getTeamId(), e.getMessage());
        }

        teamController.setTeamId(savedTeam.getTeamId());
        return teamController;
    }

    public List<TeamResponse> getTeamAllEmployees(String employeeId) {
        log.info("Fetching all teams for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        List<Team> teamList = Optional.ofNullable(employee.getTeams()).orElse(Collections.emptyList());
        if (teamList.isEmpty()) {
            log.warn("No teams found for employeeId={}", employeeId);
            return Collections.emptyList();
        }

        List<TeamResponse> teamResponses = teamList.stream().map(team -> {
            String currentTeamId = Optional.ofNullable(team.getTeamId()).orElse("UNKNOWN");
            String teamName = Optional.ofNullable(team.getTeamName()).orElse("Unnamed Team");

            List<EmployeeTeamResponse> employeeResponses = Optional.ofNullable(team.getEmployees())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(emp -> {
                        EmployeeTeamResponse response = new EmployeeTeamResponse();
                        response.setEmployeeId(emp.getEmployeeId());
                        response.setDisplayName(emp.getDisplayName());
                        response.setWorkEmail(emp.getWorkEmail());
                        response.setWorkNumber(emp.getWorkNumber());
                        response.setJobTitlePrimary(emp.getJobTitlePrimary());

                        String role = Optional.ofNullable(emp.getEmployeeProjects())
                                .orElse(Collections.emptyList())
                                .stream()
                                .filter(ep -> ep != null
                                        && ep.getProject() != null
                                        && ep.getProject().getTeam() != null
                                        && currentTeamId.equals(ep.getProject().getTeam().getTeamId()))
                                .map(EmployeeProject::getRole)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse("No Role Assigned");

                        response.setRole(role);
                        return response;
                    })
                    .toList();

            TeamResponse teamResponse = new TeamResponse();
            teamResponse.setTeamId(currentTeamId);
            teamResponse.setTeamName(teamName);
            teamResponse.setEmployees(employeeResponses);

            return teamResponse;
        }).toList();

        return teamResponses;
    }

    public List<TeamResponse> getAllTeamEmployees(String teamId) {
                log.info("Fetching all employees for teamId={}", teamId);

                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> {
                            log.error("Team not found with id {}", teamId);
                            return new TeamNotFoundException("Team not found with id " + teamId);
                        });

                Set<Employee> employees = Optional.ofNullable(team.getEmployees()).orElse(Collections.emptySet());
                if (employees.isEmpty()) {
                    log.warn("No employees found for teamId={}", teamId);
                    return Collections.emptyList();
                }

                try {
                    final String currentTeamId = Optional.ofNullable(team.getTeamId()).orElse("UNKNOWN");

                    List<EmployeeTeamResponse> employeeList = employees.stream()
                            .map(emp -> {
                                EmployeeTeamResponse response = new EmployeeTeamResponse();
                                response.setEmployeeId(emp.getEmployeeId());
                                response.setDisplayName(emp.getDisplayName());
                                response.setWorkEmail(emp.getWorkEmail());
                                response.setWorkNumber(emp.getWorkNumber());
                                response.setJobTitlePrimary(emp.getJobTitlePrimary());

                                String role = null;
                                try {
                                    if ("Team Lead".equalsIgnoreCase(emp.getJobTitlePrimary())) {
                                        role = "Team Lead";
                                    } else {
                                        role = Optional.ofNullable(emp.getEmployeeProjects())
                                                .orElse(Collections.emptyList())
                                                .stream()
                                                .filter(ep -> ep.getProject() != null
                                                        && ep.getProject().getTeam() != null
                                                        && currentTeamId.equals(ep.getProject().getTeam().getTeamId()))
                                                .map(EmployeeProject::getRole)
                                                .findFirst()
                                                .orElse("Team Member");
                                    }
                                } catch (Exception e) {
                                    log.error("Error fetching role for employee {} in team {}: {}",
                                            emp.getEmployeeId(), currentTeamId, e.getMessage(), e);
                                }

                                response.setRole(role);
                                return response;
                            })
                            .toList();

                    TeamResponse teamResponse = new TeamResponse();
                    teamResponse.setTeamId(currentTeamId);
                    teamResponse.setTeamName(Optional.ofNullable(team.getTeamName()).orElse("Unnamed Team"));
                    teamResponse.setEmployees(employeeList);

                    return List.of(teamResponse);
                } catch (Exception e) {
                    log.error("Error processing team {}: {}", teamId, e.getMessage(), e);
                    throw new RuntimeException("Failed to fetch employees for team " + teamId, e);
                }
            }




            public String updateTeam(String teamId, TeamController teamController) {
        log.info("Updating team with teamId={}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with id {}", teamId);
                    return new TeamNotFoundException("Team not found with id " + teamId);
                });

        // Prepare new employee set and roles
        Set<Employee> newEmployees = new HashSet<>();
        Map<String, String> employeeRoles = teamController.getEmployeeRoles();

        if (employeeRoles != null && !employeeRoles.isEmpty()) {
            for (Map.Entry<String, String> entry : employeeRoles.entrySet()) {
                String employeeId = entry.getKey();
                String role = entry.getValue();

                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> {
                            log.error("Employee not found with id {}", employeeId);
                            return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                        });
                newEmployees.add(employee);

                // Remove old EmployeeProject link for this project if exists
                List<EmployeeProject> eps = projectEmployeeRepository.findByEmployee(employee);
                for (EmployeeProject ep : eps) {
                    if (ep.getProject().getTeam() != null && ep.getProject().getTeam().getTeamId().equals(teamId)) {
                        projectEmployeeRepository.delete(ep);
                    }
                }

                EmployeeProject ep = new EmployeeProject();
                ep.setEmployee(employee);
                ep.setProject(projectRepository.findById(teamController.getProjectId())
                        .orElseThrow(() -> new ProjectNotFoundException("Project with ID doesn't exist " + teamController.getProjectId())));
                ep.setRole(role);
                projectEmployeeRepository.save(ep);
            }
        }

        team.setTeamName(teamController.getTeamName());
        team.setTeamDescription(teamController.getTeamDescription());
        team.setEmployees(newEmployees);
        Team savedTeam = teamRepository.save(team);
        log.info("Team '{}' updated successfully", savedTeam.getTeamName());

        Project project = projectRepository.findById(teamController.getProjectId())
                .orElseThrow(() -> {
                    log.error("Project with ID {} not found", teamController.getProjectId());
                    return new ProjectNotFoundException("Project with ID doesn't exist " + teamController.getProjectId());
                });

        if (project.getTeam() != null && !project.getTeam().getTeamId().equals(team.getTeamId())) {
            log.warn("Project {} is already assigned to another team", project.getProjectId());
            throw new APIException("Project is already assigned to another team");
        }

        project.setTeam(savedTeam);
        projectRepository.save(project);

        if (!newEmployees.isEmpty()) {
            List<NotificationRequest> notifications = newEmployees.stream()
                    .map(emp -> NotificationRequest.builder()
                            .receiver(emp.getEmployeeId())
                            .category("general")
                            .message("You have been added/updated in team: " + savedTeam.getTeamName())
                            .sender("admin@example.com")
                            .type("TEAM_UPDATE")
                            .kind("INFO")
                            .subject("Team Update")
                            .link("/teams/" + savedTeam.getTeamId())
                            .build())
                    .collect(Collectors.toList());

            try {
                notificationClient.sendList(notifications);
                log.info("Notifications sent for team update to {} employees", notifications.size());
            } catch (Exception e) {
                log.error("Failed to send notifications for team {}: {}", teamId, e.getMessage());
            }
        }

        return "Team updated successfully.";
    }


    public List<String> getProjectsByTeam(String teamId) {
        log.info("Fetching projects for teamId={}", teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with id {}", teamId);
                    return new TeamNotFoundException("Team not found with id " + teamId);
                });
        return team.getProjects().stream()
                .map(Project::getProjectId)
                .toList();
    }


    @Transactional
    public String deleteTeam(String teamId) {
        log.info("Deleting team with teamId={}", teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with id {}", teamId);
                    return new TeamNotFoundException("Team not found with id " + teamId);
                });

        Set<Employee> employees = new HashSet<>(team.getEmployees());

        List<NotificationRequest> notifications = employees.stream()
                .map(emp -> NotificationRequest.builder()
                        .receiver(emp.getEmployeeId())
                        .category("TeamRelated")
                        .message("Team " + team.getTeamName() + " has been deleted.")
                        .sender("admin@example.com")
                        .type("TEAM_DELETION")
                        .kind("INFO")
                        .subject("Team Deleted")
                        .link("/teams")
                        .build())
                .toList();

        for (Employee employee : team.getEmployees()) {
            employee.getTeams().remove(team);
        }
        team.getEmployees().clear();

        for (Project project : team.getProjects()) {
            project.setTeam(null);
        }
        team.getProjects().clear();

        teamRepository.delete(team);
        log.info("Team {} deleted successfully", teamId);

        try {
            if (!notifications.isEmpty()) {
                notificationClient.sendList(notifications);
                log.info("Notifications sent for deleted team {}", teamId);
            }
        } catch (Exception e) {
            log.error("Failed to send notifications for deleted team {}: {}", teamId, e.getMessage());
        }

        return "Team deleted successfully.";
    }

    public PaginatedDTO<EmployeeTeamDTO> getEmployeeByTeamId(
            Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String teamId) {

        log.info("Fetching employees for teamId={} page={} size={}", teamId, pageNumber, pageSize);

        if (!teamRepository.existsById(teamId)) {
            log.error("Team not found with id {}", teamId);
            throw new TeamNotFoundException("Team not found with id: " + teamId);
        }

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Employee> employeePage = employeeRepository.findByTeams_TeamId(teamId, pageable);

        List<String> employeeIds = employeePage.getContent().stream()
                .map(Employee::getEmployeeId)
                .toList();

        EmployeeTeamDTO dto = new EmployeeTeamDTO();
        dto.setTeamId(teamId);
        dto.setEmployeeId(employeeIds);

        PaginatedDTO<EmployeeTeamDTO> response = new PaginatedDTO<>();
        response.setContent(List.of(dto));
        response.setPageNumber(employeePage.getNumber());
        response.setPageSize(employeePage.getSize());
        response.setTotalElements(employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setFirst(employeePage.isFirst());
        response.setLast(employeePage.isLast());
        response.setNumberOfElements(employeePage.getNumberOfElements());

        log.debug("Returning {} employees for teamId={}", employeeIds.size(), teamId);

        return response;
    }


    public PaginatedDTO<TeamResponse> getAllTeams(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching all teams - page: {}, size: {}", pageNumber, pageSize);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Team> teamPage = teamRepository.findAll(pageable);

        if (teamPage.isEmpty()) {
            log.info("No teams found");
            return new PaginatedDTO<>();
        }

        List<TeamResponse> teamResponses = teamPage.getContent().stream().map(team -> {
            List<EmployeeTeamResponse> employeeList = team.getEmployees().stream()
                    .map(emp -> {
                        EmployeeTeamResponse response = new EmployeeTeamResponse();
                        response.setEmployeeId(emp.getEmployeeId());
                        response.setDisplayName(emp.getDisplayName());
                        response.setWorkEmail(emp.getWorkEmail());
                        response.setWorkNumber(emp.getWorkNumber());
                        response.setJobTitlePrimary(emp.getJobTitlePrimary());
                        return response;
                    }).toList();

            TeamResponse response = new TeamResponse();
            response.setTeamId(team.getTeamId());
            response.setTeamName(team.getTeamName());
            response.setEmployees(employeeList);
            return response;
        }).toList();

        PaginatedDTO<TeamResponse> response = new PaginatedDTO<>();
        response.setContent(teamResponses);
        response.setPageNumber(teamPage.getNumber());
        response.setPageSize(teamPage.getSize());
        response.setTotalElements(teamPage.getTotalElements());
        response.setTotalPages(teamPage.getTotalPages());
        response.setFirst(teamPage.isFirst());
        response.setLast(teamPage.isLast());
        response.setNumberOfElements(teamPage.getNumberOfElements());

        log.info("Fetched {} teams", teamResponses.size());
        return response;
    }



}