package com.hrms.project.repository;

import com.hrms.project.entity.WorkExperienceDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkExperienceRepository extends JpaRepository<WorkExperienceDetails, String> {
}