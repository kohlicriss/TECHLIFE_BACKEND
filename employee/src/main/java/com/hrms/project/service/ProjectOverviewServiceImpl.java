package com.hrms.project.service;

import com.hrms.project.dto.ManagerDTO;
import com.hrms.project.dto.ProjectOverViewDTO;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.Project;
import com.hrms.project.entity.ProjectOverview;
import com.hrms.project.entity.Role;
import com.hrms.project.handlers.DuplicateResourceException;
import com.hrms.project.handlers.ProjectNotFoundException;
import com.hrms.project.handlers.ResourceAlreadyExistsException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectOverViewRepository;
import com.hrms.project.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectOverviewServiceImpl {

    @Autowired
    private ProjectOverViewRepository projectOverViewRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public ProjectOverViewDTO createProjectOverView(String projectId, ProjectOverViewDTO dto) {
        log.info("Creating ProjectOverview for projectId={} with client={}", projectId, dto.getClient());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found with ID {}", projectId);
                    return new ProjectNotFoundException("Project not found: " + projectId);
                });

        boolean exists = projectOverViewRepository.existsByProject_ProjectIdAndClient(projectId, dto.getClient());
        if (exists) {
            log.warn("Duplicate ProjectOverview found for projectId={} and client={}", projectId, dto.getClient());
            throw new ResourceAlreadyExistsException(
                    "A project overview already exists for this project (" + projectId + ") and client (" + dto.getClient() + ")");
        }

        ProjectOverview entity = new ProjectOverview();
        entity.setClient(dto.getClient());
        entity.setTotal_cost(dto.getTotal_cost());
        entity.setDays_to_work(dto.getDays_to_work());
        entity.setPriority(dto.getPriority());
        entity.setStartedOn(dto.getStartedOn());
        entity.setEndDate(dto.getEndDate());
        entity.setTimeline_progress(dto.getTimeline_progress());
        entity.setDueAlert(dto.getDueAlert());
        entity.setProject(project);

        if (dto.getManager() != null && dto.getManager().getEmployeeId() != null) {
            log.debug("Assigning manager with ID {} to project {}", dto.getManager().getEmployeeId(), projectId);
            Employee manager = employeeRepository.findById(dto.getManager().getEmployeeId())
                    .orElseThrow(() -> {
                        log.error("Manager not found with ID {}", dto.getManager().getEmployeeId());
                        return new EmployeeNotFoundException("Manager not found: " + dto.getManager().getEmployeeId());
                    });

            if (manager.getRole() != Role.ROLE_MANAGER) {
                log.error("Employee {} is not a manager role", manager.getEmployeeId());
                throw new IllegalArgumentException("Employee is not a manager: " + manager.getEmployeeId());
            }
            entity.setManager(manager);
        }

        try {
            ProjectOverview saved = projectOverViewRepository.save(entity);
            log.info("Successfully created ProjectOverview for projectId={}", projectId);
            return mapToDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while saving ProjectOverview for projectId={}", projectId, ex);
            throw new DuplicateResourceException("A project overview already exists for this project ID: " + projectId);
        } catch (Exception ex) {
            log.error("Unexpected error while creating ProjectOverview for projectId={}", projectId, ex);
            throw new RuntimeException("Error creating ProjectOverview: " + ex.getMessage());
        }
    }

    public ProjectOverViewDTO updateOverview(ProjectOverViewDTO dto, String projectId) {
        log.info("Updating ProjectOverview for projectId={}", projectId);

        ProjectOverview existing = projectOverViewRepository.findByProject_ProjectId(projectId)
                .orElseThrow(() -> {
                    log.error("ProjectOverview not found for projectId={}", projectId);
                    return new ProjectNotFoundException("ProjectOverview not found for projectId: " + projectId);
                });

        existing.setClient(dto.getClient());
        existing.setTotal_cost(dto.getTotal_cost());
        existing.setDays_to_work(dto.getDays_to_work());
        existing.setPriority(dto.getPriority());
        existing.setStartedOn(dto.getStartedOn());
        existing.setEndDate(dto.getEndDate());
        existing.setTimeline_progress(dto.getTimeline_progress());
        existing.setDueAlert(dto.getDueAlert());

        if (dto.getManager() != null && dto.getManager().getEmployeeId() != null) {
            log.debug("Updating manager for projectId={} with managerId={}", projectId, dto.getManager().getEmployeeId());
            Employee manager = employeeRepository.findById(dto.getManager().getEmployeeId())
                    .orElseThrow(() -> {
                        log.error("Manager not found with ID {}", dto.getManager().getEmployeeId());
                        return new EmployeeNotFoundException("Manager not found: " + dto.getManager().getEmployeeId());
                    });

            if (manager.getRole() != Role.ROLE_MANAGER) {
                log.error("Employee {} is not a manager role", manager.getEmployeeId());
                throw new IllegalArgumentException("Employee is not a manager: " + manager.getEmployeeId());
            }

            existing.setManager(manager);
        } else {
            log.debug("Manager removed for projectId={}", projectId);
            existing.setManager(null);
        }

        ProjectOverview updated = projectOverViewRepository.save(existing);
        log.info("Successfully updated ProjectOverview for projectId={}", projectId);
        return mapToDTO(updated);
    }

    public ProjectOverViewDTO deleteOverview(String projectId) {
        log.info("Deleting ProjectOverview for projectId={}", projectId);

        ProjectOverview existing = projectOverViewRepository.findByProject_ProjectId(projectId)
                .orElseThrow(() -> {
                    log.error("ProjectOverview not found for projectId={}", projectId);
                    return new ProjectNotFoundException("ProjectOverview not found for projectId: " + projectId);
                });

        projectOverViewRepository.delete(existing);
        log.info("Successfully deleted ProjectOverview for projectId={}", projectId);
        return mapToDTO(existing);
    }

    public ProjectOverViewDTO getOverViewByProject(String projectId) {
        log.debug("Fetching ProjectOverview for projectId={}", projectId);

        ProjectOverview overview = projectOverViewRepository.findByProject_ProjectId(projectId)
                .orElseThrow(() -> {
                    log.error("ProjectOverview not found for projectId={}", projectId);
                    return new ProjectNotFoundException("ProjectOverview not found for projectId: " + projectId);
                });

        log.info("Fetched ProjectOverview for projectId={}", projectId);
        return mapToDTO(overview);
    }

    public List<ProjectOverViewDTO> getAll() {
        log.debug("Fetching all ProjectOverviews");
        List<ProjectOverview> projectOverviews = projectOverViewRepository.findAll();
        log.info("Fetched {} ProjectOverviews", projectOverviews.size());
        return projectOverviews.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ProjectOverViewDTO mapToDTO(ProjectOverview entity) {
        ProjectOverViewDTO dto = new ProjectOverViewDTO();
        dto.setClient(entity.getClient());
        dto.setTotal_cost(entity.getTotal_cost());
        dto.setDays_to_work(entity.getDays_to_work());
        dto.setPriority(entity.getPriority());
        dto.setStartedOn(entity.getStartedOn());
        dto.setEndDate(entity.getEndDate());
        dto.setTimeline_progress(entity.getTimeline_progress());
        dto.setDueAlert(entity.getDueAlert());

        if (entity.getManager() != null) {
            Employee m = entity.getManager();
            String fullName = m.getDisplayName() != null ? m.getDisplayName()
                    : String.join(" ",
                    m.getFirstName(),
                    m.getMiddleName() != null ? m.getMiddleName() : "",
                    m.getLastName() != null ? m.getLastName() : "").trim();
            dto.setManager(new ManagerDTO(m.getEmployeeId(), fullName));
        }
        return dto;
    }
}
