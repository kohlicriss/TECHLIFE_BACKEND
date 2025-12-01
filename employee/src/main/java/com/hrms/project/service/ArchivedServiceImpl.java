package com.hrms.project.service;

import com.hrms.project.dto.ArchivedDTo;
import com.hrms.project.dto.PaginatedDTO;
import com.hrms.project.entity.Archive;
import com.hrms.project.repository.ArchiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ArchivedServiceImpl {

    @Autowired
    private ArchiveRepository archiveRepository;

    @Autowired
    private S3Service s3Service;

    @Async("employeeTaskExecutor")
    public CompletableFuture<PaginatedDTO<ArchivedDTo>> getEmployee(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching archived employees pageNumber={} pageSize={} sortBy={} sortOrder={}",
                pageNumber, pageSize, sortBy, sortOrder);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Archive> employeesPage = archiveRepository.findAll(pageable);

        if (employeesPage.isEmpty()) {
            log.warn("No employees found in archive");
            return  CompletableFuture.completedFuture(new PaginatedDTO<>());
        }

        List<ArchivedDTo> employeesWithUrls = employeesPage.getContent().stream().map(employee -> {
            ArchivedDTo dto = new ArchivedDTo();
            dto.setEmployeeId(employee.getEmployeeId());
            dto.setDisplayName(employee.getDisplayName());
            dto.setWorkEmail(employee.getWorkEmail());
            dto.setWorkNumber(employee.getWorkNumber());
            dto.setGender(employee.getGender());
            dto.setDateOfJoining(employee.getDateOfJoining());
            dto.setDateOfLeaving(employee.getDateOfLeaving());
            dto.setDepartmentId(employee.getDepartmentId());
            dto.setProjectId(employee.getProjectId());
            dto.setTeamId(employee.getTeamId());
            dto.setAadharNumber(employee.getAadharNumber());
            dto.setPanNumber(employee.getPanNumber());
            dto.setPassportNumber(employee.getPassportNumber());

            dto.setEmployeeImage(safePresign(employee.getEmployeeImage()));
            dto.setAadharImage(safePresign(employee.getAadharImage()));
            dto.setPanImage(safePresign(employee.getPanImage()));
            dto.setPassportImage(safePresign(employee.getPassportImage()));

            dto.setDegreeDocuments(
                    employee.getDegreeDocuments() != null
                            ? employee.getDegreeDocuments().stream().map(this::safePresign).toList()
                            : Collections.emptyList()
            );

            return dto;
        }).toList();

        PaginatedDTO<ArchivedDTo> response = new PaginatedDTO<>();
        response.setContent(employeesWithUrls);
        response.setPageNumber(employeesPage.getNumber());
        response.setPageSize(employeesPage.getSize());
        response.setTotalElements(employeesPage.getTotalElements());
        response.setTotalPages(employeesPage.getTotalPages());
        response.setFirst(employeesPage.isFirst());
        response.setLast(employeesPage.isLast());
        response.setNumberOfElements(employeesPage.getNumberOfElements());

        log.info("Fetched {} archived employee(s) from page {}", employeesWithUrls.size(), pageNumber);
        return CompletableFuture.completedFuture(response);
    }

    private String safePresign(String key) {
        if (key == null || key.isEmpty()) return null;
        return s3Service.generatePresignedUrl(key);
    }
}
