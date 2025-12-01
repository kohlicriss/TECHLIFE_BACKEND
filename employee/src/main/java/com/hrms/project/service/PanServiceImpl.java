package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.PanDetails;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.PanDTO;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.PanDetailsRepository;
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
public class PanServiceImpl {
    @Autowired
    private PanDetailsRepository panDetailsRepository;

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationClient notificationClient;

   @Autowired
   private S3Service s3Service;

   @Autowired
   private EmployeeAsyncService employeeAsyncService;

    @Async("employeeTaskExecutor")
    public CompletableFuture<PanDetails> createPan(String employeeId, MultipartFile panImage, PanDTO panDTO) throws IOException {
        log.info("Creating PAN details for employee {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + employeeId));

        if (panDetailsRepository.findByEmployee_EmployeeId(employeeId).isPresent()) {
            log.warn("Employee {} already has a PAN assigned", employeeId);

            throw new APIException("This employee already has a PAN assigned");
        }

        if (panImage == null || panImage.isEmpty()) {
            log.error("PAN image not provided for employee {}", employeeId);
            throw new APIException("PAN image is required and cannot be null or empty");
        }

        Optional<PanDetails> existingPanOpt = panDetailsRepository.findById(panDTO.getPanNumber());

        PanDetails panDetails;
        if (existingPanOpt.isPresent()) {
            log.debug("PAN {} already exists in DB", panDTO.getPanNumber());
            panDetails = existingPanOpt.get();
            if (panDetails.getEmployee() == null)
            {
                log.info("Assigning existing PAN {} to employee {}", panDTO.getPanNumber(), employeeId);
                panDetails.setEmployee(employee);
            }
            else
            {
                log.error("PAN {} already assigned to another employee", panDTO.getPanNumber());
                throw new APIException("This PAN is already assigned to another employee");
            }
        }
        else
        {
            log.debug("Creating new PAN entry {}", panDTO.getPanNumber());
            panDetails = new PanDetails();
            modelMapper.map(panDTO,panDetails);
            panDetails.setEmployee(employee);
        }

        String s3Key = s3Service.uploadFile(employeeId, "panCard", panImage);
        log.info("PAN image uploaded to S3 for employee {} with key {}", employeeId, s3Key);
        panDetails.setPanImage(s3Key);

        PanDetails saved = panDetailsRepository.save(panDetails);
        log.info("PAN details saved successfully for employee {}", employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DOCUMENTS")
                    .type("PAN_CREATE")
                    .kind("INFO")
                    .sender("HR")
                    .subject("PAN Added")
                    .message("Your PAN details have been successfully added.")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PAN creation for employee {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send PAN creation notification for employee {}: {}", employeeId, e.getMessage());
        }

        return CompletableFuture.completedFuture(saved);
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<PanDTO> getPanDetails(String employeeId) {
        log.info("Fetching PAN details for employee {}", employeeId);

        Employee employee=employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + employeeId));

        PanDetails details=employee.getPanDetails();

        if(details!=null)
        {
            PanDTO panDTO=modelMapper.map(details,PanDTO.class);
            if(details.getPanImage()!=null){
                String presignedUrl=s3Service.generatePresignedUrl(details.getPanImage());
                log.debug("Generated presigned URL for PAN image of employee {}", employeeId);

                panDTO.setPanImage(presignedUrl);
            }
            log.info("Returning PAN details for employee {}", employeeId);

            return CompletableFuture.completedFuture(panDTO);
        }
        else
        {
            log.warn("No PAN details found for employee {}", employeeId);

            throw new APIException("This employee does not have a PAN assigned");
        }

    }
    @Async("employeeTaskExecutor")

    public CompletableFuture<PanDetails> UpdatePanDetails(String employeeId, MultipartFile panImage, PanDTO panDTO) throws IOException {
        log.info("Updating PAN details for employee {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + employeeId));

        PanDetails existing = employee.getPanDetails();
        if (existing == null) {
            log.warn("PAN details not found for employee {}", employeeId);
            throw new APIException("PAN Details not found for employee: " + employeeId);
        }

        if (!existing.getPanNumber().equals(panDTO.getPanNumber())) {
            log.error("PAN number change attempt detected for employee {}", employeeId);
            throw new APIException("PAN number cannot be changed once submitted");
        }

        if (panImage != null && !panImage.isEmpty()) {
            String oldKey = existing.getPanImage();
            String newKey = s3Service.uploadFile(employeeId, "panCard", panImage);
            log.info("Uploaded new PAN image for employee {}", employeeId);

            if (!oldKey.equals(newKey)) {
                s3Service.deleteFile(oldKey);
                log.debug("Deleted old PAN image {} for employee {}", oldKey, employeeId);

            }
            existing.setPanImage(newKey);
        }

        existing.setPanName(panDTO.getPanName());
        existing.setDateOfBirth(panDTO.getDateOfBirth());
        existing.setParentsName(panDTO.getParentsName());

        PanDetails updated = panDetailsRepository.save(existing);
        log.info("PAN details updated successfully for employee {}", employeeId);

        try {
            employeeAsyncService.sendNotification(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DOCUMENTS")
                    .type("PAN_UPDATE")
                    .kind("INFO")
                    .sender("HR")
                    .subject("PAN Updated")
                    .message("Your PAN details have been updated successfully.")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PAN update for employee {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send PAN update notification for employee {}: {}", employeeId, e.getMessage());
        }

        return CompletableFuture.completedFuture(updated);
    }


    @Async("employeeTaskExecutor")

    public CompletableFuture<PanDetails> deletePanByEmployeeId(String employeeId) {
        log.info("Deleting PAN details for employee {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with ID {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with ID: " + employeeId);
                });

        PanDetails panDetails = employee.getPanDetails();
        if (panDetails == null) {
            log.warn("No PAN details found for employee {}", employeeId);
            throw new APIException("PAN details not found for employeeId " + employeeId);
        }

        if (panDetails.getPanImage() != null) {
            s3Service.deleteFile(panDetails.getPanImage());
            log.debug("Deleted PAN image from S3 for employee {}", employeeId);
        }

        employee.setPanDetails(null);
        employeeRepository.save(employee);

        panDetailsRepository.deleteById(panDetails.getPanNumber());
        log.info("PAN details deleted from DB for employee {}", employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("DOCUMENTS")
                    .type("PAN_DELETE")
                    .kind("WARNING")
                    .sender("HR")
                    .subject("PAN Deleted")
                    .message("Your PAN details have been deleted.")
                    .link("/profile/" + employeeId + "/documents")
                    .build());
            log.info("Notification sent for PAN deletion for employee {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send PAN delete notification for employee {}: {}", employeeId, e.getMessage());
        }

        return CompletableFuture.completedFuture(panDetails);
    }
}