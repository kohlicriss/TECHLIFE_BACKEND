package com.example.OfferLetter.Service;


import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee getEmployeeById(String employeeId) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        return employee.orElse(null);
    }

    public List<Employee> getAllEmployees() {

        return employeeRepository.findAll();
    }

    public void deleteEmployee(String employeeId) {

        employeeRepository.deleteById(employeeId);
    }

    public boolean existsById(String employeeId) {
        return employeeRepository.existsById(employeeId);
    }
}
