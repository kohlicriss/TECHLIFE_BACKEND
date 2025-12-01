
package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.*;
import com.hrms.project.entity.Department;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.Task;
import com.hrms.project.handlers.DepartmentNotFoundException;
import com.hrms.project.handlers.DuplicateResourceException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.DepartmentRepository;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectEmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService{

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ProjectEmployeeRepository employeeProjectRepository;

    @Override
    public DepartmentDTO saveDepartment(DepartmentDTO departmentDTO) {
        log.info("Creating new department with name: {}", departmentDTO.getDepartmentName());

        String deptName = departmentDTO.getDepartmentName().strip();

        if (departmentRepository.findByDepartmentName(deptName).isPresent()) {
            throw new DuplicateResourceException("Department already exists with name: " + deptName);
        }
        long count=departmentRepository.count();
        String newDeptId="DEP"+ String.format("%03d",count);
        while(departmentRepository.findById(newDeptId).isPresent()){
            count++;
            newDeptId="DEP" + String.format("%03d",count);
        }

        Department dept= modelMapper.map(departmentDTO,Department.class);
        dept.setDepartmentId(newDeptId);
        Department saved= departmentRepository.save(dept);
        log.info("Department created successfully with ID: {}", saved.getDepartmentId());

        List<Employee> employeesToNotify = employeeRepository.findAll();
        log.debug("Notifying {} employees about new department {}", employeesToNotify.size(), saved.getDepartmentName());

        List<NotificationRequest> notifications = employeesToNotify.stream()
                .map(emp -> NotificationRequest.builder()
                        .receiver(emp.getEmployeeId())
                        .category("DEPARTMENTS")  // category for department notifications
                        .message("A new department has been created: " + saved.getDepartmentName())
                        .sender("HR")
                        .type("DEPARTMENT_CREATE")
                        .kind("INFO")
                        .subject("New Department Created")
                        .link("/department/" + saved.getDepartmentId())

                        .build())
                .collect(Collectors.toList());

        try {
            notificationClient.sendList(notifications);
        } catch (Exception e) {
            log.error("Failed to send department notifications: {}", e.getMessage());
        }
        return modelMapper.map(saved,DepartmentDTO.class);

    }


    @Override
    public PaginatedDTO<EmployeeDepartmentDTO> getEmployeesByDepartmentId(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String departmentId) {
        log.info("Fetching employees for department ID: {}", departmentId);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        Page<Employee> employeePage = employeeRepository.findByDepartment_DepartmentId(departmentId, pageable);

        log.info("Found {} employees in department {}", employeePage.getTotalElements(), dept.getDepartmentName());

        List<EmployeeDeptDTO> employeeTeamResponses = employeePage.getContent().stream()
                .map(emp -> {
                    EmployeeDeptDTO dto = modelMapper.map(emp, EmployeeDeptDTO.class);

                    // Assuming emp.getEmployeeImage() holds the S3 key (e.g., "emp-101/image.jpg")
                    if (emp.getEmployeeImage() != null) {
                        dto.setEmployeeImage(s3Service.generatePresignedUrl(emp.getEmployeeImage()));
                    } else {
                        dto.setEmployeeImage(null);
                    }

                    return dto;
                })
                .toList();


        EmployeeDepartmentDTO employeeDepartmentDTO = new EmployeeDepartmentDTO();
        employeeDepartmentDTO.setDepartmentId(dept.getDepartmentId());
        employeeDepartmentDTO.setDepartmentName(dept.getDepartmentName());
        employeeDepartmentDTO.setEmployeeList(employeeTeamResponses);

        PaginatedDTO<EmployeeDepartmentDTO> paginatedResponse = new PaginatedDTO<>();
        paginatedResponse.setContent(List.of(employeeDepartmentDTO));
        paginatedResponse.setPageNumber(employeePage.getNumber());
        paginatedResponse.setPageSize(employeePage.getSize());
        paginatedResponse.setTotalElements(employeePage.getTotalElements());
        paginatedResponse.setTotalPages(employeePage.getTotalPages());
        paginatedResponse.setFirst(employeePage.isFirst());
        paginatedResponse.setLast(employeePage.isLast());
        paginatedResponse.setNumberOfElements(employeePage.getNumberOfElements());

        return paginatedResponse;
    }


    @Override
    public DepartmentDTO updateDepartment(String departmentId, DepartmentDTO departmentDTO) {
        log.info("Updating department with ID: {}", departmentId);

        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        dept.setDepartmentName(departmentDTO.getDepartmentName());
        dept.setDepartmentDescription(departmentDTO.getDepartmentDescription());
        Department saved = departmentRepository.save(dept);
        log.info("Department {} updated successfully", saved.getDepartmentId());

        List<Employee> employeesToNotify = saved.getEmployee();
        log.debug("Notifying {} employees about department update {}", employeesToNotify.size(), saved.getDepartmentName());

        List<NotificationRequest> notifications = employeesToNotify.stream()
                .map(emp -> NotificationRequest.builder()
                        .receiver(emp.getEmployeeId())
                        .category("DEPARTMENTS")  // category for department notifications
                        .message("A  department has been updated: " + saved.getDepartmentName())
                        .sender("HR")
                        .type("DEPARTMENT_UPDATE")
                        .kind("INFO")
                        .subject("Updated Department")
                        .link("/department/" + saved.getDepartmentId())

                        .build())
                .collect(Collectors.toList());

        try {
            notificationClient.sendList(notifications);
        } catch (Exception e) {
            log.error("Failed to send department notifications: {}", e.getMessage());
        }
        return modelMapper.map(saved, DepartmentDTO.class);
    }


    @Override
    public PaginatedDTO<DepartmentDTO> getAllDepartmentDetails(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder) {
        log.info("Fetching all departments - page: {}, size: {}", pageNumber, pageSize);

        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Department> deptPage=departmentRepository.findAll(pageable);
        log.info("Found {} departments in page {}", deptPage.getNumberOfElements(), pageNumber);

        List<DepartmentDTO> departmentDTOS = deptPage.getContent().stream()
                .map(dept -> modelMapper.map(dept, DepartmentDTO.class))
                .toList();

        PaginatedDTO<DepartmentDTO> response = new PaginatedDTO<>();
        response.setContent(departmentDTOS);
        response.setPageNumber(deptPage.getNumber());
        response.setPageSize(deptPage.getSize());
        response.setTotalElements(deptPage.getTotalElements());
        response.setTotalPages(deptPage.getTotalPages());
        response.setFirst(deptPage.isFirst());
        response.setLast(deptPage.isLast());
        response.setNumberOfElements(deptPage.getNumberOfElements());

        return response;
    }

    @Override
    public DepartmentDTO getByDepartmentId(String departmentId) {
        log.info("Fetching department with ID: {}", departmentId);


        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("Department not found with ID: {}", departmentId);
                    return new DepartmentNotFoundException("Department not found with id: " + departmentId);
                });
        log.info("Successfully fetched department - ID: {}, Name: {}", dept.getDepartmentId(), dept.getDepartmentName());

        return modelMapper.map(dept ,DepartmentDTO.class);


    }

    @Override
    public PaginatedDTO<EmployeeDepartmentDTO> getEmployeeByEmployeeId(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder,String employeeId) {

        log.info("Fetching department details for employee ID: {}, page: {}, size: {}", employeeId, pageNumber, pageSize);

        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);


        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.warn("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });
        Department dept = employee.getDepartment();
        log.info("Employee {} belongs to department: {}", employeeId, dept.getDepartmentName());

        Page<Employee> employeePage = employeeRepository.findByDepartment_DepartmentId(dept.getDepartmentId(), pageable);

        List<EmployeeDeptDTO> employeeTeamResponses = employeePage.getContent().stream()
                .map(emp -> {
                    EmployeeDeptDTO dto = modelMapper.map(emp, EmployeeDeptDTO.class);

                    // Assuming emp.getEmployeeImage() holds the S3 key (e.g., "emp-101/image.jpg")
                    if (emp.getEmployeeImage() != null) {
                        dto.setEmployeeImage(s3Service.generatePresignedUrl(emp.getEmployeeImage()));
                    } else {
                        dto.setEmployeeImage(null);
                    }

                    return dto;
                })
                .toList();
        // Create EmployeeDepartmentDTO
        EmployeeDepartmentDTO employeeDepartmentDTO = new EmployeeDepartmentDTO();
        employeeDepartmentDTO.setDepartmentId(dept.getDepartmentId());
        employeeDepartmentDTO.setDepartmentName(dept.getDepartmentName());
        employeeDepartmentDTO.setEmployeeList(employeeTeamResponses);


        PaginatedDTO<EmployeeDepartmentDTO> response = new PaginatedDTO<>();
        response.setContent(List.of(employeeDepartmentDTO)); // Single department DTO per page
        response.setPageNumber(employeePage.getNumber());
        response.setPageSize(employeePage.getSize());
        response.setTotalElements(employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setFirst(employeePage.isFirst());
        response.setLast(employeePage.isLast());
        response.setNumberOfElements(employeePage.getNumberOfElements());

        log.info("Returning {} employees for department {}", employeeTeamResponses.size(), dept.getDepartmentName());

        return response;
    }

    @Override
    public String deleteDepartment(String departmentId) {
        log.info("Deleting department with ID: {}", departmentId);


        Department dept=departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("Department not found with ID: {}", departmentId);
                    return new DepartmentNotFoundException("Department not found with id: " + departmentId);
                });
        List<Employee> employees = dept.getEmployee();
        if (employees != null && !employees.isEmpty()) {
            List<NotificationRequest> notifications = employees.stream()
                    .map(emp -> NotificationRequest.builder()
                            .receiver(emp.getEmployeeId())
                            .category("DEPARTMENTS")
                            .message("Department '" + dept.getDepartmentName() + "' has been deleted.")
                            .sender("HR")
                            .type("DEPARTMENT_DELETE")
                            .kind("INFO")
                            .subject("Department Deleted")
                            .link("/departments")
                            .build())
                    .toList();

            try {
                notificationClient.sendList(notifications);
                log.info("Deletion notifications sent to {} employees", employees.size());

            } catch (Exception e) {
                log.error("Failed to send department deletion notifications: {}", e.getMessage());
            }
        }else
        {
            log.info("Department {} has no employees to notify", dept.getDepartmentName());


        }
        for (Employee emp : employees) {
            emp.setDepartment(null);
            employeeRepository.save(emp);
            log.debug("Removed department reference for employee {}", emp.getEmployeeId());

        }

        departmentRepository.delete(dept);
        log.info("Department {} deleted successfully", dept.getDepartmentName());


        return "Department deleted successfully";
    }
    public List<RoleProgressDTO> getDepartmentProgressByMonth(int year, int month) {
        List<Object[]> results = employeeProjectRepository.findRoleProgressByMonth(year, month);

        return results.stream()
                .map(row -> new RoleProgressDTO(
                        (String) row[0],
                        ((Number) row[1]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentDTO getEmployeeIdByDepartment(String employeeId) {
        Employee employee=employeeRepository.findById(employeeId)
                .orElseThrow(()->new EmployeeNotFoundException("employee not found"));

        Department department = employee.getDepartment();
        if (department == null) {
            log.warn("Employee {} does not belong to any department", employeeId);
            throw new DepartmentNotFoundException("No department assigned to employee ID: " + employeeId);
        }
        DepartmentDTO departmentDTO = modelMapper.map(department, DepartmentDTO.class);
        log.info("Employee {} belongs to department: {}", employeeId, departmentDTO.getDepartmentName());

        return departmentDTO;

    }

    @Override
    public DepartmentDTO updateEmployeeDepartment(String employeeId, String departmentId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));

        Department newDepartment = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        Department oldDepartment = employee.getDepartment();
        if (oldDepartment != null && oldDepartment.getDepartmentId().equals(departmentId)) {
            log.info("Employee {} is already in department {}", employeeId, departmentId);

            DepartmentDTO response = modelMapper.map(newDepartment, DepartmentDTO.class);
            response.setEmployeeId(employeeId);
            return response;
        }

        employee.setDepartment(newDepartment);
        employeeRepository.save(employee);

        log.info("Employee {} department updated from {} to {}",
                employeeId,
                oldDepartment != null ? oldDepartment.getDepartmentName() : "None",
                newDepartment.getDepartmentName());

        DepartmentDTO response = modelMapper.map(newDepartment, DepartmentDTO.class);
        response.setEmployeeId(employeeId);
        return response;
    }

}
