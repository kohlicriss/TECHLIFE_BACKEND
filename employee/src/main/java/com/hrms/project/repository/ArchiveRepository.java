package com.hrms.project.repository;

import com.hrms.project.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveRepository extends JpaRepository<Archive, String> {
}
