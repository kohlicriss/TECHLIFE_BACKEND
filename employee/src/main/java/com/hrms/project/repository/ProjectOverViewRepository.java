package com.hrms.project.repository;

import com.hrms.project.entity.Employee;
import com.hrms.project.entity.Project;
import com.hrms.project.entity.ProjectOverview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectOverViewRepository extends JpaRepository<ProjectOverview,Integer> {

    // Check if ProjectOverview exists by project entity
    boolean existsByProject(Project project);

    // Check if ProjectOverview exists by projectId and client
    boolean existsByProject_ProjectIdAndClient(String projectId, String client);

    // Find ProjectOverview by Project entity
    Optional<ProjectOverview> findByProject(Project project);

    // Find ProjectOverview by projectId
    Optional<ProjectOverview> findByProject_ProjectId(String projectId);


    List<ProjectOverview> findByManager(Employee manager);


}