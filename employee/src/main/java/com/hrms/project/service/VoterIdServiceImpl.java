package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.Employee;
import com.hrms.project.entity.VoterDetails;
import com.hrms.project.dto.VoterDTO;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.repository.EmployeeRepository;
import com.hrms.project.repository.VoterIdRepository;
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
public class VoterIdServiceImpl {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VoterIdRepository voterIdRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Async("employeeTaskExecutor")
    public CompletableFuture<VoterDetails> createVoter(String employeeId, MultipartFile voterImage,
                                                       VoterDTO voterDTO) throws IOException {
        log.info("Creating Voter ID for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });

        if (voterIdRepository.findByEmployee_EmployeeId(employeeId).isPresent()) {
            log.warn("Employee {} already has a Voter ID assigned", employeeId);
            throw new APIException("This employee already has a Voter ID assigned");
        }

        Optional<VoterDetails> existingVoterOpt = voterIdRepository.findById(voterDTO.getVoterIdNumber());
        VoterDetails cardDetails;

        if (existingVoterOpt.isEmpty()) {
            log.debug("No existing Voter ID found with number {}, creating new", voterDTO.getVoterIdNumber());

            cardDetails = new VoterDetails();
            modelMapper.map(voterDTO, cardDetails);

            if (voterImage == null || voterImage.isEmpty()) {
                log.error("Voter image is empty for employee {}", employeeId);
                throw new APIException("Voter image is empty and it should not be null");
            }

            String fileName = s3Service.uploadFile(employeeId, "voterImage", voterImage);
            log.debug("Uploaded voter image for employee {}: {}", employeeId, fileName);
            cardDetails.setUploadVoter(fileName);
            cardDetails.setEmployee(employee);
            voterIdRepository.save(cardDetails);

        } else {
            VoterDetails existing = existingVoterOpt.get();

            if (existing.getEmployee() == null) {
                log.debug("Assigning existing Voter ID {} to employee {}", existing.getVoterIdNumber(), employeeId);
                existing.setEmployee(employee);
                modelMapper.map(voterDTO, existing);
                cardDetails = voterIdRepository.save(existing);
            } else {
                log.warn("Voter ID {} is already assigned to another employee", existing.getVoterIdNumber());
                throw new APIException("This Voter ID is already assigned to another employee");
            }
        }

        notificationClient.send(NotificationRequest.builder()
                .receiver(employeeId)
                .category("DOCUMENTS")
                .type("VOTER_CREATE")
                .kind("INFO")
                .sender("HR")
                .subject("Voter ID Added")
                .message("Your Voter ID details have been successfully added.")
                .link("/profile/" + employeeId + "/documents")
                .build());

        log.info("Voter ID created successfully for employee {}", employeeId);
        return CompletableFuture.completedFuture(cardDetails);
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<VoterDTO> getVoterByEmployee(String employeeId) {
        log.info("Fetching Voter ID details for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });

        VoterDetails voterDetails = employee.getVoterDetails();
        if (voterDetails == null) {
            log.warn("No Voter ID details found for employee {}", employeeId);
            throw new APIException("Voter ID details not found for this employee");
        }

        if (voterDetails.getUploadVoter() != null) {
            String presignedUrl = s3Service.generatePresignedUrl(voterDetails.getUploadVoter());
            voterDetails.setUploadVoter(presignedUrl);
            log.debug("Generated presigned URL for employee {} voter image", employeeId);
        }

        return CompletableFuture.completedFuture(modelMapper.map(voterDetails, VoterDTO.class));
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<VoterDetails> updateVoter(String employeeId, MultipartFile voterImage, VoterDTO voterDTO) throws IOException {
        log.info("Updating Voter ID for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });

        VoterDetails existing = employee.getVoterDetails();
        if (existing == null) {
            log.warn("No Voter ID details found for employee {}", employeeId);
            throw new APIException("Voter ID details not found for this employee");
        }

        if (!existing.getVoterIdNumber().equals(voterDTO.getVoterIdNumber())) {
            log.warn("Attempted to change Voter ID number for employee {} from {} to {}",
                    employeeId, existing.getVoterIdNumber(), voterDTO.getVoterIdNumber());
            throw new APIException("Voter ID number cannot be changed once submitted");
        }

        if (voterImage != null && !voterImage.isEmpty()) {
            String oldKey = existing.getUploadVoter();
            String newKey = s3Service.uploadFile(employeeId, "voterImage", voterImage);
            if (oldKey != null && !oldKey.equals(newKey)) {
                s3Service.deleteFile(oldKey);
                log.debug("Deleted old voter image for employee {}: {}", employeeId, oldKey);
            }
            existing.setUploadVoter(newKey);
            log.debug("Uploaded new voter image for employee {}: {}", employeeId, newKey);
        }

        existing.setFullName(voterDTO.getFullName());
        existing.setRelationName(voterDTO.getRelationName());
        existing.setGender(voterDTO.getGender());
        existing.setDateOfBirth(voterDTO.getDateOfBirth());
        existing.setAddress(voterDTO.getAddress());
        existing.setIssuedDate(voterDTO.getIssuedDate());

        VoterDetails updated = voterIdRepository.save(existing);
        log.info("Voter ID details updated successfully for employee {}", employeeId);

        notificationClient.send(NotificationRequest.builder()
                .receiver(employeeId)
                .category("DOCUMENTS")
                .type("VOTER_UPDATE")
                .kind("INFO")
                .sender("HR")
                .subject("Voter ID Updated")
                .message("Your Voter ID details have been updated successfully.")
                .link("/profile/" + employeeId + "/documents")
                .build());

        return CompletableFuture.completedFuture(updated);
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<VoterDetails> deleteByEmployeeId(String employeeId) {
        log.info("Deleting Voter ID for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
                });

        VoterDetails voterDetails = employee.getVoterDetails();
        if (voterDetails == null) {
            log.warn("No Voter ID details found to delete for employee {}", employeeId);
            throw new APIException("Voter ID details not found for this employee: " + employeeId);
        }

        if (voterDetails.getUploadVoter() != null) {
            s3Service.deleteFile(voterDetails.getUploadVoter());
            log.debug("Deleted voter image for employee {}: {}", employeeId, voterDetails.getUploadVoter());
        }

        employee.setVoterDetails(null);
        employeeRepository.save(employee);
        voterIdRepository.deleteById(voterDetails.getVoterIdNumber());

        notificationClient.send(NotificationRequest.builder()
                .receiver(employeeId)
                .category("DOCUMENTS")
                .type("VOTER_DELETE")
                .kind("WARNING")
                .sender("HR")
                .subject("Voter ID Deleted")
                .message("Your Voter ID details have been deleted.")
                .link("/profile/" + employeeId + "/documents")
                .build());

        log.info("Voter ID deleted successfully for employee {}", employeeId);
        return CompletableFuture.completedFuture(voterDetails);
    }
}
