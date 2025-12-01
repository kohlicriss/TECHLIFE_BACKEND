package com.hrms.project.controller;

import com.hrms.project.dto.AchievementsDTO;
import com.hrms.project.dto.SkillsDTO;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.AchievementsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class AchievementsController {

    @Autowired
    private AchievementsServiceImpl achievementsServiceImpl;

    @PostMapping("/achievements/{employeeId}")
    @CheckPermission(value="ACHIEVEMENTS_ACHIEVEMENT_ADD_ACHIEVEMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"})
    public ResponseEntity<AchievementsDTO> addAchievements(@PathVariable String employeeId,
                                                           @RequestPart(value="achievementFile",required = false) MultipartFile achievementFile,
                                                           @RequestPart(value="achievementsDTO") AchievementsDTO achievementsDTO) throws IOException {
        return new ResponseEntity<>(achievementsServiceImpl.addAchievements(employeeId,achievementFile,achievementsDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{employeeId}/achievements")
    @CheckPermission("ACHIEVEMENTS_GET_ACHIEVEMENT")
    public ResponseEntity<List<AchievementsDTO>>  getAchievements(@PathVariable String employeeId) {
        List<AchievementsDTO> certifications=achievementsServiceImpl.getCertifications(employeeId);
        return new ResponseEntity<>(certifications,HttpStatus.OK);
    }

    @PutMapping("/{employeeId}/{certificateId}/achievements")
    @CheckPermission(
            value = "ACHIEVEMENTS_EDIT_ACHIEVEMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<AchievementsDTO> updateAchievements(
            @PathVariable String employeeId,
            @PathVariable String certificateId,
            @RequestPart(value="achievementFile",required = false) MultipartFile achievementFile,
            @RequestPart(value="achievementsDTO") AchievementsDTO achievementDTO) throws IOException {
        return new ResponseEntity<>( achievementsServiceImpl.updateAchievements(employeeId, certificateId, achievementFile, achievementDTO),HttpStatus.CREATED);
    }

    @GetMapping("/{achievementId}/achievement")

    public ResponseEntity<AchievementsDTO> getAchievement(@PathVariable String achievementId) {
        return new ResponseEntity<>(achievementsServiceImpl.getAchievement(achievementId),HttpStatus.OK);
    }

    @DeleteMapping("/{employeeId}/{certificateId}/achievements")
    @CheckPermission(
            value = "ACHIEVEMENTS_DELETE_ACHIEVEMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<AchievementsDTO> deleteAchievements(@PathVariable String employeeId, @PathVariable  String certificateId) {
        return new ResponseEntity<>(achievementsServiceImpl.deleteAchievements(employeeId,certificateId),HttpStatus.OK);
    }

    @PostMapping("/{employeeId}/skills")
    @CheckPermission("CREATE_SKILLS")

    public ResponseEntity<SkillsDTO> addSkills(@PathVariable String employeeId,
                                               @RequestBody SkillsDTO resumeDTO) {
        return new ResponseEntity<>(achievementsServiceImpl.addSkills(employeeId, resumeDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{employeeId}/skills")
    @CheckPermission(
            value = "UPDATE_SKILLS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<SkillsDTO> updateSkills(@PathVariable String employeeId, @RequestBody SkillsDTO resumeDTO) {
        return  new ResponseEntity<>(achievementsServiceImpl.updateSkills(employeeId, resumeDTO), HttpStatus.OK);
    }

    @GetMapping("/{employeeId}/skills")
    @CheckPermission("GET_SKILLS")

    public ResponseEntity<SkillsDTO> getSkills(@PathVariable String employeeId) {
        return  new ResponseEntity<>(achievementsServiceImpl.getSkills(employeeId), HttpStatus.OK);

    }

    @DeleteMapping("/{employeeId}/skills")
    @CheckPermission(
            value = "DELETE_SKILLS",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )    public ResponseEntity<SkillsDTO> deleteSkills(@PathVariable String employeeId) {
        return  new ResponseEntity<>(achievementsServiceImpl.deleteSkills(employeeId), HttpStatus.OK);
    }

}







