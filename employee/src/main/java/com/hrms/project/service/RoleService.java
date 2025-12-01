package com.hrms.project.service;

import com.hrms.project.dto.EmployeeRoleDTO;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.EmployeeProject;
import com.hrms.project.entity.Project;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectEmployeeRepository;
import com.hrms.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectEmployeeRepository projectEmployeeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public void assignRoles(EmployeeRoleDTO employeeRole) {
        Project project = projectRepository.findById(employeeRole.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + employeeRole.getProjectId()));
        employeeRole.getEmployeeRoles().forEach((empId, role) -> {
            Employee employee = employeeRepository.findById(empId)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + empId));

            EmployeeProject ep = projectEmployeeRepository.findByEmployeeAndProject(employee, project)
                    .orElse(new EmployeeProject());

            ep.setEmployee(employee);
            ep.setProject(project);
            ep.setRole(role);

            projectEmployeeRepository.save(ep);
        });


    }
}
