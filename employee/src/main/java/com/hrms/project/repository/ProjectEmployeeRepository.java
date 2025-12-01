package com.hrms.project.repository;

import com.hrms.project.entity.Employee;
import com.hrms.project.entity.EmployeeProject;
import com.hrms.project.entity.Project;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectEmployeeRepository extends JpaRepository<EmployeeProject,Long> {
    List<EmployeeProject> findByProject(Project project);
    List<EmployeeProject> findByEmployee(Employee employee);

    Optional<EmployeeProject> findByEmployeeAndProject(Employee employee, Project project);
    List<EmployeeProject> findByProject_ProjectId(String projectId);

    List<EmployeeProject> findByEmployee_EmployeeId(String employeeId);


    Optional<EmployeeProject> findByProject_ProjectIdAndEmployee_EmployeeId(String projectId, String employeeId);

    @Query(value = """
    SELECT 
        ep.role AS role,
        CASE 
            WHEN COUNT(t.task_id) = 0 THEN 0
            ELSE SUM(CASE WHEN LOWER(t.status) = 'completed' THEN 1 ELSE 0 END) * 100.0 / COUNT(t.task_id)
        END AS progress
    FROM employee.employee_project ep
    LEFT JOIN employee.task t 
        ON t.employee_id = ep.employee_id 
        AND t.project_id = ep.project_id
        AND EXTRACT(YEAR FROM t.created_date) = :year
        AND EXTRACT(MONTH FROM t.created_date) = :month
    GROUP BY ep.role
    """, nativeQuery = true)
    List<Object[]> findRoleProgressByMonth(@Param("year") int year, @Param("month") int month);
}
