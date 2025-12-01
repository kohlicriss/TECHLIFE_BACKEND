package com.hrms.project.controller;

import com.hrms.project.dto.*;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("/api/employee")

public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    @CheckPermission("EMPLOYEES_ADD")
    public CompletableFuture<ResponseEntity<EmployeeDTO>> createEmployee(
            @Valid @RequestBody EmployeeDTO employeeDTO) throws IOException {
        return employeeService.createData(employeeDTO)
                .thenApply(savedEmployee -> new ResponseEntity<>(savedEmployee, HttpStatus.CREATED));
    }

    @GetMapping("/{id}")
    @CheckPermission(
            value = "EMPLOYEES_GET_BY_EMPLOYEE_ID",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "id",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<EmployeeDTO>> getEmployeeById(@PathVariable String id) {
        System.out.println("getEmployeeById: " + id);
        log.info("Controller handling getEmployeeById on thread {}", Thread.currentThread().getName());

        CompletableFuture<EmployeeDTO> employeeFuture = employeeService.getEmployeeById(id);
        return employeeFuture.thenApply(employeeDetails -> new ResponseEntity<>(employeeDetails, HttpStatus.OK));
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/employees")
    @CheckPermission("EMPLOYEES_GET_ALL_EMPLOYEES")
    public CompletableFuture<ResponseEntity<PaginatedDTO<EmployeeDTO>>>getAllEmployees(@PathVariable Integer pageNumber,
                                                             @PathVariable Integer pageSize,
                                                             @PathVariable String sortBy,
                                                             @PathVariable String sortOrder) {
        CompletableFuture<PaginatedDTO<EmployeeDTO> >employeeResponse = employeeService.getAllEmployees(pageNumber,pageSize,sortBy,sortOrder);
        return employeeResponse.thenApply(employeeDetails->new ResponseEntity<>(employeeDetails, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    @CheckPermission(
            value = "EMPLOYEES_UPDATE_EMPLOYEE_ID",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "id",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<EmployeeDTO>>updateEmployee(
                                                      @PathVariable String id,
                                                      @Valid @RequestBody EmployeeDTO employeeDTO){
       CompletableFuture<EmployeeDTO> updatedEmployeeDetails = employeeService.updateEmployee(id, employeeDTO);
        return updatedEmployeeDetails.thenApply(employeeDetails-> new ResponseEntity<>(employeeDetails, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @CheckPermission("EMPLOYEES_EMPLOYEE_TERMINATE_EMPLOYEE")
    public CompletableFuture<ResponseEntity<EmployeeDTO>>deleteEmployee(@PathVariable String id) {
       CompletableFuture<EmployeeDTO> deletedEmployee = employeeService.deleteEmployee(id);
        return deletedEmployee.thenApply(employeeDetails->new ResponseEntity<>(employeeDetails, HttpStatus.OK));
    }

    @GetMapping("/{employeeId}/address")
    @CheckPermission(
            value = "PROFILE_PROFILE_ADDRESS_INFORMATION_GET_INFORMATION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<AddressDTO>> getAddress(@PathVariable String employeeId) {
       CompletableFuture<AddressDTO> addressDTO = employeeService.getAddress(employeeId);
        return addressDTO.thenApply(addressDetails->new ResponseEntity<>(addressDetails, HttpStatus.OK));
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/employee/address")
    public CompletableFuture<ResponseEntity<PaginatedDTO<AddressDTO>>> getAllAddress(@PathVariable Integer pageNumber,
                                                          @PathVariable Integer pageSize,
                                                          @PathVariable String sortBy,
                                                          @PathVariable String sortOrder) {
        CompletableFuture<PaginatedDTO<AddressDTO>> addressDTOList = employeeService.getAllAddress(pageNumber,pageSize,sortBy,sortOrder);
        return addressDTOList.thenApply(addressDetails->new ResponseEntity<>(addressDetails, HttpStatus.OK));
    }

    @PutMapping("/{employeeId}/address")
    @CheckPermission(
            value = "PROFILE_PROFILE_ADDRESS_INFORMATION_EDIT_INFORMATION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<AddressDTO>> updateAddress(@PathVariable String employeeId,
                                                    @Valid @RequestBody AddressDTO addressDTO) {
        CompletableFuture<AddressDTO> updatedAddressDTO = employeeService.updateEmployeeAddress(employeeId, addressDTO);
        return updatedAddressDTO.thenApply(addressDetails->new ResponseEntity<>(addressDetails, HttpStatus.CREATED));
    }

    @GetMapping("/{employeeId}/primary/details")
    @CheckPermission("PROFILE_PROFILE_PRIMARY_DETAILS_GET_DETAILS")
    public CompletableFuture<ResponseEntity<EmployeePrimaryDetailsDTO>> getEmployeePrimaryDetails(@PathVariable String employeeId) {
        CompletableFuture<EmployeePrimaryDetailsDTO> primaryDetails = employeeService.getEmployeePrimaryDetails(employeeId);
        return primaryDetails.thenApply(primaryDetail->new ResponseEntity<>(primaryDetail, HttpStatus.OK));
    }

    @PutMapping("/{employeeId}/primary/details")
    @CheckPermission(
            value = "PROFILE_PROFILE_PRIMARY_DETAILS_EDIT_DETAILS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE","ROLE_TEAM_LEAD","ROLE_MANAGER","ROLE_HR"}
    )
    public CompletableFuture<ResponseEntity<EmployeePrimaryDetailsDTO>> updateEmployeePrimaryDetails(@PathVariable String employeeId,
                                                                                  @Valid @RequestBody EmployeePrimaryDetailsDTO employeePrimaryDetailsDTO) {
        CompletableFuture<EmployeePrimaryDetailsDTO> updatedPrimaryDetails = employeeService.updateEmployeeDetails(employeeId, employeePrimaryDetailsDTO);
        return updatedPrimaryDetails.thenApply(updatedPrimary-> new ResponseEntity<>(updatedPrimary, HttpStatus.CREATED));
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    @GetMapping("{employeeId}/job/details")
    @CheckPermission(
            value = "PROFILE_JOB_GET_JOB_DETAILS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<JobDetailsDTO>> getJobDetails(@PathVariable String employeeId) {
        CompletableFuture<JobDetailsDTO> jobDetailsDTO = employeeService.getJobDetails(employeeId);
        return jobDetailsDTO.thenApply(jobDetails->new ResponseEntity<>(jobDetails, HttpStatus.OK));
    }

    @PutMapping("/{employeeId}/job/details")
    @CheckPermission(
            value = "PROFILE_JOB_JOB_DETAILS_EDIT_DETAILS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<JobDetailsDTO>> updateJobDetails(@PathVariable String employeeId,
                                                          @Valid  @RequestBody JobDetailsDTO jobDetailsDTO) {
        CompletableFuture<JobDetailsDTO> updatedJobDetails = employeeService.updateJobDetails(employeeId, jobDetailsDTO);
        return updatedJobDetails.thenApply(updatedJob->new ResponseEntity<>(updatedJob, HttpStatus.OK));
    }

    @GetMapping("/{employeeId}/contact")
    @CheckPermission(
            value = "PROFILE_PROFILE_CONTACT_DETAILS_GET_DETAILS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<ContactDetailsDTO>> getContactDetails(@PathVariable String employeeId) {
        CompletableFuture<ContactDetailsDTO> contactDetailsDTO = employeeService.getEmployeeContactDetails(employeeId);
        return contactDetailsDTO.thenApply(contactDetails->new ResponseEntity<>(contactDetails, HttpStatus.OK));
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/employee/contact")

    public CompletableFuture<ResponseEntity<List<ContactDetailsDTO>>>getAllContactDetails(@PathVariable Integer pageNumber,
                                                                        @PathVariable Integer pageSize,
                                                                        @PathVariable String sortBy,
                                                                        @PathVariable String sortOrder) {
        CompletableFuture<List<ContactDetailsDTO>> contactDetailsDTO = employeeService.getAllEmployeeContactDetails(pageNumber,pageSize,sortBy,sortOrder);
        return contactDetailsDTO.thenApply(contactDetails->new ResponseEntity<>(contactDetails, HttpStatus.OK));
    }

    @PutMapping("/{employeeId}/contact")
    @CheckPermission(
            value = "PROFILE_PROFILE_CONTACT_DETAILS_EDIT_DETAILS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE","ROLE_HR"}
    )
    public CompletableFuture<ResponseEntity<ContactDetailsDTO>> updateContactDetails(@PathVariable String employeeId,
                                                                  @Valid @RequestBody ContactDetailsDTO contactDetailsDTO) {
        CompletableFuture<ContactDetailsDTO> updatedContactDetails = employeeService.updateContactDetails(employeeId, contactDetailsDTO);
        return updatedContactDetails.thenApply(updatedContact->new ResponseEntity<>(updatedContact, HttpStatus.CREATED));
    }
    @GetMapping("/{projectId}/roles")
    public ResponseEntity<List<RoleEmployeesDTO>> getRolesForProject(@PathVariable String projectId) {
        List<RoleEmployeesDTO> response = employeeService.getRolesForProject(projectId);
        return ResponseEntity.ok(response);
    }
}
