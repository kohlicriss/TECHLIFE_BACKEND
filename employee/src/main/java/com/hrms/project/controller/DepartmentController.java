package com.hrms.project.controller;

import com.hrms.project.dto.*;
import com.hrms.project.security.CheckEmployeeAccess;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.DepartmentService;
import com.hrms.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;
    @PostMapping("/department")
    @CheckPermission("DEPARTMENTS_DEPARTMENT_CREATE_DEPARTMENT")
    public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentDTO departmentDTO) {

        DepartmentDTO departmentList = departmentService.saveDepartment(departmentDTO);
        return new ResponseEntity<>(departmentList, HttpStatus.CREATED);
    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/department/{departmentId}/employees")
    @CheckPermission("DEPARTMENTS_DEPARTMENT_GET_ALL_EMPLOYEES_DEPARTMENT")
    public ResponseEntity<PaginatedDTO<EmployeeDepartmentDTO>> getEmployeesByDepartmentId(@PathVariable Integer pageNumber,
                                                                                          @PathVariable Integer pageSize,
                                                                                          @PathVariable String sortBy,
                                                                                          @PathVariable String sortOrder,
                                                                                          @PathVariable String departmentId) {
        PaginatedDTO<EmployeeDepartmentDTO> employeeDepartmentDTO = departmentService.getEmployeesByDepartmentId(pageNumber,pageSize,sortBy,sortOrder,departmentId);
        return new ResponseEntity<>(employeeDepartmentDTO, HttpStatus.OK);
    }

    @PutMapping("/department/{departmentId}")
    @CheckPermission(
            value = "DEPARTMENTS_DEPARTMENT_EDIT_DEPARTMENT"
    )
    public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable String departmentId, @Valid @RequestBody DepartmentDTO departmentDTO) {
        return new ResponseEntity<>(departmentService.updateDepartment(departmentId, departmentDTO), HttpStatus.CREATED);
    }


    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/departments")
    @CheckPermission("DEPARTMENTS_DEPARTMENT_GET_ALL_DEPARTMENTS")
    public ResponseEntity<PaginatedDTO<DepartmentDTO>> getAllDepartments(@PathVariable Integer pageNumber,
                                                                         @PathVariable Integer pageSize,
                                                                         @PathVariable String sortBy,
                                                                         @PathVariable String sortOrder) {
        return new ResponseEntity<>(departmentService.getAllDepartmentDetails(pageNumber,pageSize,sortBy,sortOrder), HttpStatus.OK);
    }

    @GetMapping("/get/department/{departmentId}")
    public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable String departmentId) {
        return new ResponseEntity<>(departmentService.getByDepartmentId(departmentId), HttpStatus.OK);
    }

    @GetMapping("/{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/{employeeId}/department/employees")
    @CheckPermission(
            value = "DEPARTMENTS_DEPARTMENT_GET_EMPLOYEE_ID_BY_ALL_DEPARTMENTS")

    public ResponseEntity<PaginatedDTO<EmployeeDepartmentDTO>> getEmployeesByEmployeeId(@PathVariable Integer pageNumber,
                                                                                        @PathVariable Integer pageSize,
                                                                                        @PathVariable String sortBy,
                                                                                        @PathVariable String sortOrder,
                                                                                        @PathVariable String employeeId) {
        return new ResponseEntity<>(departmentService.getEmployeeByEmployeeId(pageNumber,pageSize,sortBy,sortOrder,employeeId),HttpStatus.OK);
    }

    @DeleteMapping("/{departmentId}/department")
    @CheckPermission(
            value = "DEPARTMENTS_DEPARTMENT_DELETE_DEPARTMENT"
    )
    public ResponseEntity<String> deleteDepartment(@PathVariable String departmentId) {
        return new ResponseEntity<>(departmentService.deleteDepartment(departmentId),HttpStatus.OK);
    }
    @GetMapping("/departments/progress")
    public List<RoleProgressDTO> getDepartmentProgressByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return departmentService.getDepartmentProgressByMonth(year, month);
    }

    @GetMapping("/{employeeId}/department")
    public ResponseEntity<DepartmentDTO>getEmployeeIdByDepartment(@PathVariable String employeeId){
        DepartmentDTO department=departmentService.getEmployeeIdByDepartment(employeeId);
        return new ResponseEntity<>(department,HttpStatus.OK);
    }
    @PutMapping("/{employeeId}/department/{departmentId}")
    public ResponseEntity<DepartmentDTO> updateEmployeeDepartment(
            @PathVariable String employeeId,
            @PathVariable String departmentId) {
        DepartmentDTO updatedEmployee = departmentService.updateEmployeeDepartment(employeeId, departmentId);
        return ResponseEntity.ok(updatedEmployee);
    }


}