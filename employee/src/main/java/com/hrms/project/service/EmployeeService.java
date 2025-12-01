package com.hrms.project.service;

import com.hrms.project.dto.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Component
public interface EmployeeService {

    CompletableFuture<EmployeeDTO> createData(EmployeeDTO employeeDTO) throws IOException;

    CompletableFuture<EmployeeDTO> getEmployeeById(String id);

   CompletableFuture<PaginatedDTO<EmployeeDTO>>getAllEmployees( Integer pageNumber,Integer pageSize, String sortBy, String sortOrder);


    CompletableFuture<EmployeeDTO> deleteEmployee(String id);

    CompletableFuture<EmployeeDTO> updateEmployee(String id,EmployeeDTO employeeDTO);

    CompletableFuture<ContactDetailsDTO> getEmployeeContactDetails(String employeeId);

    CompletableFuture<List<ContactDetailsDTO> >getAllEmployeeContactDetails(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder);

    CompletableFuture<ContactDetailsDTO> updateContactDetails(String employeeId,ContactDetailsDTO contactDetailsDTO);

    CompletableFuture<AddressDTO> getAddress(String employeeId);

    CompletableFuture<PaginatedDTO<AddressDTO>> getAllAddress(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder);

    CompletableFuture<AddressDTO> updateEmployeeAddress(String employeeId, AddressDTO addressDTO);

    CompletableFuture<EmployeePrimaryDetailsDTO> getEmployeePrimaryDetails(String employeeId);

   CompletableFuture<EmployeePrimaryDetailsDTO> updateEmployeeDetails(String employeeId, EmployeePrimaryDetailsDTO employeePrimaryDetailsDTO);

    CompletableFuture<JobDetailsDTO> getJobDetails(String employeeId);

    CompletableFuture<JobDetailsDTO> updateJobDetails(String employeeId, JobDetailsDTO jobDetailsDTO);

    List<RoleEmployeesDTO> getRolesForProject(String projectId);
}