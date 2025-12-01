package com.hrms.project.repository;

import com.hrms.project.entity.Achievements;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementsRepository extends JpaRepository<Achievements, String> {

    long countByEmployeeEmployeeId(String employeeId);

    boolean existsByEmployeeEmployeeIdAndId(String employeeId, String newDegreeId);

    boolean existsByEmployeeEmployeeIdAndCertificationName(String employeeId, @NotBlank(message = "Certification name is required") @Size(min = 2, max = 100, message = "Certification name must be between 2 and 100 characters") String certificationName);

}
