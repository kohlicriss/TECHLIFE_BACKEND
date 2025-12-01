package com.hrms.project.repository;

import com.hrms.project.configuration.TaskId;
import com.hrms.project.entity.TaskUpdate;
import com.hrms.project.configuration.TaskUpdateId;
import feign.Param;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskUpdateRepository extends JpaRepository<TaskUpdate, TaskUpdateId> {

    @Query("SELECT MAX(t.id.updateNumber) " +
            "FROM TaskUpdate t " +
            "WHERE t.task.id.taskId = :taskId AND t.task.id.projectId = :projectId")
    Long findMaxUpdateNumberByTask(@Param("taskId") String taskId, @Param("projectId") String projectId);

    List<TaskUpdate> findByTask_Id_TaskIdAndTask_Id_ProjectIdAndChangesAndNote(
            String taskId,
            String projectId,
            String changes,
            String note
    );

}

