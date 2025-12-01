package com.hrms.project.service;

import com.hrms.project.dto.SprintDTO;
import com.hrms.project.entity.Project;
import com.hrms.project.entity.Sprint;
import com.hrms.project.handlers.ProjectNotFoundException;
import com.hrms.project.handlers.ResourceAlreadyExistsException;
import com.hrms.project.handlers.ResourceNotFoundException;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.project.repository.SprintRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class SprintService {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelMapper modelMapper;


    public SprintDTO createSprint(String projectId, SprintDTO sprintDTO) {
        log.info("Creating sprint '{}' for projectId={}", sprintDTO.getSprintName(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found with id={}", projectId);
                    return new ProjectNotFoundException("Project not found with id: " + projectId);
                });

        boolean sprintExists = sprintRepository.existsBySprintNameAndProject_ProjectId(
                sprintDTO.getSprintName(), projectId);

        if (sprintExists) {
            log.error("Sprint '{}' already exists for projectId={}", sprintDTO.getSprintName(), projectId);
            throw new ResourceAlreadyExistsException("A sprint with the same name already exists for this project");
        }

        String newSprintId = generateNextSprintId(projectId);
        sprintDTO.setSprintId(newSprintId);

        Sprint sprint = modelMapper.map(sprintDTO, Sprint.class);
        sprint.setProject(project);

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint '{}' created successfully for projectId={} with sprintId={}",
                sprint.getSprintName(), projectId, saved.getSprintId());

        return modelMapper.map(saved, SprintDTO.class);
    }

    private String generateNextSprintId(String projectId) {
        List<Sprint> sprints = sprintRepository.findByProject_ProjectId(projectId);

        if (sprints.isEmpty()) {
            return projectId + "-SPRINT1";
        }

        int maxNum = sprints.stream()
                .map(Sprint::getSprintId)
                .filter(id -> id.startsWith(projectId + "-SPRINT"))
                .map(id -> id.replace(projectId + "-SPRINT", ""))
                .filter(num -> !num.isEmpty())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return projectId + "-SPRINT" + (maxNum + 1);
    }

    public List<SprintDTO> getSprintsByProject(String projectId) {
        log.info("Fetching sprints for projectId={}", projectId);

        if (!projectRepository.existsById(projectId)) {
            log.error("Project not found with id={}", projectId);
            throw new ProjectNotFoundException("Project not found with id: " + projectId);
        }

        List<Sprint> sprints = sprintRepository.findByProject_ProjectId(projectId);

        if (sprints.isEmpty()) {
            log.warn("No sprints found for projectId={}", projectId);
            return List.of();
        }

        log.info("Found {} sprints for projectId={}", sprints.size(), projectId);

        return sprints.stream()
                .sorted(Comparator.comparing(Sprint::getSprintId))
                .map(s -> modelMapper.map(s, SprintDTO.class))
                .toList();
    }

    public SprintDTO updateSprint(String projectId, String sprintId, SprintDTO sprintDTO) {
        log.info("Updating sprint={} for projectId={}", sprintId, projectId);

        Sprint sprint = sprintRepository.findBySprintIdAndProject_ProjectId(sprintId, projectId)
                .orElseThrow(() -> {
                    log.error("Sprint not found with sprintId={} under projectId={}", sprintId, projectId);
                    return new ResourceNotFoundException("Sprint not found with id: " + sprintId + " in project " + projectId);
                });

        sprint.setSprintName(sprintDTO.getSprintName());
        sprint.setStartDate(sprintDTO.getStartDate());
        sprint.setEndDate(sprintDTO.getEndDate());
        sprint.setStatus(sprintDTO.getStatus());

        Sprint updated = sprintRepository.save(sprint);
        log.info("Sprint '{}' updated successfully for projectId={}", sprint.getSprintName(), projectId);

        return modelMapper.map(updated, SprintDTO.class);
    }

    public void deleteSprint(String projectId, String sprintId) {
        log.info("Deleting sprint={} for projectId={}", sprintId, projectId);

        Sprint sprint = sprintRepository.findBySprintIdAndProject_ProjectId(sprintId, projectId)
                .orElseThrow(() -> {
                    log.error("Sprint not found with sprintId={} under projectId={}", sprintId, projectId);
                    return new ResourceNotFoundException("Sprint not found with id: " + sprintId + " in project " + projectId);
                });

        sprintRepository.delete(sprint);
        log.info("Sprint '{}' deleted successfully for projectId={}", sprint.getSprintName(), projectId);
    }
}
