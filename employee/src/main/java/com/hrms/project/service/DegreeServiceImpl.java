package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.entity.DegreeCertificates;
import com.hrms.project.entity.Employee;
import com.hrms.project.handlers.DegreeNotFoundException;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.DegreeDTO;
import com.hrms.project.repository.DegreeCertificatesRepository;
import com.hrms.project.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DegreeServiceImpl {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DegreeCertificatesRepository degreeCertificatesRepository;

    @Autowired
    private NotificationClient notificationClient;

    public List<DegreeCertificates> getDegree(String employeeId) {
        log.info("Fetching degree with employee {}",employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id " + employeeId));

        List<DegreeCertificates> degreeCertificates = employee.getDegreeCertificates();

        if (degreeCertificates == null || degreeCertificates.isEmpty()) {
            log.warn("No degree certificates found for employee {}", employeeId);

            return Collections.emptyList();
        }

        degreeCertificates.forEach(d -> {
            if (d.getAddFiles() != null) {
                d.setAddFiles(s3Service.generatePresignedUrl(d.getAddFiles()));
            }
        });
        log.info("Successfully fetched {} degree certificate(s) for employee {}", degreeCertificates.size(), employeeId);
        return degreeCertificates;
    }



    public DegreeCertificates deleteById(String employeeId, String id) {
        log.info("Deleting degree {} for employee {}", id, employeeId);


        DegreeCertificates degreeCertificates = degreeCertificatesRepository.findById(id)
                .orElseThrow(() -> new APIException("Degree certificate not found with ID: " + id));

        if (!degreeCertificates.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new APIException("This degree certificate does not belong to the given employee." + employeeId);
        }

        if (degreeCertificates.getAddFiles() != null && !degreeCertificates.getAddFiles().isEmpty()) {
            s3Service.deleteFile(degreeCertificates.getAddFiles());
            log.info("Successfully deleted file {} from S3", degreeCertificates.getAddFiles());

        }
        degreeCertificatesRepository.deleteById(id);

        try {
            notificationClient.send(
                    NotificationRequest.builder()
                            .receiver(employeeId)
                            .message("Degree certificate deleted successfully.")
                            .sender("SYSTEM")
                            .type("DEGREE")
                            .link("/profile/" + employeeId + "/profile")
                            .category("DOCUMENT")
                            .kind("DELETED")
                            .subject("Degree Deleted")
                            .build()
            );
            log.info("Notification is sent to employee {}",employeeId);
        } catch (Exception e) {
            log.debug("Failed to send degree delete notification: " + e.getMessage());
        }
        log.info("Deleted degree {} for employee {}", id, employeeId);

        return degreeCertificates;
    }


    public DegreeDTO addDegree(String employeeId, MultipartFile addFiles, @Valid DegreeCertificates degreeCertificates) throws IOException {
        log.info("Degree added for employee {}",employeeId);
        long count = degreeCertificatesRepository.countByEmployeeEmployeeId(employeeId);
        String newDegreeId =  "DEGREE" + String.format("%03d", count + 1);

        while (degreeCertificatesRepository.existsByEmployeeEmployeeIdAndId(employeeId, newDegreeId)) {
            count++;
            newDegreeId =  "DEGREE" + String.format("%03d", count + 1);
            log.warn("new DegreeId {} added for employee {}",newDegreeId,employeeId);
        }
        if (degreeCertificatesRepository.existsByEmployeeEmployeeIdAndDegreeType(employeeId, degreeCertificates.getDegreeType())) {
            throw new IllegalArgumentException("This degree type already exists for the employee.");
        }
        String fileKey = s3Service.uploadDegreeFile(employeeId,"degree", degreeCertificates.getDegreeType(),addFiles);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        degreeCertificates.setEmployee(employee);
        degreeCertificates.setAddFiles(fileKey);
        degreeCertificates.setId(newDegreeId);
        DegreeCertificates saved=degreeCertificatesRepository.save(degreeCertificates);
        log.info("Degree Saved {} for employee{}",saved,employeeId);
        try {
            notificationClient.send(
                    NotificationRequest.builder()
                            .receiver(employeeId)
                            .message("New degree certificate added successfully.")
                            .sender("SYSTEM")
                            .type("DEGREE")
                            .link("/profile/" + employeeId + "/profile")
                            .category("DOCUMENT")
                            .kind("CREATED")
                            .subject("Degree Added")
                            .build()
            );
            log.info("Notification sent to employee {}",employeeId);
        } catch (Exception e) {
            log.debug("Failed to send degree add notification: " + e.getMessage());
        }
        return modelMapper.map(saved,DegreeDTO.class);

    }

    public DegreeDTO updateDegree(String employeeId, MultipartFile addFiles, String id, @Valid DegreeCertificates degreeCertificates) throws IOException {

        log.info("update degree certificates for employee {}",employeeId);
        DegreeCertificates existingDegree = degreeCertificatesRepository.findById(id)
                .orElseThrow(() -> new APIException("Degree not found with ID: " + id));


        if (!existingDegree.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new APIException("This degree does not belong to the given employee");
        }

        if (!existingDegree.getDegreeType().equals(degreeCertificates.getDegreeType())) {
            boolean exists = degreeCertificatesRepository.existsByEmployeeEmployeeIdAndDegreeType(employeeId, degreeCertificates.getDegreeType());
            if (exists) {
                throw new IllegalArgumentException("This degree type already exists for the employee.");
            }
            existingDegree.setDegreeType(degreeCertificates.getDegreeType());
        }
        log.debug("Updating degree details for employeeId={}", employeeId);

        existingDegree.setBranchOrSpecialization(degreeCertificates.getBranchOrSpecialization());
        existingDegree.setStartMonth(degreeCertificates.getStartMonth());
        existingDegree.setEndMonth(degreeCertificates.getEndMonth());
        existingDegree.setStartYear(degreeCertificates.getStartYear());
        existingDegree.setEndYear(degreeCertificates.getEndYear());
        existingDegree.setCgpaOrPercentage(degreeCertificates.getCgpaOrPercentage());
        existingDegree.setUniversityOrCollege(degreeCertificates.getUniversityOrCollege());
        existingDegree.setDegreeType(degreeCertificates.getDegreeType());

        if (addFiles != null && !addFiles.isEmpty()) {
            log.info("Updating degree file for employeeId={} and degreeId={}", employeeId, id);

            if (existingDegree.getAddFiles() != null) {
                log.debug("Deleting old file {} for employeeId={}", existingDegree.getAddFiles(), employeeId);

                s3Service.deleteFile(existingDegree.getAddFiles());
            }
            String fileKey = s3Service.uploadDegreeFile(employeeId,"degree", existingDegree.getDegreeType(),addFiles);
            log.debug("Uploaded new degree file={} for employeeId={}", fileKey, employeeId);
            existingDegree.setAddFiles(fileKey);
        }
        DegreeCertificates saved=degreeCertificatesRepository.save(existingDegree);
        log.info("Degree with ID={} successfully updated for employeeId={}", saved.getId(), employeeId);

        try {
            notificationClient.send(
                    NotificationRequest.builder()
                            .receiver(employeeId)
                            .message("Degree certificate updated successfully.")
                            .sender("SYSTEM")
                            .type("DEGREE")
                            .link("/profile/" + employeeId + "/profile")
                            .category("DOCUMENT")
                            .kind("UPDATED")
                            .subject("Degree Updated")
                            .build()
            );
            log.info("Notification sent successfully for employeeId={}", employeeId);

        } catch (Exception e) {
            log.error("Failed to send degree update notification: {}", e.getMessage());
        }
        return modelMapper.map(saved, DegreeDTO.class);

    }

    public DegreeDTO getById(String employeeId, String id) {
        log.info("Fetching degree from employee {}",employeeId);
        DegreeCertificates degreeCertificates = degreeCertificatesRepository.findById(id)
                .orElseThrow(() -> new DegreeNotFoundException("Degree not found with id :" + id));

        if (!degreeCertificates.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new APIException("This degree does not belong to the given employee");

        }
        log.info("Successfully fetched degree {} for employee {}", id, employeeId);
        return modelMapper.map(degreeCertificates,DegreeDTO.class);

    }
}