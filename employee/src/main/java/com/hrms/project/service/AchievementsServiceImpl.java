package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.NotificationRequest;
import com.hrms.project.dto.SkillsDTO;
import com.hrms.project.entity.Achievements;
import com.hrms.project.entity.Employee;
import com.hrms.project.handlers.APIException;
import com.hrms.project.handlers.EmployeeNotFoundException;
import com.hrms.project.dto.AchievementsDTO;
import com.hrms.project.repository.AchievementsRepository;
import com.hrms.project.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class AchievementsServiceImpl {

    @Autowired
    private AchievementsRepository achievementsRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ModelMapper modelMapper;

    public AchievementsDTO addAchievements(String employeeId, MultipartFile achievementFile, AchievementsDTO achievementsDTO) throws IOException {
        log.info("Adding achievement for employeeId={}", employeeId);

        if (achievementsRepository.existsByEmployeeEmployeeIdAndCertificationName(employeeId, achievementsDTO.getCertificationName())) {
            log.warn("Duplicate achievement found for employeeId={} and certificationName={}", employeeId, achievementsDTO.getCertificationName());
            throw new APIException("This certification already exists for the employee.");
        }

        long count = achievementsRepository.countByEmployeeEmployeeId(employeeId);
        String newAchievId = "ACHIEVEMENT" + String.format("%03d", count + 1);
        while (achievementsRepository.existsByEmployeeEmployeeIdAndId(employeeId, newAchievId)) {
            count++;
            newAchievId = "ACHIEVEMENT" + String.format("%03d", count + 1);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        Achievements achievements = new Achievements();
        achievements.setId(newAchievId);
        achievements.setCertificationName(achievementsDTO.getCertificationName());
        achievements.setExpirationYear(achievementsDTO.getExpirationYear());
        achievements.setExpirationMonth(achievementsDTO.getExpirationMonth());
        achievements.setCertificationURL(achievementsDTO.getCertificationURL());
        achievements.setIssueMonth(achievementsDTO.getIssueMonth());
        achievements.setIssueYear(achievementsDTO.getIssueYear());
        achievements.setIssuingAuthorityName(achievementsDTO.getIssuingAuthorityName());
        achievements.setLicenseNumber(achievementsDTO.getLicenseNumber());
        achievements.setEmployee(employee);

        if (achievementFile != null && !achievementFile.isEmpty()) {
            log.info("Uploading achievement file for employeeId={} certification={}", employeeId, achievementsDTO.getCertificationName());
            String fileKey = s3Service.uploadDegreeFile(employeeId, "achievements", achievements.getCertificationName(), achievementFile);
            achievements.setAchievementFile(fileKey);
        }

        Achievements saved = achievementsRepository.save(achievements);
        log.info("Achievement saved successfully with id={} for employeeId={}", saved.getId(), employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .category("ACHIEVEMENTS")
                    .message("New achievement '" + saved.getCertificationName() + "' added.")
                    .sender("HR")
                    .type("ACHIEVEMENT_ADD")
                    .kind("INFO")
                    .subject("Achievement Added")
                    .link("/profile/" + employeeId + "/achievements")
                    .build());
            log.info("Achievement add notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send achievement add notification: {}", e.getMessage());
        }

        return modelMapper.map(saved, AchievementsDTO.class);
    }

    public List<AchievementsDTO> getCertifications(String employeeId) {
        log.info("Fetching certifications for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        List<Achievements> achievements = employee.getAchievements();
        log.info("Found {} achievements for employeeId={}", achievements.size(), employeeId);

        return achievements.stream().map(a -> {
            AchievementsDTO dto = modelMapper.map(a, AchievementsDTO.class);
            if (a.getAchievementFile() != null && !a.getAchievementFile().isBlank()) {
                String url = s3Service.generatePresignedUrl(a.getAchievementFile());
                dto.setAchievementFile(url);
            }
            return dto;
        }).toList();
    }

    public AchievementsDTO getAchievement(String achievementId) {
        log.info("Fetching achievement by id={}", achievementId);

        Achievements achievements = achievementsRepository.findById(achievementId)
                .orElseThrow(() -> {
                    log.error("Achievement not found with id {}", achievementId);
                    return new APIException("Achievement not found with id " + achievementId);
                });

        if (achievements.getAchievementFile() != null && !achievements.getAchievementFile().isBlank()) {
            String fileUrl = s3Service.generatePresignedUrl(achievements.getAchievementFile());
            achievements.setAchievementFile(fileUrl);
        }
        return modelMapper.map(achievements, AchievementsDTO.class);
    }

    public AchievementsDTO updateAchievements(String employeeId, String certificateId,
                                              MultipartFile achievementFile, AchievementsDTO achievementDTO) throws IOException {
        log.info("Updating achievement for employeeId={} certificateId={}", employeeId, certificateId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        if (employee.getAchievements() == null || employee.getAchievements().isEmpty()) {
            log.warn("No achievements found for employeeId={}", employeeId);
            throw new RuntimeException("No achievements found for employee " + employeeId);
        }

        Achievements achievementToUpdate = employee.getAchievements().stream()
                .filter(achieve -> achieve.getId().equals(certificateId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Achievement not found with certificate ID {}", certificateId);
                    return new RuntimeException("Achievement with certificate ID " + certificateId + " not found.");
                });

        if (achievementFile != null && !achievementFile.isEmpty()) {
            log.info("Updating achievement file for certificateId={}", certificateId);
            if (achievementToUpdate.getAchievementFile() != null && !achievementToUpdate.getAchievementFile().isEmpty()) {
                s3Service.deleteFile(achievementToUpdate.getAchievementFile());
            }
            String fileKey = s3Service.uploadDegreeFile(employeeId, "achievements", achievementToUpdate.getCertificationName(), achievementFile);
            achievementToUpdate.setAchievementFile(fileKey);
        }

        achievementsRepository.save(achievementToUpdate);
        log.info("Achievement updated successfully for certificateId={}", certificateId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("ACHIEVEMENTS")
                    .message("Achievement '" + achievementToUpdate.getCertificationName() + "' has been updated.")
                    .sender("HR")
                    .type("ACHIEVEMENT_UPDATE")
                    .kind("INFO")
                    .subject("Achievement Updated")
                    .link("/profile/" + employeeId + "/achievements")
                    .build());
            log.info("Achievement update notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send achievement update notification: {}", e.getMessage());
        }

        return modelMapper.map(achievementToUpdate, AchievementsDTO.class);
    }

    public AchievementsDTO deleteAchievements(String employeeId, String certificateId) {
        log.info("Deleting achievement for employeeId={} certificateId={}", employeeId, certificateId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        Achievements achievementToDelete = employee.getAchievements().stream()
                .filter(achieve -> achieve.getId().equals(certificateId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Achievement with certificate ID {} not found", certificateId);
                    return new RuntimeException("Achievement with certificate ID " + certificateId + " not found.");
                });

        if (achievementToDelete.getAchievementFile() != null && !achievementToDelete.getAchievementFile().isEmpty()) {
            s3Service.deleteFile(achievementToDelete.getAchievementFile());
        }

        achievementsRepository.delete(achievementToDelete);
        log.info("Achievement deleted successfully for certificateId={}", certificateId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("ACHIEVEMENTS")
                    .message("Achievement '" + achievementToDelete.getCertificationName() + "' has been deleted.")
                    .sender("HR")
                    .type("ACHIEVEMENT_DELETE")
                    .kind("ALERT")
                    .subject("Achievement Deleted")
                    .link("/profile/" + employeeId + "/achievements")
                    .build());
            log.info("Achievement delete notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send achievement delete notification: {}", e.getMessage());
        }

        return modelMapper.map(achievementToDelete, AchievementsDTO.class);
    }

    public SkillsDTO addSkills(String employeeId, SkillsDTO resumeDTO) {
        log.info("Adding skills for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        if (employee.getSkills() != null && !employee.getSkills().isEmpty()) {
            log.warn("Skills already exist for employeeId={}", employeeId);
            throw new APIException("Skills already exist for this employee cannot add again.");
        }

        modelMapper.map(resumeDTO, employee);
        log.info("Skills mapped successfully for employeeId={}", employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("SKILLS")
                    .message("Skills have been added to your profile.")
                    .sender("HR")
                    .type("SKILL_ADD")
                    .kind("INFO")
                    .subject("Skills Added")
                    .link("/employee/" + employeeId + "/skills")
                    .build());
            log.info("Skills add notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send skills add notification: {}", e.getMessage());
        }

        return modelMapper.map(employee, SkillsDTO.class);
    }

    public SkillsDTO updateSkills(String employeeId, SkillsDTO resumeDTO) {
        log.info("Updating skills for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        modelMapper.map(resumeDTO, employee);
        employeeRepository.save(employee);
        log.info("Skills updated successfully for employeeId={}", employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("SKILLS")
                    .message("Your skills have been updated.")
                    .sender("HR")
                    .type("SKILL_UPDATE")
                    .kind("INFO")
                    .subject("Skills Updated")
                    .link("/employee/" + employeeId + "/skills")
                    .build());
            log.info("Skills update notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send skills update notification: {}", e.getMessage());
        }

        return modelMapper.map(employee, SkillsDTO.class);
    }

    public SkillsDTO getSkills(String employeeId) {
        log.info("Fetching skills for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        SkillsDTO resumeDTO = modelMapper.map(employee, SkillsDTO.class);
        resumeDTO.setSkills(employee.getSkills());
        log.info("Skills fetched successfully for employeeId={}", employeeId);

        return resumeDTO;
    }

    public SkillsDTO deleteSkills(String employeeId) {
        log.info("Deleting skills for employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id {}", employeeId);
                    return new EmployeeNotFoundException("Employee not found with id " + employeeId);
                });

        if (employee.getSkills() == null || employee.getSkills().isEmpty()) {
            log.warn("No skills found to delete for employeeId={}", employeeId);
            throw new APIException("No skills found to delete for this employee.");
        }

        employee.setSkills(null);
        employeeRepository.save(employee);
        log.info("Skills deleted successfully for employeeId={}", employeeId);

        try {
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .category("SKILLS")
                    .message("Your skills have been deleted.")
                    .sender("HR")
                    .type("SKILL_DELETE")
                    .kind("ALERT")
                    .subject("Skills Deleted")
                    .link("/employee/" + employeeId + "/skills")
                    .build());
            log.info("Skills delete notification sent successfully to employeeId={}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send skills delete notification: {}", e.getMessage());
        }

        SkillsDTO resumeDTO = modelMapper.map(employee, SkillsDTO.class);
        resumeDTO.setSkills(employee.getSkills());

        return resumeDTO;
    }
}
