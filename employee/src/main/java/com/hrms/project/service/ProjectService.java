package com.hrms.project.service;

import com.hrms.project.dto.*;
import com.hrms.project.entity.Project;
import org.jose4j.mac.MacUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public interface ProjectService {
    ProjectDTO saveProject(ProjectDTO projectDTO, MultipartFile details) throws IOException;

    PaginatedDTO<ProjectDTO> getAllProjects(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProjectDTO getProjectById(String id);

    ProjectDTO updateProject(String id, ProjectDTO projectDTO,MultipartFile details) throws IOException;

    ProjectDTO deleteProject(String id);

    PaginatedDTO<ProjectTableDataDTO> getAllProjectsData(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder,String employeeId);

    PaginatedDTO<ProjectStatusDataDTO> getProjectStatusData(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder,String employeeId);


    List<ProjectDTO> getProjectByEmployee(String employeeId);

    void refreshTaskCounts(Project project);
    void initializeProjectTaskCounts();

    Project getProjectByIdEntity(String projectId);

    double calculateStatusPercentage(Project project);

    String calculateDuration(Project project);

    Map<String, Object> getProjectDetails(String projectId);

    List<EmployeePerformanceDTO> getTeamPerformance(String projectId);

    List<AttendanceDTO> getAttendance(String projectId);

    EmployeeDetailsDTO getEmployeeDetailsInProject(String projectId, String employeeId);
}