package com.hrms.project.repository;

import com.hrms.project.configuration.TaskId;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.Project;
import com.hrms.project.entity.Task;
import org.springframework.data.repository.query.Param;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, TaskId> {


    Page<Task> findByEmployee_EmployeeId(String employeeId, Pageable pageable);

    Page<Task> findByCreatedBy(String tlId, Pageable pageable);

    boolean existsByTitleAndProject_ProjectIdAndEmployee_EmployeeId(@NotBlank(message = "Task title is required") @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters") String title, String projectId, String employeeId);


    //List<Task> findByProjectId(String projectId);

    List<Task> findByProject_ProjectId(String projectId);

    long countByProjectAndStatus(Project project, String status);

    long countByProjectAndStatusNot(Project project, String status);

    List<Task> findByEmployee_EmployeeIdAndProject_ProjectId(String employeeId, String projectId);


    Long countByEmployee_EmployeeIdAndProject_ProjectIdAndStatus(String employeeId, String projectId, String completed);

    Long countByEmployee_EmployeeIdAndProject_ProjectIdAndStatusNot(String employeeId, String projectId, String completed);


    @Query("SELECT t FROM Task t " +
            "WHERE t.project.projectId = :projectId " +
            "AND t.employee.employeeId = :employeeId")
    List<Task> findEmployeeTasksInProject(String projectId, String employeeId);

    @Query(value = """
            SELECT 
                e.employee_image AS employeeImage,
                e.employee_id AS employeeId,
                e.display_name AS employeeName,
                ep.role AS role,
                CASE 
                    WHEN COUNT(t.task_id) = 0 THEN 0
                    ELSE SUM(CASE WHEN LOWER(t.status) = 'completed' THEN 1 ELSE 0 END) * 100.0 / COUNT(t.task_id)
                END AS percentageCompleted,
                CONCAT(
                    SUM(CASE WHEN LOWER(t.status) = 'completed' THEN 1 ELSE 0 END),
                    '/',
                    COUNT(t.task_id)
                ) AS status
            FROM employee.employee_project ep
            JOIN employee.employee e ON ep.employee_id = e.employee_id
            LEFT JOIN employee.task t 
                ON t.employee_id = e.employee_id 
                AND t.project_id = ep.project_id
            WHERE ep.project_id = :projectId
            GROUP BY e.employee_image, e.employee_id, e.display_name, ep.role
            """, nativeQuery = true)
    List<Object[]> findTeamPerformanceWithRoleNative(@Param("projectId") String projectId);


    int countByProject_ProjectIdAndEmployee_EmployeeId(String projectId, String employeeId);

    int countByProject_ProjectIdAndEmployee_EmployeeIdAndStatus(String projectId, String employeeId, String completed);
}