package com.hrms.project.service;

import com.hrms.project.dto.EmployeeKPIResponse;
import com.hrms.project.dto.EmployeeProjectKPI;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KPIService {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    public EmployeeKPIResponse getEmployeeKPIs(String employeeId) {
        log.info("Fetching KPIs for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id={}", employeeId);
                    return new EmployeeNotFoundException("Employee not found");
                });

        List<EmployeeProjectKPI> projectKPIs = new ArrayList<>();

        for (EmployeeProject ep : employee.getEmployeeProjects()) {
            Project project = ep.getProject();
            log.debug("Calculating KPIs for projectId={} (title={})", project.getProjectId(), project.getTitle());

            int totalTasks = taskRepository.countByProject_ProjectIdAndEmployee_EmployeeId(project.getProjectId(), employeeId);
            int completedTasks = taskRepository.countByProject_ProjectIdAndEmployee_EmployeeIdAndStatus(project.getProjectId(), employeeId, "Completed");

            log.debug("Employee {} -> totalTasks={}, completedTasks={}", employeeId, totalTasks, completedTasks);

            int totalSprints = sprintRepository.countByProject_ProjectId(project.getProjectId());
            int completedSprints = sprintRepository.countByProject_ProjectIdAndStatus(project.getProjectId(), SprintStatus.COMPLETED);

            log.debug("Project {} -> totalSprints={}, completedSprints={}", project.getProjectId(), totalSprints, completedSprints);

            Map<String, String> kpis = new HashMap<>();
            kpis.put("Task Completion", completedTasks + "/" + totalTasks);
            kpis.put("Sprint Completion", completedSprints + "/" + totalSprints);

            double overallPerformance = calculateOverallPerformance(completedTasks, totalTasks, completedSprints, totalSprints);
            kpis.put("Overall Performance", String.format("%.0f%%", overallPerformance));

            String status = overallPerformance >= 75 ? "On Track" : "Needs Improvement";
            log.info("Employee {} in project {} has performance={} and status={}", employeeId, project.getProjectId(), overallPerformance, status);

            projectKPIs.add(new EmployeeProjectKPI(project.getProjectId(), project.getTitle(),project.getDescription(), kpis, status));
        }

        EmployeeKPIResponse response = new EmployeeKPIResponse(employeeId, employee.getFirstName() + " " + employee.getLastName(), projectKPIs);
        log.info("KPI calculation completed successfully for employeeId={}", employeeId);

        return response;
    }

    private double calculateOverallPerformance(int completedTasks, int totalTasks,
                                               int completedSprints, int totalSprints) {
        log.debug("Calculating overall performance: completedTasks={}, totalTasks={}, completedSprints={}, totalSprints={}",
                completedTasks, totalTasks, completedSprints, totalSprints);

        double weightedSum = 0;
        double totalWeight = 0;

        if (totalTasks > 0) {
            double taskScore = completedTasks * 100.0 / totalTasks;
            weightedSum += taskScore * 0.5;
            totalWeight += 0.5;
            log.debug("TaskScore={} weighted by 0.5", taskScore);
        }

        if (totalSprints > 0) {
            double sprintScore = completedSprints * 100.0 / totalSprints;
            weightedSum += sprintScore * 0.2;
            totalWeight += 0.2;
            log.debug("SprintScore={} weighted by 0.2", sprintScore);
        }

        if (totalWeight == 0) {
            log.warn("No tasks or sprints found — returning 0 performance");
            return 0;
        }

        double finalScore = weightedSum / totalWeight;
        log.debug("Final calculated performance score={}", finalScore);

        return finalScore;
    }

    public EmployeeProjectKPI getEmployeeProjectKPI(String employeeId, String projectId) {
        log.info("Fetching KPI for employeeId={} and projectId={}", employeeId, projectId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        int totalTasks = taskRepository.countByProject_ProjectIdAndEmployee_EmployeeId(projectId, employeeId);
        int completedTasks = taskRepository.countByProject_ProjectIdAndEmployee_EmployeeIdAndStatus(projectId, employeeId, "Completed");

        int totalSprints = sprintRepository.countByProject_ProjectId(projectId);
        int completedSprints = sprintRepository.countByProject_ProjectIdAndStatus(projectId, SprintStatus.COMPLETED);

        Map<String, String> kpis = new HashMap<>();
        kpis.put("Task Completion", completedTasks + "/" + totalTasks);
        kpis.put("Sprint Completion", completedSprints + "/" + totalSprints);

        double overallPerformance = calculateOverallPerformance(completedTasks, totalTasks, completedSprints, totalSprints);
        kpis.put("Overall Performance", String.format("%.0f%%", overallPerformance));

        String status = overallPerformance >= 75 ? "On Track" : "Needs Improvement";

        log.info("Employee {} in project {} performance={} and status={}", employeeId, projectId, overallPerformance, status);

        return new EmployeeProjectKPI(projectId, project.getTitle(),project.getDescription() ,kpis, status);
    }

}
