package com.app.chat_service.service;

import java.lang.System.Logger;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.mime.MimeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.EmployeeDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeDetailsService {

    @Autowired
    private EmployeeDetailsRepository employeeRepository;

    // Add Employee
    @CachePut(value = "employees", key = "#employeeDto.employeeId",unless = "#result == null")
    public employee_details addEmployee(EmployeeDTO employeeDto) {
    	employee_details employee = new employee_details();
    	employee.setEmployeeId(employeeDto.getEmployeeId());
    	employee.setEmployeeName(employeeDto.getDisplayName());
    	employee.setProfileLink(employeeDto.getEmployeeImage());    	
    	log.info("employee saved in employe_details db : {}",employeeDto.getEmployeeId());
    	return employeeRepository.save(employee);
        }

    // Update Employee
    @CacheEvict(value = "employees", key = "#employeeId")
    public employee_details updateEmployee(String employeeId, EmployeeDTO updatedEmployeeDto) {
    	
        	employee_details emp = employeeRepository.findById(employeeId)
        			.orElseThrow(() -> new NotFoundException("Employee not found with Id:" +employeeId));
        
        	emp.setEmployeeName(updatedEmployeeDto.getDisplayName());
        	log.info("Setting the employeeName : {} ", updatedEmployeeDto.getDisplayName());
            emp.setProfileLink(updatedEmployeeDto.getEmployeeImage());
            log.info("Updating an employee {} ",emp);
            return employeeRepository.save(emp);
        }
        
    

    // Get Employee by ID
//    @Cacheable(value = "employees", key = "#employeeId")
//    public employee_details getEmployeeById(String employeeId) {
//        return employeeRepository.findById(employeeId)
//        		.orElseThrow(() -> new NotFoundException("Employee not found with Id {}: "+employeeId));
//    }

    
//    Delete Emp by Id
    @CacheEvict(value = "employeeDetails", key = "#employeeId")
	public void deleteById(String employeeId) {
    	
    	if(!employeeRepository.existsById(employeeId)) {
    		throw new NotFoundException("Employee not found with id: "+employeeId);
    	}
    	employeeRepository.deleteById(employeeId);
    }
}
    	
		
    
