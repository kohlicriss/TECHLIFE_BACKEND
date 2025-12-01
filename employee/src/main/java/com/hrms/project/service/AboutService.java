package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.AboutDTO;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.About;
import com.hrms.project.entity.Employee;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.handlers.ResourceAlreadyExistsException;
import com.hrms.project.handlers.ResourceNotFoundException;
import com.hrms.project.repository.AboutRepository;
import com.hrms.project.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AboutService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AboutRepository aboutRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationClient notificationClient;

    public AboutDTO createAbout(String employeeId, AboutDTO aboutDTO) {
        log.info("Creating About details for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for ID={}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID " + employeeId);
                });

        if (aboutRepository.findByEmployee(employee).isPresent()) {
            log.warn("Attempt to create duplicate About entry for employeeId={}", employeeId);
            throw new ResourceAlreadyExistsException("About already exists for this employee. Use update instead.");
        }

        About about = modelMapper.map(aboutDTO, About.class);
        about.setEmployee(employee);

        About saved = aboutRepository.save(about);
        log.info("Successfully created About section for employeeId={}", employeeId);

        NotificationRequest notification = NotificationRequest.builder()
                .receiver(saved.getEmployee().getEmployeeId())
                .message("Your description has been added.")
                .sender("ANASOL CONSULTANCY SERVICES")
                .type("EMPLOYEE_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Employee Description added")
                .link("/profile/" + saved.getEmployee().getEmployeeId() + "/about")
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send added notification to employee {}: {}", saved.getEmployee().getEmployeeId(), e.getMessage());
        }
        return modelMapper.map(saved, AboutDTO.class);
    }

    public AboutDTO updateAbout(String employeeId, AboutDTO aboutDTO) {
        log.info("Updating About details for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for ID={}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID " + employeeId);
                });

        About about = aboutRepository.findByEmployee(employee)
                .orElseThrow(() -> {
                    log.error("About details not found for employeeId={}", employeeId);
                    return new ResourceNotFoundException("About not found for this employee");
                });

        about.setJobLove(aboutDTO.getJobLove());
        about.setHobbies(aboutDTO.getHobbies());
        about.setAbout(aboutDTO.getAbout());

        About updated = aboutRepository.save(about);
        log.info("Successfully updated About section for employeeId={}", employeeId);
        NotificationRequest notification = NotificationRequest.builder()
                .receiver(updated.getEmployee().getEmployeeId())
                .message("Your description has been added.")
                .sender("ANASOL CONSULTANCY SERVICES")
                .type("EMPLOYEE_UPDATE")
                .category("SYSTEM")
                .kind("INFO")
                .subject("Employee Description updated")
                .link("/profile/" + updated.getEmployee().getEmployeeId() + "/about")
                .build();

        try {
            notificationClient.send(notification);
        } catch (Exception e) {
            log.error("Failed to send update notification to employee {}: {}", updated.getEmployee().getEmployeeId(), e.getMessage());
        }
        return modelMapper.map(updated, AboutDTO.class);
    }

    public AboutDTO getAboutByEmployee(String employeeId) {
        log.info("Fetching About details for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for ID={}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID " + employeeId);
                });

        About about = aboutRepository.findByEmployee(employee)
                .orElseThrow(() -> {
                    log.error("About details not found for employeeId={}", employeeId);
                    return new ResourceNotFoundException("About not found for employee " + employeeId);
                });

        log.info("Successfully fetched About details for employeeId={}", employeeId);
        return modelMapper.map(about, AboutDTO.class);
    }


    @Transactional
    public void deleteAbout(String employeeId) {
        log.info("Deleting About details for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for ID={}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID " + employeeId);
                });

        if (aboutRepository.findByEmployee(employee).isEmpty()) {
            log.warn("No About record found to delete for employeeId={}", employeeId);
            throw new ResourceNotFoundException("No About record found for this employee");
        }

        aboutRepository.deleteByEmployee(employee);
        log.info("Successfully deleted About details for employeeId={}", employeeId);
    }
}
