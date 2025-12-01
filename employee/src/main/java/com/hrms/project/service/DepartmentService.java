package com.hrms.project.service;

import com.hrms.project.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface DepartmentService {


    DepartmentDTO saveDepartment(DepartmentDTO departmentDTO);

    PaginatedDTO<EmployeeDepartmentDTO> getEmployeesByDepartmentId(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String departmentId);

    DepartmentDTO updateDepartment(String departmentId, DepartmentDTO departmentDTO);

    PaginatedDTO<DepartmentDTO> getAllDepartmentDetails(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder);

    DepartmentDTO getByDepartmentId(String departmentId);

    PaginatedDTO<EmployeeDepartmentDTO> getEmployeeByEmployeeId(Integer pageNumber,Integer pageSize, String sortBy, String sortOrder,String employeeId);

    String deleteDepartment(String departmentId);

    List<RoleProgressDTO> getDepartmentProgressByMonth(int year, int month);

    DepartmentDTO getEmployeeIdByDepartment(String employeeId);

    DepartmentDTO updateEmployeeDepartment(String employeeId, String departmentId);
}