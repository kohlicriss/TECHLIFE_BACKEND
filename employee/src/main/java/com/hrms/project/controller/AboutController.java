package com.hrms.project.controller;

import com.hrms.project.dto.AboutDTO;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.AboutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/employee")
public class AboutController {

    @Autowired
    private AboutService aboutService;


    @PostMapping("/{employeeId}/createAbout")
    @CheckPermission(
            value = "PROFILE_ABOUT_ADD_DESCRIPTION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<AboutDTO>createAbout(@PathVariable String employeeId,@RequestBody AboutDTO aboutDTO){
        AboutDTO about=aboutService.createAbout(employeeId,aboutDTO);
        return new ResponseEntity<>(about, HttpStatus.CREATED);
    }

    @PutMapping("/{employeeId}/updateAbout")
    @CheckPermission(
            value = "PROFILE_ABOUT_EDIT_DESCRIPTION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<AboutDTO>updateAbout(@PathVariable String employeeId,@RequestBody AboutDTO aboutDTO) {
        AboutDTO about = aboutService.updateAbout(employeeId,aboutDTO);
        return new ResponseEntity<>(about, HttpStatus.OK);
    }

    @GetMapping("about/{employeeId}")
    @CheckPermission("PROFILE_ABOUT_GET_DESCRIPTION")
    public ResponseEntity<AboutDTO>getAboutByEmployee(@PathVariable String employeeId){
        AboutDTO about=aboutService.getAboutByEmployee(employeeId);
        return new ResponseEntity<>(about,HttpStatus.OK);
    }

    @DeleteMapping("about/{employeeId}")
    @CheckPermission(
            value = "PROFILE_ABOUT_DELETE_DESCRIPTION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<String>deleteAbout(@PathVariable String employeeId){
        aboutService.deleteAbout(employeeId);
        return ResponseEntity.ok("About details deleted successfully");
    }
}