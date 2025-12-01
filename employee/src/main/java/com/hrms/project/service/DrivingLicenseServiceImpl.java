package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.DrivingLicense;
import com.hrms.project.entity.Employee;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.DrivingLicenseDTO;
import com.hrms.project.repository.DrivingLicenseRepository;
import com.hrms.project.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DrivingLicenseServiceImpl {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DrivingLicenseRepository drivingLicenseRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private S3Service  s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private EmployeeAsyncService employeeAsyncService;

    @Async("employeeTaskExecutor")
    public CompletableFuture<DrivingLicense> createDrivingLicense(String employeeId, MultipartFile licenseImage, DrivingLicenseDTO drivingLicenseDTO) throws IOException {
        log.debug("Attempting to create Driving License for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });
        if (drivingLicenseRepository.findByEmployee_EmployeeId(employeeId).isPresent()) {
            log.warn("Employee {} already has a Driving License assigned", employeeId);
            throw new APIException("This employee already has a Driving License assigned");
        }

        if (licenseImage == null || licenseImage.isEmpty()) {
            log.warn("Employee {} tried to create Driving License without image", employeeId);
            throw new APIException("Driving License image is required and cannot be null or empty");
        }

        Optional<DrivingLicense> existingLicenseOpt = drivingLicenseRepository.findById(drivingLicenseDTO.getLicenseNumber());

        DrivingLicense drivingLicense;
        if (existingLicenseOpt.isPresent()) {
            drivingLicense = existingLicenseOpt.get();
            if (drivingLicense.getEmployee() == null) {
                drivingLicense.setEmployee(employee);
                log.info("Re-assigning unlinked Driving License {} to employee {}", drivingLicense.getLicenseNumber(), employeeId);

            }
            else {
                throw new APIException("This Driving License is already assigned to another employee");
            }
        } else {
            drivingLicense = new DrivingLicense();
            modelMapper.map(drivingLicenseDTO, drivingLicense);
            drivingLicense.setEmployee(employee);
            log.debug("Mapped DrivingLicenseDTO to entity for employee {}", employeeId);

        }

        String s3Key = s3Service.uploadFile(employeeId, "drivingLicense", licenseImage);
        drivingLicense.setLicenseImage(s3Key);
        DrivingLicense saved=drivingLicenseRepository.save(drivingLicense);
        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DRIVING_LICENSE")
                    .message("Driving License '" + saved.getLicenseNumber() + "' has been added to your profile.")
                    .sender("HR")
                    .type("DRIVING_LICENSE_ADD")
                    .kind("INFO")
                    .subject("Driving License Added")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.debug("Notification sent for Driving License creation, employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send driving license create notification: {}", e.getMessage());
        }

        return CompletableFuture.completedFuture(saved);
    }


    @Async("employeeTaskExecutor")
    public CompletableFuture<DrivingLicenseDTO> getDrivingDetails(String employeeId) {
        log.debug("Fetching Driving License details for employee {}", employeeId);

        Employee employee=employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });        DrivingLicense details=employee.getDrivingLicense();

        if(details==null)
        {
            log.warn("Employee {} does not have a Driving License assigned", employeeId);
            throw new APIException("This employee does not have a Driving License assigned");
        }
        if(details.getLicenseImage()!=null){
            String presignedUrl=s3Service.generatePresignedUrl(details.getLicenseImage());
            details.setLicenseImage(presignedUrl);
            log.debug("Generated presigned URL for employee {} Driving License image", employeeId);


        }
        log.info("Driving License details retrieved successfully for employee {}", employeeId);
        return CompletableFuture.completedFuture(modelMapper.map(details, DrivingLicenseDTO.class));

    }


    @Async("employeeTaskExecutor")
    public CompletableFuture<DrivingLicense> updatedrivingDetails(String employeeId, MultipartFile licenseImage, DrivingLicenseDTO drivingLicenseDTO) throws IOException {
        log.debug("Updating Driving License for employee {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + employeeId));

        DrivingLicense existing = employee.getDrivingLicense();
        if (existing == null) {
            log.warn("No Driving License found for employee {}", employeeId);

            throw new APIException("Driving License details not found for employee: " + employeeId);
        }

        if (!existing.getLicenseNumber().equals(drivingLicenseDTO.getLicenseNumber())) {
            log.error("Attempt to change Driving License number for employee {}", employeeId);

            throw new APIException("License number cannot be changed once submitted");
        }

        if (licenseImage != null && !licenseImage.isEmpty()) {
            String oldKey = existing.getLicenseImage();
            String newKey = s3Service.uploadFile(employeeId, "drivingLicense", licenseImage);
            if (!oldKey.equals(newKey)) {
                s3Service.deleteFile(oldKey);
                log.debug("Old Driving License image deleted for employee {}", employeeId);

            }
            existing.setLicenseImage(newKey);
        }
        existing.setName(drivingLicenseDTO.getName());
        existing.setDateOfBirth(drivingLicenseDTO.getDateOfBirth());
        existing.setBloodGroup(drivingLicenseDTO.getBloodGroup());
        existing.setFatherName(drivingLicenseDTO.getFatherName());
        existing.setIssueDate(drivingLicenseDTO.getIssueDate());
        existing.setExpiresOn(drivingLicenseDTO.getExpiresOn());
        existing.setAddress(drivingLicenseDTO.getAddress());

        DrivingLicense updated = drivingLicenseRepository.save(existing);
        log.info("Driving License {} updated successfully for employee {}", updated.getLicenseNumber(), employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DRIVING_LICENSE")
                    .message("Driving License '" + updated.getLicenseNumber() + "' has been updated.")
                    .sender("HR")
                    .type("DRIVING_LICENSE_UPDATE")
                    .kind("INFO")
                    .subject("Driving License Updated")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.debug("Notification sent for Driving License update, employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send driving license update notification: {}", e.getMessage());
        }

        return CompletableFuture.completedFuture(updated);

    }


    @Async("employeeTaskExecutor")
    public CompletableFuture<DrivingLicense> deleteByEmployeeId(String employeeId) {
        log.debug("Deleting Driving License for employee {}", employeeId);

        Employee employee =employeeRepository.findById(employeeId)
               .orElseThrow(() -> {
            log.error("Employee not found with ID: {}", employeeId);
            return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
        });
        DrivingLicense drivingLicense=employee.getDrivingLicense();

        if(drivingLicense==null){
            log.warn("No Driving License found for employee {}", employeeId);

            throw new APIException("No Driving License found for employee with ID: " + employeeId);
        }
        if(drivingLicense.getLicenseImage()!=null){
            s3Service.deleteFile(drivingLicense.getLicenseImage());
            log.debug("Driving License image deleted from S3 for employee {}", employeeId);

        }

        employee.setDrivingLicense(null);
        employeeRepository.save(employee);

        drivingLicenseRepository.deleteById(drivingLicense.getLicenseNumber());
        log.info("Driving License {} deleted successfully for employee {}", drivingLicense.getLicenseNumber(), employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DRIVING_LICENSE")
                    .message("Driving License '" + drivingLicense.getLicenseNumber() + "' has been deleted.")
                    .sender("HR")
                    .type("DRIVING_LICENSE_DELETE")
                    .kind("ALERT")
                    .subject("Driving License Deleted")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.debug("Notification sent for Driving License deletion, employeeId: {}", employeeId);

        } catch (Exception e) {
            log.error("Failed to send driving license delete notification: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(drivingLicense);

    }
}