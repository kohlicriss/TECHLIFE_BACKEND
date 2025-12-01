package com.hrms.project.repository;


import com.hrms.project.entity.Employee;
import com.hrms.project.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Page<Employee> findByDepartment_DepartmentId(String departmentId, Pageable pageable);

    Page<Employee> findByTeams_TeamId(String teamId, Pageable pageable);

    List<Employee> findByRole(Role role);
}
