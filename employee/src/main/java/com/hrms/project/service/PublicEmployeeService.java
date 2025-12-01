package com.hrms.project.service;

import com.hrms.project.client.ChatEmployeeClient;
import com.hrms.project.dto.*;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.DepartmentRepository;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectEmployeeRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class PublicEmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ProjectEmployeeRepository projectEmployeeRepository;

    @Autowired
    private ChatEmployeeClient chatEmployeeClient;

    public PaginatedDTO<PublicEmployeeDetails> getAllEmployees(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching employees - pageNumber: {}, pageSize: {}, sortBy: {}, sortOrder: {}", pageNumber, pageSize, sortBy, sortOrder);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Employee> employeePage = employeeRepository.findAll(pageable);

        if (employeePage.isEmpty()) {
            log.warn("No employees found for pageNumber: {}", pageNumber);
            return new PaginatedDTO<>();
        }

        log.debug("Mapping employee entities to DTOs for {} records", employeePage.getNumberOfElements());

        List<PublicEmployeeDetails> content = employeePage.getContent().stream().map(emp -> {
            PublicEmployeeDetails dto = new PublicEmployeeDetails();
            dto.setEmployeeId(emp.getEmployeeId());
            dto.setName(emp.getDisplayName());
            dto.setWorkEmail(emp.getWorkEmail());
            dto.setLocation(emp.getLocation());
            dto.setJobTitlePrimary(emp.getJobTitlePrimary());

            if (emp.getDepartment() != null && emp.getDepartment().getDepartmentName() != null) {
                dto.setDepartment(emp.getDepartment().getDepartmentName());
            }

            if (emp.getEmployeeImage() != null && !emp.getEmployeeImage().isEmpty()) {
                dto.setEmployeeImage(s3Service.generatePresignedUrl(emp.getEmployeeImage()));
            }
            return dto;
        }).toList();

        PaginatedDTO<PublicEmployeeDetails> response = new PaginatedDTO<>();
        response.setContent(content);
        response.setPageNumber(employeePage.getNumber());
        response.setPageSize(employeePage.getSize());
        response.setTotalElements(employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setFirst(employeePage.isFirst());
        response.setLast(employeePage.isLast());
        response.setNumberOfElements(employeePage.getNumberOfElements());

        log.info("Returning {} employees on page {}", content.size(), pageNumber);
        return response;
    }

    public PublicEmployeeDetails getEmployeeDetails(String employeeId) {
        log.info("Fetching detailed info for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id: {}", employeeId);
                    return new EmployeeNotFoundException("Employee with id: " + employeeId + " not found");
                });

        log.debug("Employee found: {}", employee.getDisplayName());

        PublicEmployeeDetails details = new PublicEmployeeDetails();
        details.setEmployeeId(employee.getEmployeeId());
        details.setName(employee.getDisplayName());
        details.setWorkEmail(employee.getWorkEmail());
        details.setLocation(employee.getLocation());
        details.setJobTitlePrimary(employee.getJobTitlePrimary());

        if (employee.getDepartment() != null) {
            details.setDepartment(employee.getDepartment().getDepartmentName());
        }

        if (employee.getEmployeeImage() != null) {
            log.debug("Generating presigned URL for employee image");
            details.setEmployeeImage(s3Service.generatePresignedUrl(employee.getEmployeeImage()));
        }

        details.setContact(employee.getWorkNumber());
        details.setSkills(employee.getSkills());

        // ✅ Achievements
        if (employee.getAchievements() != null && !employee.getAchievements().isEmpty()) {
            log.debug("Mapping {} achievements", employee.getAchievements().size());
            details.setAchievements(employee.getAchievements().stream().map(a -> {
                AchievementsDTO dto = new AchievementsDTO();
                dto.setCertificationName(a.getCertificationName());
                dto.setIssuingAuthorityName(a.getIssuingAuthorityName());
                return dto;
            }).toList());
        }

        // ✅ Projects via EmployeProject
        List<EmployeeProject> employeProjects = projectEmployeeRepository.findByEmployee(employee);
        if (employeProjects != null && !employeProjects.isEmpty()) {
            log.debug("Mapping {} projects from EmployeProject", employeProjects.size());
            details.setProjects(employeProjects.stream().map(ep -> {
                Project p = ep.getProject();
                PublicProjectDTO projectDTO = new PublicProjectDTO();
                projectDTO.setProjectName(p.getTitle());
                projectDTO.setProjectDescription(p.getDescription());
               // projectDTO.setRole(ep.getRole()); // include role from join table
                return projectDTO;
            }).toList());
        }

        log.info("Successfully fetched detailed info for employeeId: {}", employeeId);
        return details;
    }


    // ✅ Get Header Details
    public PublicEmployeeDetails getHeaderDetails(String employeeId) {
        log.info("Fetching header details for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for header fetch with id: {}", employeeId);
                    return new EmployeeNotFoundException("Employee with id: " + employeeId + " not found");
                });

        PublicEmployeeDetails dto = new PublicEmployeeDetails();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getDisplayName());
        dto.setWorkEmail(employee.getWorkEmail());
        dto.setLocation(employee.getLocation());
        dto.setJobTitlePrimary(employee.getJobTitlePrimary());

        if (employee.getDepartment() != null) {
            dto.setDepartment(employee.getDepartment().getDepartmentName());
        }

        if (employee.getEmployeeImage() != null) {
            log.debug("Generating presigned URL for header image");
            dto.setEmployeeImage(s3Service.generatePresignedUrl(employee.getEmployeeImage()));
        }

        dto.setContact(employee.getWorkNumber());

        log.info("Header details fetched successfully for employeeId: {}", employeeId);
        return dto;
    }

    // ✅ Update Header
    public PublicEmployeeDetails updateHeader(String employeeId, @Valid PublicEmployeeDetails updateRequest) {
        log.info("Updating header info for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for update with id: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });

        if (updateRequest.getName() != null) {
            log.debug("Updating name: {}", updateRequest.getName());
            employee.setDisplayName(updateRequest.getName());
        }
        if (updateRequest.getWorkEmail() != null) {
            log.debug("Updating workEmail: {}", updateRequest.getWorkEmail());
            employee.setWorkEmail(updateRequest.getWorkEmail());
        }
        if (updateRequest.getLocation() != null) {
            log.debug("Updating location: {}", updateRequest.getLocation());
            employee.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getJobTitlePrimary() != null) {
            log.debug("Updating jobTitlePrimary: {}", updateRequest.getJobTitlePrimary());
            employee.setJobTitlePrimary(updateRequest.getJobTitlePrimary());
        }
        if (updateRequest.getDepartment() != null) {
            String deptInput = updateRequest.getDepartment().strip();
            log.debug("Validating department: {}", deptInput);

            Department dept = departmentRepository
                    .findByDepartmentIdOrDepartmentName(deptInput, deptInput)
                    .orElseThrow(() -> {
                        log.error("Department not found for input: {}", deptInput);
                        return new APIException("Department not found with ID or Name: " + deptInput);
                    });

            employee.setDepartment(dept);
        }
        if (updateRequest.getContact() != null) {
            log.debug("Updating contact: {}", updateRequest.getContact());
            employee.setWorkNumber(updateRequest.getContact());
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Header updated successfully for employeeId: {}", employeeId);
        try {
            ChatEmployeeDTO chatDTO = ChatEmployeeDTO.builder()
                    .employeeId(saved.getEmployeeId())
                    .displayName(saved.getDisplayName())
                    .employeeImage(saved.getEmployeeImage())
                    .build();

            chatEmployeeClient.updateEmployee(saved.getEmployeeId(), chatDTO);
            log.info("Employee header info synced with Chat service for ID {}", saved.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to sync updated header info with Chat service: {}", e.getMessage());
        }
        PublicEmployeeDetails response = new PublicEmployeeDetails();
        response.setEmployeeId(saved.getEmployeeId());
        response.setName(saved.getDisplayName());
        response.setWorkEmail(saved.getWorkEmail());
        response.setLocation(saved.getLocation());
        response.setJobTitlePrimary(saved.getJobTitlePrimary());
        if (saved.getDepartment() != null) {
            response.setDepartment(saved.getDepartment().getDepartmentName());
        }
        response.setContact(saved.getWorkNumber());

        return response;
    }
}
