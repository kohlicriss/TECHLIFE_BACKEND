//package com.auth.jwtsecurity.controller;
//
//import com.auth.jwtsecurity.model.Employee;
//import com.auth.jwtsecurity.service.EmployeeService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/employees")
//@RequiredArgsConstructor
//public class EmployeeController {
//
//    private final EmployeeService employeeService;
//
//    @GetMapping
//    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'TEAM_LEAD', 'HR', 'MANAGER')")
//    public ResponseEntity<?> getAllEmployees() {
//        log.info("Fetching all employees");
//        return ResponseEntity.ok(employeeService.getAllEmployees());
//    }
//
//    @GetMapping("/{employeeId}")
//    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'TEAM_LEAD', 'HR', 'MANAGER')")
//    public ResponseEntity<?> getEmployeeById(@PathVariable String employeeId) {
//        log.info("Fetching employee with ID: {}", employeeId);
//        return employeeService.getEmployeeById(employeeId)
//                .<ResponseEntity<?>>map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("error", "Employee with id " + employeeId + " not found")));
//    }
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
//    public ResponseEntity<?> createEmployee(@Valid @RequestBody Employee employee) {
//        log.info("Creating employee: {}", employee);
//        return ResponseEntity.ok(employeeService.saveEmployee(employee));
//    }
//
//    @PutMapping("/{employeeId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<?> updateEmployee(@PathVariable String employeeId, @Valid @RequestBody Employee employee) {
//        if (!employeeService.existsById(employeeId)) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("error", "Employee with id " + employeeId + " not found"));
//        }
//        employee.setEmployeeId(employeeId);
//        log.info("Updating employee with ID: {}", employeeId);
//        return ResponseEntity.ok(employeeService.saveEmployee(employee));
//    }
//
//    @DeleteMapping("/{employeeId}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<?> deleteEmployee(@PathVariable String employeeId) {
//        log.warn("Deleting employee with ID: {}", employeeId);
//        if (!employeeService.existsById(employeeId)) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("error", "Employee with id " + employeeId + " not found"));
//        }
//        employeeService.deleteEmployeeById(employeeId);
//        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
//    }
//}
