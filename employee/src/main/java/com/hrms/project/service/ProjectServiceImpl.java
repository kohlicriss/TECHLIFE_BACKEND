package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.*;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.handlers.ProjectNotFoundException;
import com.hrms.project.handlers.ResourceAlreadyExistsException;
import com.hrms.project.handlers.ResourceNotFoundException;
import com.hrms.project.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ProjectEmployeeRepository projectEmployeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectOverViewRepository projectOverViewRepository;

    @Autowired
    private AboutRepository aboutRepository;

    @Override
    public ProjectDTO saveProject(ProjectDTO projectDTO, MultipartFile details) throws IOException {
        log.info("Saving new project with title: {}", projectDTO.getTitle());

        try {
            if (projectRepository.findByTitle(projectDTO.getTitle()).isPresent()) {
                log.warn("Duplicate project title detected: {}", projectDTO.getTitle());
                throw new ResourceAlreadyExistsException("A project with the title '" + projectDTO.getTitle() + "' already exists");
            }

            long count = projectRepository.count();
            String newProjectId;
            do {
                newProjectId = "PRO" + String.format("%03d", ++count);
            } while (projectRepository.findById(newProjectId).isPresent());

            Project project = modelMapper.map(projectDTO, Project.class);
            project.setProjectId(newProjectId);
            project.setTeam(null);
            project.setOpenTask(0);
            project.setClosedTask(0);

            String s3Key = s3Service.uploadFile(project.getProjectId(), "project", details);
            project.setDetails(s3Key);

            if (projectDTO.getTeamLeadId() != null && !projectDTO.getTeamLeadId().isEmpty()) {
                List<Employee> teamLeads = projectDTO.getTeamLeadId().stream()
                        .map(id -> employeeRepository.findById(id)
                                .orElseThrow(() -> {
                                    log.error("Team lead not found: {}", id);
                                    return new EmployeeNotFoundException("Team lead not found: " + id);
                                }))
                        .map(this::validateTeamLead)
                        .collect(Collectors.toList());
                project.setTeamLeads(teamLeads);
            }

            Project savedProject = projectRepository.save(project);
            log.info("Project '{}' saved successfully with ID: {}", savedProject.getTitle(), savedProject.getProjectId());

            // Send notifications
            try {
                List<NotificationRequest> notifications = employeeRepository.findAll().stream()
                        .map(emp -> NotificationRequest.builder()
                                .receiver(emp.getEmployeeId())
                                .category("PROJECTS")
                                .message("A new project has been created: " + savedProject.getTitle())
                                .sender("HR")
                                .type("PROJECT_ASSIGN")
                                .kind("INFO")
                                .subject("New Project")
                                .link("/projects/" + savedProject.getProjectId())
                                .build())
                        .toList();

                notificationClient.sendList(notifications);
                log.info("Notifications sent successfully for project '{}'", savedProject.getTitle());
            } catch (Exception e) {
                log.error("Error while sending notifications for project '{}': {}", savedProject.getTitle(), e.getMessage());
            }

            ProjectDTO savedDTO = modelMapper.map(savedProject, ProjectDTO.class);
            savedDTO.setProjectId(savedProject.getProjectId());

            if (savedProject.getTeamLeads() != null && !savedProject.getTeamLeads().isEmpty()) {
                savedDTO.setTeamLeadId(savedProject.getTeamLeads().stream()
                        .map(Employee::getEmployeeId)
                        .toList());
            }

            return savedDTO;

        } catch (IOException e) {
            log.error("File upload failed while saving project '{}': {}", projectDTO.getTitle(), e.getMessage());
            throw new APIException("Failed to upload project details file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred while saving project '{}': {}", projectDTO.getTitle(), e.getMessage());
            throw new APIException("Unable to save project due to internal error");
        }
    }

    @Override
    public PaginatedDTO<ProjectDTO> getAllProjects(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching all projects: pageNumber={}, pageSize={}, sortBy={}, sortOrder={}", pageNumber, pageSize, sortBy, sortOrder);

        try {
            Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ?
                    Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
            Page<Project> projectPage = projectRepository.findAll(pageable);

            if (projectPage.isEmpty()) {
                log.warn("No projects found in database.");
                throw new ResourceNotFoundException("No projects found");
            }

            List<ProjectDTO> projectDTOs = projectPage.getContent().stream().map(proj -> {
                refreshTaskCounts(proj);

                ProjectDTO dto = modelMapper.map(proj, ProjectDTO.class);
                if (proj.getDetails() != null) {
                    dto.setDetails(s3Service.generatePresignedUrl(proj.getDetails()));
                }
                if (proj.getTeamLeads() != null && !proj.getTeamLeads().isEmpty()) {
                    dto.setTeamLeadId(proj.getTeamLeads().stream()
                            .map(Employee::getEmployeeId)
                            .toList());
                } else {
                    dto.setTeamLeadId(Collections.emptyList());
                }

                return dto;
            }).toList();

            PaginatedDTO<ProjectDTO> response = new PaginatedDTO<>();
            response.setContent(projectDTOs);
            response.setPageNumber(projectPage.getNumber());
            response.setPageSize(projectPage.getSize());
            response.setTotalElements(projectPage.getTotalElements());
            response.setTotalPages(projectPage.getTotalPages());
            response.setFirst(projectPage.isFirst());
            response.setLast(projectPage.isLast());
            response.setNumberOfElements(projectPage.getNumberOfElements());

            log.info("Returning {} projects successfully.", projectDTOs.size());
            return response;

        } catch (Exception e) {
            log.error("Error fetching projects: {}", e.getMessage());
            throw new APIException("Failed to fetch projects: " + e.getMessage());
        }
    }

    @Override
    public ProjectDTO getProjectById(String id) {
        log.info("Fetching project by ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {}", id);
                    return new ProjectNotFoundException("Project not found with id " + id);
                });

        refreshTaskCounts(project);

        ProjectDTO dto = modelMapper.map(project, ProjectDTO.class);
        if (project.getDetails() != null) {
            dto.setDetails(s3Service.generatePresignedUrl(project.getDetails()));
        }
        if (project.getTeamLeads() != null && !project.getTeamLeads().isEmpty()) {
            dto.setTeamLeadId(project.getTeamLeads().stream()
                    .map(Employee::getEmployeeId)
                    .toList());
        } else {
            dto.setTeamLeadId(Collections.emptyList());
        }

        log.info("Project '{}' retrieved successfully with team leads: {}", project.getTitle(), dto.getTeamLeadId());
        return dto;
    }

    @Override
    public ProjectDTO updateProject(String id, ProjectDTO projectDTO, MultipartFile details) throws IOException {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Project not found for update with ID: {}", id);
                    return new ProjectNotFoundException("Project not found with id " + id);
                });

        try {
            project.setTitle(projectDTO.getTitle());
            project.setDescription(projectDTO.getDescription());
            project.setProjectPriority(projectDTO.getProjectPriority());
            project.setProjectStatus(projectDTO.getProjectStatus());
            project.setStartDate(projectDTO.getStartDate());
            project.setEndDate(projectDTO.getEndDate());
            project.setOpenTask(projectDTO.getOpenTask());
            project.setClosedTask(projectDTO.getClosedTask());

            if (details != null && !details.isEmpty()) {
                log.info("Updating project details file for '{}'", id);
                String oldKey = project.getDetails();
                String newKey = s3Service.uploadFile(project.getProjectId(), "project", details);

                if (oldKey != null && !oldKey.equals(newKey)) {
                    s3Service.deleteFile(oldKey);
                    log.debug("Old project file deleted: {}", oldKey);
                }
                project.setDetails(newKey);
            }

            if (projectDTO.getTeamLeadId() != null && !projectDTO.getTeamLeadId().isEmpty()) {
                List<Employee> teamLeads = projectDTO.getTeamLeadId().stream()
                        .map(idLead -> employeeRepository.findById(idLead)
                                .orElseThrow(() -> new EmployeeNotFoundException("Team lead not found: " + idLead)))
                        .peek(emp -> {
                            if (emp.getRole() != Role.ROLE_TEAM_LEAD) {
                                log.warn("Invalid team lead role for employee: {}", emp.getEmployeeId());
                                throw new APIException("Employee is not a team lead: " + emp.getEmployeeId());
                            }
                        })
                        .collect(Collectors.toList());
                project.setTeamLeads(teamLeads);
            } else {
                project.getTeamLeads().clear();
            }

            Project savedProject = projectRepository.save(project);
            log.info("Project '{}' updated successfully", savedProject.getTitle());

            projectEmployeeRepository.findByProject(savedProject).forEach(ep -> {
                try {
                    notificationClient.send(NotificationRequest.builder()
                            .receiver(ep.getEmployee().getEmployeeId())
                            .category("PROJECTS")
                            .message("Project '" + savedProject.getTitle() + "' has been updated.")
                            .sender("HR")
                            .type("PROJECT_UPDATE")
                            .kind("INFO")
                            .subject("Project Updated")
                            .link("/projects/" + savedProject.getProjectId())
                            .build());
                    log.debug("Update notification sent to employee {}", ep.getEmployee().getEmployeeId());
                } catch (Exception e) {
                    log.error("Failed to send update notification to {}: {}", ep.getEmployee().getEmployeeId(), e.getMessage());
                }
            });

            ProjectDTO updatedDTO = modelMapper.map(savedProject, ProjectDTO.class);
            updatedDTO.setTeamLeadId(savedProject.getTeamLeads().stream()
                    .map(Employee::getEmployeeId)
                    .collect(Collectors.toList()));

            return updatedDTO;

        } catch (IOException e) {
            log.error("File handling error while updating project {}: {}", id, e.getMessage());
            throw new APIException("File upload failed during project update");
        } catch (Exception e) {
            log.error("Unexpected error during project update {}: {}", id, e.getMessage());
            throw new APIException("Project update failed due to internal error");
        }
    }

@Transactional
    @Override
    public ProjectDTO deleteProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + id));

        ProjectDTO dto = modelMapper.map(project, ProjectDTO.class);

        if (project.getDetails() != null && !project.getDetails().isEmpty()) {
            try {
                s3Service.deleteFile(project.getDetails());
                log.info("Deleted S3 file for project {}: {}", project.getProjectId(), project.getDetails());
            } catch (Exception e) {
                log.error("Failed to delete S3 file for project {}: {}", project.getProjectId(), e.getMessage());
            }
            dto.setDetails(null);
        }

        project.getTeamLeads().clear();
        projectRepository.delete(project);
        projectRepository.flush();

        return dto;
    }

    @Override
    public PaginatedDTO<ProjectTableDataDTO> getAllProjectsData(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String employeeId) {
        log.info("Fetching project table data for employeeId={}, pageNumber={}, pageSize={}", employeeId, pageNumber, pageSize);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Project> projectPage = projectRepository.findProjectsByEmployeeId(employeeId, pageable);

        log.info("Found {} projects for employee {}", projectPage.getTotalElements(), employeeId);

        List<ProjectTableDataDTO> content = projectPage.getContent().stream().map(project -> {
            ProjectTableDataDTO response = new ProjectTableDataDTO();
            response.setProject_id(project.getProjectId());
            response.setProject_name(project.getTitle());
            response.setStatus(project.getProjectStatus());
            response.setStart_date(project.getStartDate());
            response.setEnd_date(project.getEndDate());
            response.setDetails(project.getDescription());
            response.setPriority(project.getProjectPriority());

            List<String> employeeTeam = projectEmployeeRepository.findByProject(project).stream()
                    .map(eProj -> safePresign(eProj.getEmployee().getEmployeeImage()))
                    .toList();
            response.setEmployee_team(employeeTeam);

            return response;
        }).toList();

        PaginatedDTO<ProjectTableDataDTO> responseDTO = new PaginatedDTO<>();
        responseDTO.setContent(content);
        responseDTO.setPageNumber(projectPage.getNumber());
        responseDTO.setPageSize(projectPage.getSize());
        responseDTO.setTotalElements(projectPage.getTotalElements());
        responseDTO.setTotalPages(projectPage.getTotalPages());
        responseDTO.setFirst(projectPage.isFirst());
        responseDTO.setLast(projectPage.isLast());
        responseDTO.setNumberOfElements(projectPage.getNumberOfElements());

        return responseDTO;
    }


    private String safePresign(String key) {
        if (key == null || key.isEmpty()) return null;
        return s3Service.generatePresignedUrl(key);
    }

    public PaginatedDTO<ProjectStatusDataDTO> getProjectStatusData(Integer pageNumber,
                                                                   Integer pageSize,
                                                                   String sortBy,
                                                                   String sortOrder,
                                                                   String employeeId) {

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Page<Project> projectsPage = projectRepository
                .findByEmployeeProjects_Employee_EmployeeId(employeeId, pageable);

        List<ProjectStatusDataDTO> dtoList = projectsPage.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PaginatedDTO.<ProjectStatusDataDTO>builder()
                .content(dtoList)
                .pageNumber(projectsPage.getNumber() + 1)
                .pageSize(projectsPage.getSize())
                .totalElements(projectsPage.getTotalElements())
                .totalPages(projectsPage.getTotalPages())
                .last(projectsPage.isLast())
                .first(projectsPage.isFirst())
                .numberOfElements(projectsPage.getNumberOfElements())
                .build();
    }

    private ProjectStatusDataDTO mapToDTO(Project project) {
        ProjectStatusDataDTO dto = new ProjectStatusDataDTO();
        dto.setProject_id(project.getProjectId());
        dto.setProject_name(project.getTitle());
        dto.setDuration(calculateDuration(project));
        dto.setStatus(calculateStatusPercentage(project));
        return dto;
    }

    public String calculateDuration(Project project) {
        LocalDate start = project.getStartDate();
        LocalDate end = project.getEndDate() != null ? project.getEndDate() : LocalDate.now();
        long days = ChronoUnit.DAYS.between(start, end);
        return days + " days";
    }

    public double calculateStatusPercentage(Project project) {
        if (project.getAssignments() == null || project.getAssignments().isEmpty()) return 0.0;

        long totalTasks = project.getAssignments().size();
        long closedTasks = project.getAssignments().stream()
                .filter(t -> "Completed".equalsIgnoreCase(t.getStatus()))
                .count();

        return Math.round(((double) closedTasks / totalTasks) * 100); // returns % as double
    }




    @Override
    public List<ProjectDTO> getProjectByEmployee(String employeeId) {
        log.info("Fetching projects assigned to employee ID: {}", employeeId);

        List<EmployeeProject> employeeProjects = projectEmployeeRepository.findByEmployee_EmployeeId(employeeId);

        if (employeeProjects.isEmpty()) {
            log.warn("No projects found for employee ID: {}", employeeId);
            return Collections.emptyList();
        }

        List<ProjectDTO> projectDTOs = employeeProjects.stream()
                .map(EmployeeProject::getProject)
                .filter(Objects::nonNull)
                .map(project -> {
                    ProjectDTO dto = modelMapper.map(project, ProjectDTO.class);
                    dto.setTeamLeadId(project.getTeamLeads().stream()
                            .map(Employee::getEmployeeId)
                            .collect(Collectors.toList()));
                    dto.setProjectId(project.getProjectId());
                    dto.setTitle(project.getTitle());
                    dto.setDescription(project.getDescription());
                    dto.setProjectPriority(project.getProjectPriority());
                    dto.setProjectStatus(project.getProjectStatus());
                    dto.setStartDate(project.getStartDate());
                    dto.setEndDate(project.getEndDate());
                    dto.setOpenTask(project.getOpenTask());
                    dto.setClosedTask(project.getClosedTask());
                    return dto;
                })
                .distinct()
                .collect(Collectors.toList());

        log.info("Found {} projects for employee ID: {}", projectDTOs.size(), employeeId);
        return projectDTOs;
    }

    @Transactional
    public void refreshTaskCounts(Project project) {
        if (project == null) return;

        long closed = taskRepository.countByProjectAndStatus(project, "Completed");

        long open = taskRepository.countByProjectAndStatusNot(project, "Completed");

        project.setClosedTask((int) closed);
        project.setOpenTask((int) open);

        projectRepository.save(project);

        log.debug("✅ Updated task counts for project {}: open={}, closed={}",
                project.getProjectId(), open, closed);
    }

    @PostConstruct
    public void initializeProjectTaskCounts() {
        List<Project> allProjects = projectRepository.findAll();
        for (Project project : allProjects) {
            refreshTaskCounts(project);
        }
        System.out.println("✅ Project task counts initialized successfully");
    }

    @Override
    public Project getProjectByIdEntity(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
    }

    @Override
    public Map<String, Object> getProjectDetails(String projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        ProjectOverview overview = projectOverViewRepository.findByProject(project).orElse(null);


        List<Map<String, Object>> teamMembers = Optional.ofNullable(project.getEmployeeProjects())
                .orElse(Collections.emptyList())
                .stream()
                .map(ep -> {
                    Map<String, Object> member = new HashMap<>();
                    member.put("employeeId", ep.getEmployee().getEmployeeId());
                    member.put("displayName", ep.getEmployee().getDisplayName());
                    if (ep.getEmployee().getEmployeeImage() != null) {
                        String presignedUrl = s3Service.generatePresignedUrl(ep.getEmployee().getEmployeeImage());
                        member.put("employeeImage", presignedUrl);
                    } else {
                        member.put("employeeImage", null);
                    }                    member.put("role", ep.getRole() != null ? ep.getRole() : "Team Member");
                    return member;
                }).collect(Collectors.toList());


        List<Map<String, Object>> teamLeads = Optional.ofNullable(project.getTeamLeads())
                .orElse(Collections.emptyList())
                .stream()
                .map(emp -> {
                    Map<String, Object> lead = new HashMap<>();
                    lead.put("employeeId", emp.getEmployeeId());
                    lead.put("displayName", emp.getDisplayName());
                    if (emp.getEmployeeImage() != null && !emp.getEmployeeImage().isEmpty()) {
                        try {
                            String presignedUrl = s3Service.generatePresignedUrl(emp.getEmployeeImage());
                            lead.put("employeeImage", presignedUrl);
                        } catch (Exception e) {
                            lead.put("employeeImage", null);
                        }
                    } else {
                        lead.put("employeeImage", null);
                    }
                    lead.put("role", "Team Lead");
                    return lead;
                }).collect(Collectors.toList());


        Map<String, Object> manager = null;
        if (overview != null && overview.getManager() != null) {
            manager = new HashMap<>();
            manager.put("employeeId", overview.getManager().getEmployeeId());
            manager.put("displayName", overview.getManager().getDisplayName());
            if (overview.getManager().getEmployeeImage() != null && !overview.getManager().getEmployeeImage().isEmpty()) {
                try {
                    String presignedUrl = s3Service.generatePresignedUrl(overview.getManager().getEmployeeImage());
                    manager.put("employeeImage", presignedUrl);
                } catch (Exception e) {
                    manager.put("employeeImage", null);
                }
            } else {
                manager.put("employeeImage", null);
            }            manager.put("role", "Manager"); // <-- Explicit Role
        }


        Map<String, Object> response = new LinkedHashMap<>();
        response.put("projectId", project.getProjectId());
        response.put("projectName", project.getTitle());
        response.put("description", project.getDescription());
        response.put("projectManager", manager);
        response.put("teamLeads", teamLeads);
        response.put("teamMembers", teamMembers);

        return response;
    }


    @Override
    public List<EmployeePerformanceDTO> getTeamPerformance(String projectId) {
        List<Object[]> results = taskRepository.findTeamPerformanceWithRoleNative(projectId);
        List<EmployeePerformanceDTO> performanceList = new ArrayList<>();

        for (Object[] row : results) {
            String employeeImageKey = (String) row[0]; // S3 object key (e.g. emp-1/employeeImage.png)
            String employeeId = (String) row[1];
            String employeeName = (String) row[2];
            String role = (String) row[3];
            double percentageCompleted = ((Number) row[4]).doubleValue();
            String status = (String) row[5];

            String employeeImageUrl = null;
            if (employeeImageKey != null && !employeeImageKey.isEmpty()) {
                employeeImageUrl = s3Service.generatePresignedUrl(employeeImageKey);
            }

            percentageCompleted = Math.round(percentageCompleted * 100.0) / 100.0;

            if ("0/0".equals(status)) {
                status = "No tasks assigned";
            }

            performanceList.add(new EmployeePerformanceDTO(
                    employeeImageUrl, // 👈 pre-signed URL instead of key
                    employeeId,
                    employeeName,
                    percentageCompleted,
                    status,
                    role
            ));
        }

        return performanceList;
    }

    private Employee validateTeamLead(Employee emp) {
        if (emp.getRole() != Role.ROLE_TEAM_LEAD) {
            throw new RuntimeException("Selected employee is not a team lead: " + emp.getEmployeeId());
        }
        return emp;
    }


    @Override
    public List<AttendanceDTO> getAttendance(String projectId) {
        LocalDate today=LocalDate.now();

        Project project=projectRepository.findById(projectId).
                orElseThrow(()->new ProjectNotFoundException("Project not found "+projectId));
        List<EmployeeProject>team=projectEmployeeRepository.findByProject(project);
        List<AttendanceDTO>attendanceList=new ArrayList<>();
        for(EmployeeProject emp:team){

            AttendanceDTO dto=new AttendanceDTO(
            emp.getEmployee().getEmployeeId(),
            emp.getEmployee().getDisplayName(),
            today,
            null,
            emp.getRole());

            attendanceList.add(dto);
        }

        return attendanceList;
    }

    @Override
    public EmployeeDetailsDTO getEmployeeDetailsInProject(String projectId, String employeeId) {

        EmployeeProject employeeProject = projectEmployeeRepository
                .findByProject_ProjectIdAndEmployee_EmployeeId(projectId, employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found in this project"));

        Employee employee=employeeProject.getEmployee();

        String description = aboutRepository.findByEmployee_EmployeeId(employeeId)
                .map(About::getAbout)
                .orElse(null);

        EmployeeDetailsDTO dto = new EmployeeDetailsDTO();
        if (employee.getEmployeeImage() != null) {
            dto.setEmployeeImage(s3Service.generatePresignedUrl(employee.getEmployeeImage()));
        }
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setEmployeeName(employee.getDisplayName());
        dto.setRole(employeeProject.getRole());
        dto.setEmail(employee.getWorkEmail());
        dto.setContactNumber(employee.getWorkNumber());
        dto.setDescription(description);
        dto.setStatus(null);
        return dto;
    }

}