package com.hrms.project.controller;

import com.hrms.project.entity.*;
import com.hrms.project.dto.*;
import com.hrms.project.handlers.UnauthorizedAccessException;
import com.hrms.project.security.CheckEmployeeAccess;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/employee")

public class IdentityDetailsController {

    @Autowired
    private AadhaarServiceImpl aadhaarServiceImpl;

    @Autowired
    private PanServiceImpl panServiceImpl;

    @Autowired
    private DrivingLicenseServiceImpl drivingLicenseServiceImpl;

    @Autowired
    private PassportDetailsServiceImpl passportDetailsImpl;

    @Autowired
    private DegreeServiceImpl degreeServiceImpl;

    @Autowired
    private VoterIdServiceImpl  voterIdServiceImpl;

    @Autowired
    private WorkExperienceServiceImpl workExperienceServiceImpl;


    @PostMapping("/aadhaar/{employeeId}")
    @CheckPermission("PROFILE_DOCUMENTS_AADHAAR_CARD_ADD_DOCUMENT")
    public CompletableFuture<ResponseEntity<AadhaarCardDetails>> save(@PathVariable String employeeId,
                                                   @RequestPart(value="aadhaarImage",required = true) MultipartFile aadhaarImage,
                                                   @Valid @RequestPart(value="aadhaar") AadhaarDTO aadhaarCardDetails) throws IOException {


        return aadhaarServiceImpl.createAadhaar(employeeId, aadhaarImage, aadhaarCardDetails)
                .thenApply(aadhaar -> ResponseEntity.status(201).body(aadhaar));
    }

    @GetMapping("/{employeeId}/aadhaar")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_AADHAAR_CARD_GET_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE","ROLE_TEAM_LEAD","ROLE_MANAGER"}
    )
    public CompletableFuture<ResponseEntity<AadhaarDTO>> getAadhaar(@PathVariable String employeeId) {
        return aadhaarServiceImpl.getAadhaarByEmployeeId(employeeId)
                .thenApply(ResponseEntity::ok);
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PutMapping("/{employeeId}/aadhaar")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_AADHAAR_CARD_EDIT_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
  //  @CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD","MANAGER"})
    public CompletableFuture<ResponseEntity<AadhaarCardDetails>>updateAadhaar(@PathVariable String employeeId,
                                                           @RequestPart(value="aadhaarImage", required = false) MultipartFile aadhaarImage,
                                                           @Valid @RequestPart(value="aadhaar") AadhaarDTO aadhaarDTO) throws IOException{
        CompletableFuture<AadhaarCardDetails> dto = aadhaarServiceImpl.updateAadhaar(employeeId,aadhaarImage,aadhaarDTO);
        return aadhaarServiceImpl.updateAadhaar(employeeId, aadhaarImage, aadhaarDTO)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/{employeeId}/aadhaar")
    @CheckPermission("PROFILE_DOCUMENTS_AADHAAR_CARD_DELETE_DOCUMENT")
    public CompletableFuture<ResponseEntity<AadhaarCardDetails>>deleteAadhaar(@PathVariable String employeeId){
       return  aadhaarServiceImpl.deleteAadharByEmployeeId(employeeId)
                .thenApply(ResponseEntity::ok);
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PostMapping("/{employeeId}/pan")
    @CheckPermission("PROFILE_DOCUMENTS_PAN_CARD_ADD_DOCUMENT")
    public CompletableFuture<ResponseEntity<PanDetails>> savePan(
            @PathVariable String employeeId,
            @RequestPart("panImage") MultipartFile panImage,
            @Valid @RequestPart(value = "panDetails") PanDTO panDTO) throws IOException {
        return panServiceImpl.createPan(employeeId,panImage,panDTO)
                .thenApply(pan->ResponseEntity.status(201).body(pan));
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @GetMapping("/{employeeId}/pan")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_PAN_CARD_GET_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<PanDTO>> getPanDetails(@PathVariable String employeeId) {
        return panServiceImpl.getPanDetails(employeeId)
                .thenApply(ResponseEntity::ok);
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PutMapping("/{employeeId}/pan")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_PAN_CARD_EDIT_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    //@CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD"})
    public CompletableFuture<ResponseEntity<PanDetails>> updatePanDetails(@PathVariable String employeeId,
                                                       @RequestPart(value="panImage", required = false) MultipartFile panImage,
                                                       @Valid  @RequestPart PanDTO panDTO) throws IOException {
        return panServiceImpl.UpdatePanDetails(employeeId, panImage, panDTO)
                .thenApply(ResponseEntity::ok);
    }

   // @PreAuthorize("hasAnyRole('ADMIN')")
   @DeleteMapping("/{employeeId}/pan")
   @CheckPermission("PROFILE_DOCUMENTS_PAN_CARD_DELETE_DOCUMENT")
   public CompletableFuture<ResponseEntity<PanDetails>> deletePanDetails(@PathVariable String employeeId) {
       return panServiceImpl.deletePanByEmployeeId(employeeId)
               .thenApply(ResponseEntity::ok);
   }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PostMapping("/drivinglicense/{employeeId}")
    @CheckPermission("PROFILE_DOCUMENTS_DRIVING_LICENSE_ADD_DOCUMENT")
    public CompletableFuture<ResponseEntity<DrivingLicense>> saveLicense(@PathVariable String employeeId,
                                                      @RequestPart(value="licenseImage") MultipartFile licenseImage,
                                                      @Valid @RequestPart(value="drivingLicense") DrivingLicenseDTO drivingLicenseDTO) throws IOException {
        return drivingLicenseServiceImpl.createDrivingLicense(employeeId, licenseImage, drivingLicenseDTO)
                .thenApply(savedLicense -> ResponseEntity.status(HttpStatus.CREATED).body(savedLicense));
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PutMapping("/{employeeId}/driving")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_DRIVING_LICENSE_EDIT_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
   // @CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD","MANAGER"})
    public CompletableFuture<ResponseEntity<DrivingLicense>> updateDrivingLicense(@PathVariable String employeeId,
                                                               @RequestPart(value = "licenseImage",required = false) MultipartFile licenseImage,
                                                               @Valid   @RequestPart(value="drivingLicense") DrivingLicenseDTO drivingLicenseDTO) throws IOException {
        return drivingLicenseServiceImpl.updatedrivingDetails(employeeId,licenseImage,drivingLicenseDTO)
                .thenApply(licence->ResponseEntity.status(HttpStatus.OK).body(licence));
    }

  //  @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
  @GetMapping("/{employeeId}/driving")
  @CheckPermission(
          value = "PROFILE_DOCUMENTS_DRIVING_LICENSE_GET_DOCUMENT",
          MatchParmName = "employeeId",
          MatchParmFromUrl = "employeeId",
          MatchParmForRoles = {"ROLE_EMPLOYEE"}
  )
  public CompletableFuture<ResponseEntity<DrivingLicenseDTO>> getDrivingLicense(@PathVariable String employeeId) {
      return drivingLicenseServiceImpl.getDrivingDetails(employeeId)
              .thenApply(ResponseEntity::ok);
  }

    @DeleteMapping("/{employeeId}/driving")
    @CheckPermission("PROFILE_DOCUMENTS_DRIVING_LICENSE_DELETE_DOCUMENT")
    public CompletableFuture<ResponseEntity<DrivingLicense>> deleteDriving(@PathVariable String employeeId) {
        return drivingLicenseServiceImpl.deleteByEmployeeId(employeeId)
                .thenApply(licence->ResponseEntity.status(HttpStatus.OK).body(licence));
    }


    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PostMapping("/passport/details/{employeeId}")
    @CheckPermission("PROFILE_DOCUMENTS_PASSPORT_ADD_DOCUMENT")
    public CompletableFuture<ResponseEntity<PassportDetails>> savePassportDetails(@PathVariable String employeeId,
                                                               @RequestPart("passportImage") MultipartFile passportImage,
                                                               @Valid  @RequestPart(value="passportDetails") PassportDetailsDTO passportDetailsDTO) throws IOException {
        return passportDetailsImpl.createPassport(employeeId,passportImage,passportDetailsDTO)
                .thenApply(passport->ResponseEntity.status(HttpStatus.CREATED).body(passport));
    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @GetMapping("/{employeeId}/passport")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_PASSPORT_GET_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<PassportDetailsDTO>> getPassportDetails(@PathVariable String employeeId){
        return  passportDetailsImpl.getPassportDetails(employeeId)
                .thenApply(passport->ResponseEntity.status(HttpStatus.OK).body(passport));
    }

    //@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PutMapping("/{employeeId}/passport")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_PASSPORT_EDIT_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<PassportDetails>> updatePassportDetails(@PathVariable String employeeId,
                                                                 @RequestPart(value = "passportImage",required = false) MultipartFile passportImage,
                                                                 @Valid   @RequestPart("passportDetails") PassportDetailsDTO passportDetailsDTO) throws IOException {
        return passportDetailsImpl.updatePasswordDetails(employeeId,passportImage,passportDetailsDTO)
                .thenApply(passport->ResponseEntity.status(HttpStatus.OK).body(passport));
    }

  //  @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{employeeId}/passport")
    @CheckPermission("PROFILE_DOCUMENTS_PASSPORT_DELETE_DOCUMENT")
    public CompletableFuture<ResponseEntity<PassportDetails>>deletePassport(@PathVariable String employeeId){
        return passportDetailsImpl.deleteByEmployeeId(employeeId)
                .thenApply(passport->ResponseEntity.status(HttpStatus.OK).body(passport));
    }

  //  @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PostMapping("/{employeeId}/voter")
    @CheckPermission("PROFILE_DOCUMENTS_VOTER_ID_CARD_ADD_DOCUMENT")
    public CompletableFuture<ResponseEntity<VoterDetails>>addVoter(@PathVariable String employeeId,
                                                @RequestPart("voterImage") MultipartFile voterImage,
                                                @Valid  @RequestPart(value="voterDetails") VoterDTO voterDTO)throws IOException {
        return voterIdServiceImpl.createVoter(employeeId,voterImage,voterDTO)
                .thenApply(voter->ResponseEntity.status(HttpStatus.CREATED).body(voter));

    }

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAMLEAD','MANAGER')")
    @GetMapping("/{employeeId}/voter")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_VOTER_ID_CARD_GET_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public CompletableFuture<ResponseEntity<VoterDTO>>getVoter(@PathVariable String employeeId){
        return voterIdServiceImpl.getVoterByEmployee(employeeId)
                .thenApply(voter->ResponseEntity.status(HttpStatus.OK).body(voter));
    }



   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAMLEAD','MANAGER')")
    @PutMapping("/{employeeId}/voter")
    @CheckPermission(
            value = "PROFILE_DOCUMENTS_VOTER_ID_CARD_EDIT_DOCUMENT",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
   // @CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD","MANAGER"})
    public CompletableFuture<ResponseEntity<VoterDetails>>updateVoter(@PathVariable String employeeId,
                                                   @RequestPart(value="voterImage",required=false) MultipartFile voterImage,
                                                   @Valid  @RequestPart VoterDTO voterDTO) throws IOException {
        return voterIdServiceImpl.updateVoter(employeeId,voterImage,voterDTO)
                .thenApply(voter->ResponseEntity.status(HttpStatus.OK).body(voter));
    }

   // @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{employeeId}/voter")
    @CheckPermission("PROFILE_DOCUMENTS_VOTER_ID_CARD_DELETE_DOCUMENT")
    public CompletableFuture<ResponseEntity<VoterDetails>>deleteVoter(@PathVariable String employeeId){
        return voterIdServiceImpl.deleteByEmployeeId(employeeId)
                .thenApply(voter->ResponseEntity.status(HttpStatus.OK).body(voter));
    }


    //@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD')")
    @PostMapping("/{employeeId}/previousExperience")
    @CheckPermission("PROFILE_PROFILE_PREVIOUS_EXPERIENCE_ADD_EXPERIENCE")
    public ResponseEntity<WorkExperienceDetails>createExperience(@PathVariable String employeeId,
                                                                 @Valid @RequestBody WorkExperienceDTO workExperienceDTO) {
        WorkExperienceDetails experienceDetails=  workExperienceServiceImpl.createExperenceByEmployeId(employeeId,workExperienceDTO);
        return new ResponseEntity<>(experienceDetails,HttpStatus.CREATED);
    }

    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD')")
    @PutMapping("/{employeeId}/previousExperience/{id}")
    @CheckPermission(
            value = "PROFILE_PROFILE_PREVIOUS_EXPERIENCE_EDIT_EXPERIENCE",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    //@CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD"})
    public ResponseEntity<WorkExperienceDetails>updateExperience(@PathVariable String employeeId,
                                                                 @Valid  @RequestBody WorkExperienceDTO workExperienceDTO,
                                                                 @PathVariable String id) {
        WorkExperienceDetails workExperienceDetails1=workExperienceServiceImpl.updateExperience(employeeId,workExperienceDTO,id);
        return new ResponseEntity<>(workExperienceDetails1,HttpStatus.CREATED);
    }

    //  @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD')")
    @GetMapping("/{employeeId}/previousExperience")
    @CheckPermission(
            value = "PROFILE_PROFILE_PREVIOUS_EXPERIENCE_GET_EXPERIENCE",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<List<WorkExperienceDetails>> getExperience(@PathVariable String employeeId){
        List<WorkExperienceDetails>experienceDetails=workExperienceServiceImpl.getExperience(employeeId);
        return new ResponseEntity<>(experienceDetails,HttpStatus.OK);
    }

    // @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{employeeId}/previousExperience/{id}")
    @CheckPermission("PROFILE_PROFILE_PREVIOUS_EXPERIENCE_DELETE_EXPERIENCE")
    public ResponseEntity<WorkExperienceDetails>deleteExperience(@PathVariable String employeeId,
                                                                 @PathVariable String id){
        return new ResponseEntity<>(workExperienceServiceImpl.deleteExperienceById(employeeId,id),HttpStatus.OK);
    }

    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @GetMapping("/{employeeId}/previousExperience/{id}")
    @CheckPermission(
            value = "PROFILE_PROFILE_PREVIOUS_EXPERIENCE_GET_EXPERIENCE_BY_ID",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<WorkExperienceDetails> getExperienceById(@PathVariable String employeeId,
                                                                   @PathVariable String id) {
        WorkExperienceDetails experienceDetails = workExperienceServiceImpl.getExperienceById(employeeId, id);
        return new ResponseEntity<>(experienceDetails, HttpStatus.OK);
    }


    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PostMapping("/{employeeId}/degreeDetails")
    @CheckPermission("PROFILE_PROFILE_EDUCATION_DETAILS_ADD_EDUCATION")
    public ResponseEntity<DegreeDTO>addDegree(@PathVariable String employeeId,
                                              @RequestPart("addFiles")MultipartFile addFiles,
                                              @Valid @RequestPart(value="degree") DegreeCertificates degreeCertificates ) throws IOException {
        DegreeDTO degree=degreeServiceImpl.addDegree(employeeId,addFiles,degreeCertificates);
        return new ResponseEntity<>(degree,HttpStatus.CREATED);
    }

    //  @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @GetMapping("/{employeeId}/degreeDetails")
    @CheckPermission(
            value = "PROFILE_PROFILE_EDUCATION_DETAILS_GET_EDUCATION"
    )
    public ResponseEntity<List<DegreeCertificates>>getDetails(@PathVariable String  employeeId){
        List<DegreeCertificates>degreeDTOS=degreeServiceImpl.getDegree(employeeId);
        return new ResponseEntity<>(degreeDTOS,HttpStatus.OK);
    }

    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE','TEAM_LEAD','MANAGER')")
    @PutMapping("/{employeeId}/degreeDetails/{id}")
    @CheckPermission(
            value = "PROFILE_PROFILE_EDUCATION_DETAILS_EDIT_EDUCATION",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    // @CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR","EMPLOYEE","TEAM_LEAD","MANAGER"})
    public ResponseEntity<DegreeDTO>updateDegree(@PathVariable String employeeId,
                                                 @RequestPart(value = "addFiles",required = false)MultipartFile addFiles,
                                                 @PathVariable String id,
                                                 @Valid @RequestPart(value="degree") DegreeCertificates degreeCertificates) throws IOException, UnauthorizedAccessException {
        DegreeDTO degree =degreeServiceImpl.updateDegree(employeeId,addFiles,id,degreeCertificates);
        return new ResponseEntity<>(degree,HttpStatus.OK);
    }

    @DeleteMapping("/{employeeId}/degreeDetails/{id}")
    @CheckPermission("PROFILE_PROFILE_EDUCATION_DETAILS_DELETE_EDUCATION_BY_ID")
    public ResponseEntity<DegreeCertificates>deleteDegree(@PathVariable String employeeId,
                                                          @PathVariable String id) {
        return new ResponseEntity<>(degreeServiceImpl.deleteById(employeeId,id),HttpStatus.OK);

    }
    //  @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','TEAM_LEAD','MANAGER')")
    @GetMapping("/{employeeId}/degreeDetails/{id}")
    @CheckPermission(
            value = "PROFILE_PROFILE_EDUCATION_DETAILS_GET_EDUCATION_BY_ID",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<DegreeDTO>getById(@PathVariable String employeeId,
                                            @PathVariable String id){
        DegreeDTO degreeDTO=degreeServiceImpl.getById(employeeId,id);
        return new ResponseEntity<>(degreeDTO,HttpStatus.OK);
    }
}