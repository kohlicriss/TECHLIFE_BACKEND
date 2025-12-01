package com.hrms.project.repository;

import com.hrms.project.entity.DegreeCertificates;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DegreeCertificatesRepository extends JpaRepository<DegreeCertificates,String> {
    List<DegreeCertificates> findByEmployee_EmployeeId(String employeeId);

    boolean existsByEmployeeEmployeeIdAndDegreeType(String employeeId, String degreeType);

    long countByEmployeeEmployeeId(String employeeId);

    boolean existsByEmployeeEmployeeIdAndId(String employeeId, String newDegreeId);

    Optional<DegreeCertificates> findTopByEmployeeEmployeeIdOrderByIdDesc(String employeeId);
}