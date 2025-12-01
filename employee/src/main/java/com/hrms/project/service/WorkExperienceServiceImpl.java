package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.WorkExperienceDetails;
import com.hrms.project.dto.WorkExperienceDTO;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.WorkExperienceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WorkExperienceServiceImpl {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationClient notificationClient;

    public WorkExperienceDetails createExperenceByEmployeId(String employeeId,
                                                            WorkExperienceDTO workExperienceDTO) {
        log.info("Creating work experience for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        boolean exists = employee.getWorkExperienceDetails().stream()
                .anyMatch(exp ->
                        exp.getCompanyName().equalsIgnoreCase(workExperienceDTO.getCompanyName())
                                && exp.getDescription().equalsIgnoreCase(workExperienceDTO.getDescription())
                );

        if (exists) {
            log.warn("Duplicate work experience detected for employee {}", employeeId);
            throw new APIException("This work experience with the same company and description already exists for the employee.");
        }

        int maxSeq = employee.getWorkExperienceDetails().stream()
                .map(WorkExperienceDetails::getId)
                .filter(id -> id.startsWith("WORK"))
                .map(id -> id.substring(4)) // remove "WORK"
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        String newId = "WORK" + String.format("%03d", maxSeq + 1);
        log.debug("Generated new work experience ID={} for employee {}", newId, employeeId);

        WorkExperienceDetails workExperienceDetails = modelMapper.map(workExperienceDTO, WorkExperienceDetails.class);
        workExperienceDetails.setEmployee(employee);
        workExperienceDetails.setId(newId);

        WorkExperienceDetails saved = workExperienceRepository.save(workExperienceDetails);
        log.info("Work experience saved successfully with ID={} for employee {}", newId, employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("EXPERIENCE")
                    .type("WORK_EXP_CREATE")
                    .kind("INFO")
                    .sender("HR")
                    .subject("Work Experience Added")
                    .message("Your work experience at '" + workExperienceDTO.getCompanyName() + "' has been added successfully.")
                    .link("/employee/" + employeeId + "/experience")
                    .build());
            log.info("Notification sent for employee {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send notification for employee {}: {}", employeeId, e.getMessage());
        }

        return saved;
    }

    public WorkExperienceDetails updateExperience(String employeeId,
                                                  WorkExperienceDTO updatedData,
                                                  String id) {
        log.info("Updating work experience ID={} for employeeId={}", id, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        WorkExperienceDetails existing = workExperienceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Work experience not found with ID {}", id);
                    return new APIException("Work experience not found");
                });

        if (!existing.getEmployee().getEmployeeId().equals(employeeId)) {
            log.warn("Attempted to update work experience ID={} for wrong employee {}", id, employeeId);
            throw new APIException("This work experience does not belong to employee with ID: " + employeeId);
        }

        modelMapper.map(updatedData, existing);
        existing.setId(id);
        existing.setEmployee(employee);

        WorkExperienceDetails saved = workExperienceRepository.save(existing);
        log.info("Work experience ID={} updated successfully for employee {}", id, employeeId);

        notificationClient.send(NotificationRequest.builder()
                .receiver(employeeId)
                .category("EXPERIENCE")
                .type("WORK_EXP_UPDATE")
                .kind("INFO")
                .sender("HR")
                .subject("Work Experience Updated")
                .message("Your work experience at '" + updatedData.getCompanyName() + "' has been updated successfully.")
                .link("/employee/" + employeeId + "/experience")
                .build());

        return saved;
    }

    public List<WorkExperienceDetails> getExperience(String employeeId) {
        log.info("Fetching all work experiences for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        return employee.getWorkExperienceDetails();
    }

    public WorkExperienceDetails deleteExperienceById(String employeeId, String id) {
        log.info("Deleting work experience ID={} for employeeId={}", id, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        WorkExperienceDetails workExperienceDetails = workExperienceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Work experience not found with ID {}", id);
                    return new APIException("Experience not found with ID: " + id);
                });

        if (!workExperienceDetails.getEmployee().getEmployeeId().equals(employeeId)) {
            log.warn("Attempted to delete work experience ID={} for wrong employee {}", id, employeeId);
            throw new APIException("This experience does not belong to the given employee.");
        }

        workExperienceRepository.delete(workExperienceDetails);
        log.info("Work experience ID={} deleted for employee {}", id, employeeId);

        notificationClient.send(NotificationRequest.builder()
                .receiver(employeeId)
                .category("EXPERIENCE")
                .type("WORK_EXP_DELETE")
                .kind("WARNING")
                .sender("HR")
                .subject("Work Experience Deleted")
                .message("Your work experience at '" + workExperienceDetails.getCompanyName() + "' has been deleted.")
                .link("/employee/" + employeeId + "/experience")
                .build());

        return workExperienceDetails;
    }

    public WorkExperienceDetails getExperienceById(String employeeId, String experienceId) {
        log.info("Fetching work experience ID={} for employeeId={}", experienceId, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        WorkExperienceDetails experience = workExperienceRepository.findById(experienceId)
                .orElseThrow(() -> {
                    log.error("Work experience not found with ID {}", experienceId);
                    return new APIException("Work experience not found with ID: " + experienceId);
                });

        if (!experience.getEmployee().getEmployeeId().equals(employeeId)) {
            log.warn("Work experience ID={} does not belong to employee {}", experienceId, employeeId);
            throw new APIException("This experience does not belong to employee with ID: " + employeeId);
        }

        return experience;
    }
}