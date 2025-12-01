package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.AadhaarCardDetails;
import com.hrms.project.entity.Employee;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.AadhaarAlreadyAssignedException;
import com.hrms.project.handlers.AadhaarNotFoundException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.AadhaarDTO;
import com.hrms.project.repository.AadhaarDetailsRepository;
import com.hrms.project.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AadhaarServiceImpl {

    @Autowired
    private AadhaarDetailsRepository aadhaarDetailsRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private EmployeeAsyncService employeeAsyncService;

    @Async("employeeTaskExecutor")
    public CompletableFuture<AadhaarCardDetails> createAadhaar(String employeeId, MultipartFile aadhaarImage, AadhaarDTO aadhaarCardDetails) {
        try {
            log.info("Creating Aadhaar for employee {}", employeeId);
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

            if (aadhaarDetailsRepository.findByEmployee_EmployeeId(employeeId) != null) {
                log.warn("Employee {} already has Aadhaar assigned", employeeId);
                throw new AadhaarAlreadyAssignedException("This employee already has Aadhaar assigned");
            }

            if (aadhaarCardDetails.getAadhaarNumber() == null || aadhaarCardDetails.getAadhaarNumber().trim().isEmpty()) {
                throw new APIException("Aadhaar number cannot be null or empty");
            }

            AadhaarCardDetails savedDetails;

            if (aadhaarDetailsRepository.findById(aadhaarCardDetails.getAadhaarNumber()).isEmpty()) {
                if (aadhaarImage == null || aadhaarImage.isEmpty()) {
                    throw new APIException("Aadhaar image is required and cannot be null or empty");
                }

                AadhaarCardDetails cardDetails = new AadhaarCardDetails();
                modelMapper.map(aadhaarCardDetails, cardDetails);

                String s3Key = s3Service.uploadFile(employeeId, "aadhaar", aadhaarImage);
                cardDetails.setUploadAadhaar(s3Key);
                cardDetails.setEmployee(employee);

                savedDetails = aadhaarDetailsRepository.save(cardDetails);
                log.info("Aadhaar created successfully for employee {}", employeeId);

            } else {
                AadhaarCardDetails details = aadhaarDetailsRepository.findById(aadhaarCardDetails.getAadhaarNumber()).get();

                if (details.getEmployee() == null) {
                    details.setEmployee(employee);
                    modelMapper.map(aadhaarCardDetails, details);
                    savedDetails = aadhaarDetailsRepository.save(details);
                    log.info("Aadhaar {} assigned to employee {}", aadhaarCardDetails.getAadhaarNumber(), employeeId);

                } else {
                    log.warn("Aadhaar {} already assigned to another employee", aadhaarCardDetails.getAadhaarNumber());
                    throw new AadhaarAlreadyAssignedException("Current Aadhaar card is already assigned to another employee");
                }
            }

            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employeeId)
                    .message("Your Aadhaar details have been uploaded successfully.")
                    .sender("SYSTEM")
                    .type("AADHAAR")
                    .link("/profile/" + employeeId + "/documents")
                    .category("DOCUMENT")
                    .kind("CREATED")
                    .subject("Aadhaar Uploaded")
                    .build();

            employeeAsyncService.sendNotifications(List.of(notification));

            return CompletableFuture.completedFuture(savedDetails);

        } catch (Exception e) {
            log.error("Error creating Aadhaar for employee {}", employeeId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<AadhaarDTO> getAadhaarByEmployeeId(String employeeId) {
        try {
            log.info("Fetching Aadhaar for employee {}", employeeId);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee Not Found with id: " + employeeId));

            AadhaarCardDetails aadhaarCardDetails = employee.getAadhaarCardDetails();
            if (aadhaarCardDetails == null) {
                throw new AadhaarNotFoundException("Aadhaar card Details not found for the employee with id: " + employeeId);
            }

            AadhaarDTO aadhaarDTO = modelMapper.map(aadhaarCardDetails, AadhaarDTO.class);

            if (aadhaarCardDetails.getUploadAadhaar() != null) {
                String presignedUrl = s3Service.generatePresignedUrl(aadhaarCardDetails.getUploadAadhaar());
                aadhaarDTO.setUploadAadhaar(presignedUrl);
            }

            return CompletableFuture.completedFuture(aadhaarDTO);

        } catch (Exception e) {
            log.error("Error fetching Aadhaar for employee {}", employeeId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<AadhaarCardDetails> updateAadhaar(String employeeId, MultipartFile aadhaarImage, AadhaarDTO aadhaarCardDetails) {
        try {
            log.info("Updating Aadhaar details for employee {}", employeeId);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee Not Found with the id: " + employeeId));

            AadhaarCardDetails existing = employee.getAadhaarCardDetails();
            if (existing == null) {
                throw new AadhaarNotFoundException("Aadhaar card Details not found for employee with id: " + employeeId);
            }

            if (!existing.getAadhaarNumber().equals(aadhaarCardDetails.getAadhaarNumber())) {
                throw new APIException("Aadhaar number cannot be changed once submitted");
            }

            if (aadhaarImage != null && !aadhaarImage.isEmpty()) {
                String oldKey = existing.getUploadAadhaar();
                String newKey = s3Service.uploadFile(employeeId, "aadhaar", aadhaarImage);
                if (oldKey != null && !oldKey.equals(newKey)) {
                    s3Service.deleteFile(oldKey);
                }
                existing.setUploadAadhaar(newKey);
            }

            existing.setDateOfBirth(aadhaarCardDetails.getDateOfBirth());
            existing.setEnrollmentNumber(aadhaarCardDetails.getEnrollmentNumber());
            existing.setAadhaarName(aadhaarCardDetails.getAadhaarName());
            existing.setGender(aadhaarCardDetails.getGender());
            existing.setAddress(aadhaarCardDetails.getAddress());

            employeeAsyncService.sendNotification(
                    NotificationRequest.builder()
                            .receiver(employeeId)
                            .message("Your Aadhaar details have been updated successfully.")
                            .sender("SYSTEM")
                            .type("AADHAAR")
                            .link("/profile/" + employeeId + "/documents")
                            .category("DOCUMENT")
                            .kind("UPDATED")
                            .subject("Aadhaar Updated")
                            .build()
            );

            return CompletableFuture.completedFuture(aadhaarDetailsRepository.save(existing));

        } catch (Exception e) {
            log.error("Error updating Aadhaar for employee {}", employeeId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<AadhaarCardDetails> deleteAadharByEmployeeId(String employeeId) {
        try {
            log.info("Starting Aadhaar deletion for employee {}", employeeId);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee Not Found with the id: " + employeeId));

            AadhaarCardDetails aadhaarDetails = employee.getAadhaarCardDetails();
            if (aadhaarDetails == null) {
                throw new AadhaarNotFoundException("No Aadhaar details found for employee with id: " + employeeId);
            }

            if (aadhaarDetails.getUploadAadhaar() != null) {
                s3Service.deleteFile(aadhaarDetails.getUploadAadhaar());
            }

            employee.setAadhaarCardDetails(null);
            employeeRepository.save(employee);
            aadhaarDetailsRepository.deleteById(aadhaarDetails.getAadhaarNumber());

            employeeAsyncService.sendNotification(
                    NotificationRequest.builder()
                            .receiver(employeeId)
                            .message("Your Aadhaar details have been deleted.")
                            .sender("SYSTEM")
                            .type("AADHAAR")
                            .link("/profile/" + employeeId + "documents")
                            .category("DOCUMENT")
                            .kind("DELETED")
                            .subject("Aadhaar Deleted")
                            .build()
            );

            return CompletableFuture.completedFuture(aadhaarDetails);

        } catch (Exception e) {
            log.error("Error deleting Aadhaar for employee {}", employeeId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
