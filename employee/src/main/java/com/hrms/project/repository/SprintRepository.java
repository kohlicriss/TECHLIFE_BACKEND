package com.hrms.project.repository;

import com.hrms.project.entity.Sprint;
import com.hrms.project.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    int countByProject_ProjectId(String projectId);
    int countByProject_ProjectIdAndStatus(String projectId, SprintStatus status);

    List<Sprint> findByProject_ProjectId(String projectId);

    Sprint findTopByOrderBySprintIdDesc();

    boolean existsBySprintNameAndProject_ProjectId(String sprintName, String projectId);


    Optional<Sprint> findBySprintIdAndProject_ProjectId(String sprintId, String projectId);
}