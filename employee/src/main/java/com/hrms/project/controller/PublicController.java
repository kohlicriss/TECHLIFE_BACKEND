package com.hrms.project.controller;

import com.hrms.project.dto.PaginatedDTO;
import com.hrms.project.dto.PublicEmployeeDetails;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.PublicEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee")

public class PublicController {

    @Autowired
    private PublicEmployeeService publicEmployeeService;

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/public/employees")
    public ResponseEntity<PaginatedDTO<PublicEmployeeDetails>> getAllEmployees(@PathVariable Integer pageNumber,
                                                                               @PathVariable Integer pageSize,
                                                                               @PathVariable String sortBy,
                                                                               @PathVariable String sortOrder) {
        return new ResponseEntity<>(publicEmployeeService.getAllEmployees(pageNumber,pageSize,sortBy,sortOrder), HttpStatus.OK);
    }

        @GetMapping("public/{employeeId}/details")
        @CheckPermission(
                value = "GET_PUBLIC"

        )
  //      @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
        public ResponseEntity<PublicEmployeeDetails> getEmployeeDetails(@PathVariable String employeeId) {
            return new ResponseEntity<>(publicEmployeeService.getEmployeeDetails(employeeId),HttpStatus.OK);
        }

        @GetMapping("/{employeeId}/header")
     //   @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
        @CheckPermission(
                value = "PROFILE_GET_HEADER",
                MatchParmName = "employeeId",
                MatchParmFromUrl = "employeeId",
                MatchParmForRoles = {"ROLE_EMPLOYEE"}
        )
        public  ResponseEntity<PublicEmployeeDetails> getHeaderDetails(@PathVariable String employeeId) {
            return new ResponseEntity<>(publicEmployeeService.getHeaderDetails(employeeId),HttpStatus.OK);
        }

    //@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
    @PutMapping("/{employeeId}/header")
    @CheckPermission(
            value = "PROFILE_EDIT_HEADER",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<PublicEmployeeDetails> updateHeader(
            @PathVariable String employeeId,
            @RequestBody  PublicEmployeeDetails updateRequest)
          {

        PublicEmployeeDetails updatedHeader = publicEmployeeService.updateHeader(employeeId, updateRequest);
        return ResponseEntity.ok(updatedHeader);
    }



}
