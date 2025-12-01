package com.hrms.project.repository;

import com.hrms.project.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {


    Optional<Department> findByDepartmentIdOrDepartmentName(String departmentId, String departmentName);

    Optional<Object> findByDepartmentName(String strip);
}
