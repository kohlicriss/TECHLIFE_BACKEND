package com.hrms.project.controller;


import com.hrms.project.dto.EmployeeRoleDTO;
import com.hrms.project.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee")
public class EmployeeRoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping("/assignRole")
    public ResponseEntity<String>assignRole(@RequestBody EmployeeRoleDTO employeeRole){
         roleService.assignRoles(employeeRole);
         return ResponseEntity.ok("Role assigned succesfully");
        
    }
}
