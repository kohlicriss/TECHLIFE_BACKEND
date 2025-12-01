package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.PassportDetails;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.PassportDetailsDTO;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.PassportDetailsRepository;
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
public class PassportDetailsServiceImpl {

    @Autowired
    private PassportDetailsRepository passportDetailsRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private EmployeeAsyncService employeeAsyncService;

    @Async("employeeTaskExecutor")
    public CompletableFuture<PassportDetails> createPassport(String employeeId, MultipartFile passportImage, PassportDetailsDTO passportDetailsDTO) throws IOException {
        log.info("Creating Passport for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        if (passportDetailsRepository.findByEmployee_EmployeeId(employeeId).isPresent()) {
            log.warn("Passport already exists for employeeId: {}", employeeId);
            throw new APIException("This employee already has a Passport assigned");
        }

        if (passportImage == null || passportImage.isEmpty()) {
            log.error("Passport image is null/empty for employeeId: {}", employeeId);
            throw new APIException("Passport image is required and cannot be null or empty");
        }

        Optional<PassportDetails> existingPassportOpt = passportDetailsRepository.findById(passportDetailsDTO.getPassportNumber());
        PassportDetails passportDetails;

        if (existingPassportOpt.isPresent()) {
            passportDetails = existingPassportOpt.get();
            if (passportDetails.getEmployee() == null) {
                passportDetails.setEmployee(employee);
                log.info("Re-assigning passport {} to employeeId: {}", passportDetailsDTO.getPassportNumber(), employeeId);
            } else {
                log.error("Passport {} is already assigned to another employee", passportDetailsDTO.getPassportNumber());
                throw new APIException("This Passport is already assigned to another employee");
            }
        } else {
            passportDetails = new PassportDetails();
            modelMapper.map(passportDetailsDTO, passportDetails);
            passportDetails.setEmployee(employee);
            log.debug("Mapped PassportDetailsDTO to PassportDetails for employeeId: {}", employeeId);
        }

        String s3Key = s3Service.uploadFile(employeeId, "passport", passportImage);
        passportDetails.setPassportImage(s3Key);
        PassportDetails saved = passportDetailsRepository.save(passportDetails);
        log.info("Passport {} saved for employeeId: {}", saved.getPassportNumber(), employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DOCUMENTS")
                    .type("PASSPORT_CREATE")
                    .kind("INFO")
                    .sender("HR")
                    .subject("PASSPORT Added")
                    .message("Your PASSPORT details have been successfully added.")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PASSPORT_CREATE to employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send passport create notification: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(saved);
    }

    @Async("employeeTaskExecutor")

    public CompletableFuture<PassportDetailsDTO> getPassportDetails(String employeeId) {
        log.info("Fetching PassportDetails for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        PassportDetails details = employee.getPassportDetails();
        if (details == null) {
            log.warn("No PassportDetails found for employeeId: {}", employeeId);
            throw new APIException("This employee does not have a Passport assigned");
        }

        if (details.getPassportImage() != null) {
            String presignedUrl = s3Service.generatePresignedUrl(details.getPassportImage());
            details.setPassportImage(presignedUrl);
            log.debug("Generated presigned URL for Passport image for employeeId: {}", employeeId);
        }

        log.info("Returning PassportDetails for employeeId: {}", employeeId);
        return CompletableFuture.completedFuture(modelMapper.map(details, PassportDetailsDTO.class));
    }
    @Async("employeeTaskExecutor")
    public CompletableFuture<PassportDetails> updatePasswordDetails(String employeeId, MultipartFile passportImage, PassportDetailsDTO passportDetailsDTO) throws IOException {
        log.info("Updating PassportDetails for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        PassportDetails existing = employee.getPassportDetails();
        if (existing == null) {
            log.error("Passport Details not found for employeeId: {}", employeeId);
            throw new APIException("Passport Details not found for employee with ID: " + employeeId);
        }

        if (!existing.getPassportNumber().equals(passportDetailsDTO.getPassportNumber())) {
            log.error("Passport number mismatch for employeeId: {}. Existing: {}, New: {}",
                    employeeId, existing.getPassportNumber(), passportDetailsDTO.getPassportNumber());
            throw new APIException("Passport number cannot be changed once submitted");
        }

        if (passportImage != null && !passportImage.isEmpty()) {
            log.info("Updating passport image for employeeId: {}", employeeId);
            String oldKey = existing.getPassportImage();
            String newKey = s3Service.uploadFile(employeeId, "passport", passportImage);
            if (oldKey != null && !oldKey.equals(newKey)) {
                s3Service.deleteFile(oldKey);
                log.debug("Deleted old passport image for employeeId: {}", employeeId);
            }
            existing.setPassportImage(newKey);
        }

        // Update fields
        existing.setCountryCode(passportDetailsDTO.getCountryCode());
        existing.setPassportType(passportDetailsDTO.getPassportType());
        existing.setDateOfBirth(passportDetailsDTO.getDateOfBirth());
        existing.setName(passportDetailsDTO.getName());
        existing.setGender(passportDetailsDTO.getGender());
        existing.setDateOfIssue(passportDetailsDTO.getDateOfIssue());
        existing.setPlaceOfIssue(passportDetailsDTO.getPlaceOfIssue());
        existing.setPlaceOfBirth(passportDetailsDTO.getPlaceOfBirth());
        existing.setDateOfExpiration(passportDetailsDTO.getDateOfExpiration());
        existing.setAddress(passportDetailsDTO.getAddress());

        PassportDetails updated = passportDetailsRepository.save(existing);
        log.info("Passport {} updated for employeeId: {}", updated.getPassportNumber(), employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("PASSPORT")
                    .message("Passport '" + updated.getPassportNumber() + "' has been updated.")
                    .sender("HR")
                    .type("PASSPORT_UPDATE")
                    .kind("INFO")
                    .subject("Passport Updated")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PASSPORT_UPDATE to employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send passport update notification: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(updated);
    }
    @Async("employeeTaskExecutor")

    public CompletableFuture<PassportDetails> deleteByEmployeeId(String employeeId) {
        log.info("Deleting PassportDetails for employeeId: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        PassportDetails passportDetails = employee.getPassportDetails();
        if (passportDetails == null) {
            log.warn("No passport found for employeeId: {}", employeeId);
            throw new APIException("No passport found for employee with ID: " + employeeId);
        }

        if (passportDetails.getPassportImage() != null) {
            s3Service.deleteFile(passportDetails.getPassportImage());
            log.debug("Deleted passport image from S3 for employeeId: {}", employeeId);
        }

        employee.setPassportDetails(null);
        employeeRepository.save(employee);
        passportDetailsRepository.deleteById(passportDetails.getPassportNumber());
        log.info("Passport {} deleted for employeeId: {}", passportDetails.getPassportNumber(), employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("PASSPORT")
                    .message("Passport '" + passportDetails.getPassportNumber() + "' has been deleted from your profile.")
                    .sender("HR")
                    .type("PASSPORT_DELETE")
                    .kind("ALERT")
                    .subject("Passport Deleted")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PASSPORT_DELETE to employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send passport delete notification: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(passportDetails);
    }
}
