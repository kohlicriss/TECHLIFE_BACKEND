package com.example.notifications.clients;





import com.example.notifications.dtos.EmployeeDepartmentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "employee-service", contextId = "departmentClient")
public interface DepartmentClient {

    @GetMapping("/api/employee/department/{departmentId}/employees")
    EmployeeDepartmentDTO getEmployeesInDepartment(@PathVariable("departmentId") String departmentId);
}
