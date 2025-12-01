package com.hrms.project.service;

import com.hrms.project.client.ChatEmployeeClient;
import com.hrms.project.client.NotificationClient;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.DepartmentNotFoundException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.*;
import com.hrms.project.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {


    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ArchiveRepository archiveRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ChatEmployeeClient chatEmployeeClient;

    @Autowired
    private ProjectEmployeeRepository employeeProjectRepository;

    @Autowired
    private ProjectOverViewRepository projectOverViewRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AboutRepository aboutRepository;

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeeDTO> createData(
            EmployeeDTO employeeDTO) throws IOException {

        log.info("Creating employee {}", employeeDTO.getEmployeeId());

        if (employeeRepository.findById(employeeDTO.getEmployeeId()).isPresent()) {
            throw new APIException("Employee already exists");
        }

        Employee employee = modelMapper.map(employeeDTO, Employee.class);

        if (employeeDTO.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(employeeDTO.getDepartmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException("Department not found with name: " + employeeDTO.getDepartmentId()));
            employee.setDepartment(dept);
        }

        employeeRepository.save(employee);
        log.info("Employee {} saved successfully", employee.getEmployeeId());


        try {

            ChatEmployeeDTO chatDTO = ChatEmployeeDTO.builder()
                    .employeeId(employee.getEmployeeId())
                    .displayName(employee.getDisplayName())
                    .employeeImage(employee.getEmployeeImage())
                    .build();

            chatEmployeeClient.addEmployee(chatDTO);
            log.info("Employee {} synced with Chat service", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to sync employee with chat service: {}", e.getMessage());
        }
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .message("New Employee Created: " + employee.getDisplayName())
                    .sender("ANASOL CONSULTANCY SERVICES")
                    .type("EMPLOYEE")
                    .link("/employees/" + employee.getEmployeeId())
                    .category("SYSTEM")
                    .kind("INFO")
                    .subject("Employee Creation Successful")
                    .build();
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send employee creation notification: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(modelMapper.map(employee, EmployeeDTO.class));

    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeeDTO> getEmployeeById(String id) {
        log.info("Thread [{}] - Fetching employee details for {}",
                Thread.currentThread().getName(), id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        EmployeeDTO employeeDTO = modelMapper.map(employee, EmployeeDTO.class);
        if (employee.getEmployeeImage() != null) {
            employeeDTO.setEmployeeImage(s3Service.generatePresignedUrl(employee.getEmployeeImage()));
        }
        log.info("Thread [{}] - Fetched employee details for {}",
                Thread.currentThread().getName(), id);
        return CompletableFuture.completedFuture(employeeDTO);

    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<PaginatedDTO<EmployeeDTO>> getAllEmployees(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching employees: pageNumber={}, pageSize={}, sortBy={}, sortOrder={}", pageNumber, pageSize, sortBy, sortOrder);
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Employee> employees = employeeRepository.findAll(pageable);

        if (employees.isEmpty()) {
            log.info("No employees found for pageNumber={} and pageSize={}", pageNumber, pageSize);
            return CompletableFuture.completedFuture(new PaginatedDTO<>());
        }
        List<EmployeeDTO> allEmployeeDTOs = employees.getContent().stream().map(employee -> {
            EmployeeDTO employeeDTO = modelMapper.map(employee, EmployeeDTO.class);

            if (employee.getEmployeeImage() != null && !employee.getEmployeeImage().isEmpty()) {
                employeeDTO.setEmployeeImage(s3Service.generatePresignedUrl(employee.getEmployeeImage()));
            } else {
                employeeDTO.setEmployeeImage(null);
            }

            return employeeDTO;
        }).toList();
        PaginatedDTO<EmployeeDTO> response = new PaginatedDTO<>();
        response.setContent(allEmployeeDTOs);
        response.setPageNumber(employees.getNumber());
        response.setPageSize(employees.getSize());
        response.setTotalElements(employees.getTotalElements());
        response.setTotalPages(employees.getTotalPages());
        response.setFirst(employees.isFirst());
        response.setLast(employees.isLast());
        response.setNumberOfElements(employees.getNumberOfElements());

        log.info("Fetched {} employees", allEmployeeDTOs.size());
        return CompletableFuture.completedFuture(response);
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeeDTO> updateEmployee(String id, EmployeeDTO employeeDTO) {
        log.info("Updating employee {}", id);

        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));

        if (employeeDTO.getDepartmentId() != null) {

            updateEmployee.setDepartment(null);
            employeeRepository.save(updateEmployee);

            Department department = departmentRepository.findById(employeeDTO.getDepartmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + employeeDTO.getDepartmentId()));
            updateEmployee.setDepartment(department);
        }

        modelMapper.map(employeeDTO, updateEmployee);
        employeeRepository.save(updateEmployee);

        log.info("Employee {} updated successfully", id);

        try {
            ChatEmployeeDTO chatDTO = ChatEmployeeDTO.builder()
                    .employeeId(updateEmployee.getEmployeeId())
                    .displayName(updateEmployee.getDisplayName())
                    .employeeImage(updateEmployee.getEmployeeImage())
                    .build();


            chatEmployeeClient.updateEmployee(updateEmployee.getEmployeeId(), chatDTO);
            log.info("Employee updated {} synced with Chat service", updateEmployee.getEmployeeId());

        } catch (Exception e) {
            log.error("Failed to update employee in chat service: {}", e.getMessage());
        }
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(updateEmployee.getEmployeeId())
                .message("Your profile has been updated successfully.")
                .sender("ANASOL CONSULTANCY SERVICES")
                .type("EMPLOYEE_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Employee Profile Updated")
                .link("/employees/" + updateEmployee.getEmployeeId())
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send update notification to employee {}: {}", updateEmployee.getEmployeeId(), e.getMessage());
        }

        return CompletableFuture.completedFuture(modelMapper.map(updateEmployee, EmployeeDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<ContactDetailsDTO> getEmployeeContactDetails(String employeeId) {
        log.info("fetching contact details of employee {}", employeeId);
        Employee employeeDetails = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        return CompletableFuture.completedFuture(modelMapper.map(employeeDetails, ContactDetailsDTO.class));


    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<List<ContactDetailsDTO>> getAllEmployeeContactDetails(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        List<Employee> contactDetails = employeeRepository.findAll(pageable).getContent();

        if (contactDetails.isEmpty()) {
            log.info("No employee contact details found for pageNumber={} and pageSize={}", pageNumber, pageSize);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        List<ContactDetailsDTO> dtoList = contactDetails.stream()
                .map(employee -> modelMapper.map(employee, ContactDetailsDTO.class))
                .toList();

        return CompletableFuture.completedFuture(dtoList);
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<ContactDetailsDTO> updateContactDetails(String employeeId, ContactDetailsDTO contactDetailsDTO) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        modelMapper.map(contactDetailsDTO, employee);
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(employee.getEmployeeId()) // notify the employee
                .message("Your contact details have been updated successfully.")
                .sender("HR")
                .type("CONTACT_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Contact Details Updated")
                .link("/profile/" + employee.getEmployeeId()+  "/profile")
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send contact details update notification to employee {}: {}", employee.getEmployeeId(), e.getMessage());
        }

        employeeRepository.save(employee);
        return CompletableFuture.completedFuture(modelMapper.map(employee, ContactDetailsDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<AddressDTO> getAddress(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        return CompletableFuture.completedFuture(modelMapper.map(employee, AddressDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<PaginatedDTO<AddressDTO>> getAllAddress(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Employee> employees = employeeRepository.findAll(pageable);
        if (employees.isEmpty()) {
            log.info("No employee address details found for pageNumber={} and pageSize={}", pageNumber, pageSize);

            return CompletableFuture.completedFuture(new PaginatedDTO<>());
        }
        List<AddressDTO> content = employees.getContent().stream()
                .map(employeeDetails -> modelMapper.map(employeeDetails, AddressDTO.class))
                .toList();
        PaginatedDTO<AddressDTO> response = new PaginatedDTO<>();
        response.setContent(content);
        response.setPageNumber(employees.getNumber());
        response.setPageSize(employees.getSize());
        response.setTotalPages(employees.getTotalPages());
        response.setTotalElements(employees.getTotalElements());
        response.setFirst(employees.isFirst());
        response.setLast(employees.isLast());
        response.setNumberOfElements(employees.getNumberOfElements());

        return CompletableFuture.completedFuture(response);
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<AddressDTO> updateEmployeeAddress(String employeeId, AddressDTO addressDTO) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        modelMapper.map(addressDTO, employee);
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(employee.getEmployeeId()) // notify the employee
                .message("Your address has been updated successfully.")
                .sender("HR")
                .type("ADDRESS_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Address Updated")
                .link("/profile/" + employee.getEmployeeId() + "profile")
                .createdAt(LocalDateTime.now())
                .build();
        notificationClient.send(notification);
        employeeRepository.save(employee);
        return CompletableFuture.completedFuture(modelMapper.map(employee, AddressDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeePrimaryDetailsDTO> getEmployeePrimaryDetails(String employeeId) {
        Employee employeeDetails = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        return CompletableFuture.completedFuture(modelMapper.map(employeeDetails, EmployeePrimaryDetailsDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeePrimaryDetailsDTO> updateEmployeeDetails(String employeeId, EmployeePrimaryDetailsDTO employeePrimaryDetailsDTO) {
        System.out.println(employeePrimaryDetailsDTO);
        Employee employeeDetails = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        modelMapper.map(employeePrimaryDetailsDTO, employeeDetails);
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(employeeDetails.getEmployeeId()) // notify the employee
                .message("Your personal details (name, DOB, marital status, blood group, etc.) have been updated successfully.")
                .sender("HR")
                .type("EMPLOYEE_PRIMARY_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Personal Details Updated")
                .link("/profile/" + employeeDetails.getEmployeeId() + "/profile")
                .createdAt(LocalDateTime.now())
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send personal details update notification to employee {}: {}", employeeId, e.getMessage());
        }
        employeeRepository.save(employeeDetails);
        return CompletableFuture.completedFuture(modelMapper.map(employeeDetails, EmployeePrimaryDetailsDTO.class));

    }

    @Override
    public CompletableFuture<JobDetailsDTO> getJobDetails(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        return CompletableFuture.completedFuture(modelMapper.map(employee, JobDetailsDTO.class));
    }

    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<JobDetailsDTO> updateJobDetails(String employeeId, JobDetailsDTO jobDetailsDTO) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));
        modelMapper.map(jobDetailsDTO, employee);
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(employee.getEmployeeId())       // Employee themselves
                .message("Your job details have been updated successfully.")
                .sender("HR")
                .type("JOB_DETAILS_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Job Details Updated")
                .link("/profile/" + employee.getEmployeeId() + "/profile")
                .createdAt(LocalDateTime.now())
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send job details update notification to employee {}: {}", employee.getEmployeeId(), e.getMessage());
        }
        employeeRepository.save(employee);
        return CompletableFuture.completedFuture(modelMapper.map(employee, JobDetailsDTO.class));
    }



    @Async("employeeTaskExecutor")
    @Override
    public CompletableFuture<EmployeeDTO> deleteEmployee(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        employeeRepository.save(employee);

        Archive archive = modelMapper.map(employee, Archive.class);
        archive.setDateOfLeaving(LocalDate.now());

        // Aadhaar
        if (employee.getAadhaarCardDetails() != null) {
            archive.setAadharNumber(employee.getAadhaarCardDetails().getAadhaarNumber());
            archive.setAadharImage(employee.getAadhaarCardDetails().getUploadAadhaar());
        }

        // PAN
        if (employee.getPanDetails() != null) {
            archive.setPanNumber(employee.getPanDetails().getPanNumber());
            archive.setPanImage(employee.getPanDetails().getPanImage());
        }

        // Passport
        if (employee.getPassportDetails() != null) {
            archive.setPassportNumber(employee.getPassportDetails().getPassportNumber());
            archive.setPassportImage(employee.getPassportDetails().getPassportImage());
        }

        // Department
        if (employee.getDepartment() != null) {
            archive.setDepartmentId(employee.getDepartment().getDepartmentId());
        }


        // Degree Certificates
        List<DegreeCertificates> documents = employee.getDegreeCertificates();
        if (documents != null && !documents.isEmpty()) {
            archive.setDegreeDocuments(
                    documents.stream().map(DegreeCertificates::getAddFiles).toList()
            );
        } else {
            log.info("No degree documents found for employee {}", employeeId);
        }

        // Projects (Updated to use EmployeProject)
        List<EmployeeProject> employeProjects = employeeProjectRepository.findByEmployee(employee);
        if (employeProjects != null && !employeProjects.isEmpty()) {
            archive.setProjectId(
                    employeProjects.stream()
                            .map(ep -> ep.getProject().getProjectId())
                            .toList()
            );
        } else {
            log.info("No projects assigned to employee {}", employeeId);
        }
        List<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            if (project.getTeamLeads() != null && project.getTeamLeads().contains(employee)) {
                project.getTeamLeads().remove(employee);
                projectRepository.save(project);
                log.info("Removed {} as team lead from project {}", employeeId, project.getProjectId());
            }
        }

        // Teams
        List<Team> teams = employee.getTeams();
        if (teams != null && !teams.isEmpty()) {
            archive.setTeamId(
                    teams.stream().map(Team::getTeamId).toList()
            );
        } else {
            log.info("No teams assigned to employee {}", employeeId);
        }

        archiveRepository.save(archive);

        // Remove employee from teams
        if (teams != null) {
            for (Team team : teams) {
                team.getEmployees().remove(employee);
            }
            employee.getTeams().clear();
        }

        // Remove employee-project links
        if (employeProjects != null && !employeProjects.isEmpty()) {
            for (EmployeeProject ep : employeProjects) {
                employeeProjectRepository.delete(ep);
            }
        }
        List<ProjectOverview> projectOverviews = projectOverViewRepository.findByManager(employee);
        if (projectOverviews != null && !projectOverviews.isEmpty()) {
            for (ProjectOverview overview : projectOverviews) {
                overview.setManager(null); // break the FK reference
            }
            projectOverViewRepository.saveAll(projectOverviews);
            log.info("Cleared manager references for {} project overviews.", projectOverviews.size());
        }

        aboutRepository.findByEmployee(employee).ifPresent(about -> {
            about.setEmployee(null);
            aboutRepository.save(about);
            log.info("Unlinked About record for employee {}", employeeId);
        });


        List<NotificationRequest> notifications = new ArrayList<>();

        // Notify employee
        notifications.add(NotificationRequest.builder()
                .receiver(employee.getEmployeeId())
                .message("Your account has been deactivated as of " + LocalDate.now())
                .sender("HR")
                .type("EMPLOYEE_DELETE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Account Deactivated")
                .link("/employees/" + employeeId)
                .build());

        // Notify department colleagues
        if (employee.getDepartment() != null) {
            List<Employee> deptEmployees = employee.getDepartment().getEmployee();
            for (Employee deptEmp : deptEmployees) {
                notifications.add(NotificationRequest.builder()
                        .receiver(deptEmp.getEmployeeId())
                        .message("Employee " + employee.getDisplayName() + " has left the department.")
                        .sender("HR")
                        .type("EMPLOYEE_DELETE")
                        .category("SYSTEM")
                        .kind("INFO")
                        .subject("Employee Left Department")
                        .link("/employees/" + employeeId)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        // Notify project colleagues
        if (employeProjects != null && !employeProjects.isEmpty()) {
            for (EmployeeProject ep : employeProjects) {
                Project project = ep.getProject();
                List<EmployeeProject> sameProjectEmployees = employeeProjectRepository.findByProject(project);

                for (EmployeeProject otherEP : sameProjectEmployees) {
                    notifications.add(NotificationRequest.builder()
                            .receiver(otherEP.getEmployee().getEmployeeId())
                            .message("Employee " + employee.getDisplayName() +
                                    " has been removed from project " + project.getProjectId())
                            .sender("HR")
                            .type("EMPLOYEE_DELETE")
                            .category("SYSTEM")
                            .kind("INFO")
                            .subject("Employee Removed From Project")
                            .link("/employees/" + employeeId)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        }

        // Notify team colleagues
        if (teams != null) {
            for (Team team : teams) {
                for (Employee teamEmp : team.getEmployees()) {
                    notifications.add(NotificationRequest.builder()
                            .receiver(teamEmp.getEmployeeId())
                            .message("Employee " + employee.getDisplayName() + " has been removed from team " + team.getTeamName())
                            .sender("HR")
                            .type("EMPLOYEE_DELETE")
                            .category("SYSTEM")
                            .kind("INFO")
                            .subject("Employee Removed From Team")
                            .link("/employees/" + employeeId)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        }

        try {
            notificationClient.sendList(notifications);
        } catch (Exception e) {
            log.error("Failed to send employee deletion notifications: {}", e.getMessage());
        }

        try {
            chatEmployeeClient.deleteEmployee(employeeId);
            log.info("Employee deleted in chat Service: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to delete employee in Chat service: {}", e.getMessage());
        }

        // Delete employee
        employeeRepository.delete(employee);
        log.info("Employee deleted successfully: {}", employeeId);

        return CompletableFuture.completedFuture(modelMapper.map(employee, EmployeeDTO.class));
    }
    @Override
    public List<RoleEmployeesDTO> getRolesForProject(String projectId) {
        log.info("Fetching roles and employees for project {}", projectId);

        List<EmployeeProject> employeeProjects = employeeProjectRepository.findByProject_ProjectId(projectId);

        if (employeeProjects.isEmpty()) {
            log.info("No employees found for project {}", projectId);
            return Collections.emptyList();
        }

        // Use LinkedHashMap to preserve insertion order and avoid case duplicates
        Map<String, List<EmployeeProject>> groupedByRole = new LinkedHashMap<>();

        for (EmployeeProject ep : employeeProjects) {
            if (ep.getEmployee() != null && ep.getRole() != null) {
                String lowerRole = ep.getRole().toLowerCase();
                groupedByRole.computeIfAbsent(lowerRole, r -> new ArrayList<>()).add(ep);
            }
        }

        // Convert each group into RoleEmployeesDTO
        List<RoleEmployeesDTO> result = groupedByRole.entrySet().stream()
                .map(entry -> {
                    String originalRole = entry.getValue().get(0).getRole();

                    List<EmployeeSimpleDTO> employees = entry.getValue().stream()
                            .map(ep -> {
                                String presignedImage = null;
                                if (ep.getEmployee().getEmployeeImage() != null && !ep.getEmployee().getEmployeeImage().isEmpty()) {
                                    try {
                                        presignedImage = s3Service.generatePresignedUrl(ep.getEmployee().getEmployeeImage());
                                    } catch (Exception e) {
                                        log.error("Failed to generate presigned URL for employee {}: {}",
                                                ep.getEmployee().getEmployeeId(), e.getMessage());
                                    }
                                }

                                return EmployeeSimpleDTO.builder()
                                        .employeeId(ep.getEmployee().getEmployeeId())
                                        .displayName(ep.getEmployee().getDisplayName())
                                        .employeeImage(presignedImage)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return RoleEmployeesDTO.builder()
                            .role(originalRole)
                            .count(employees.size())
                            .employees(employees)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Fetched {} roles for project {}", result.size(), projectId);
        return result;
    }
}
