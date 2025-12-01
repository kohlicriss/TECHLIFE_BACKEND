package com.hrms.project.repository;

import com.hrms.project.entity.About;
import com.hrms.project.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AboutRepository extends JpaRepository<About, Long> {
    Optional<About> findByEmployee(Employee employee);
    void deleteByEmployee(Employee employee);

    Optional<About> findByEmployee_EmployeeId(String employeeId);
}
