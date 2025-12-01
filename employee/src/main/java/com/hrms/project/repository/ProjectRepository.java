package com.hrms.project.repository;


import com.hrms.project.dto.ProjectDTO;
import com.hrms.project.entity.Project;
import feign.Param;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {


   // Page<Project> findByEmployees_Employee_EmployeeId(String employeeId, Pageable pageable);

    Optional<Project> findByTitle(@NotBlank(message = "Project title is required") @Size(min = 3, max = 50, message = "Title must be between 3 and 50 characters") String title);

    @Query("SELECT p FROM Project p JOIN p.employeeProjects ep WHERE ep.employee.employeeId = :employeeId")
    Page<Project> findProjectsByEmployeeId(@Param("employeeId") String employeeId, Pageable pageable);

   // Page<Project> findByEmployeeId(String employeeId, Pageable pageable);

    Page<Project> findByEmployeeProjects_Employee_EmployeeId(String employeeId, Pageable pageable);

    //  Page<Project> findByEmployeeProjects_Employee_EmployeeId(String employeeId, Pageable pageable);
    //   Optional<ProjectDTO> findByTitle(@NotBlank(message = "Project title is required") @Size(min = 3, max = 50, message = "Title must be between 3 and 50 characters") String title);
}
