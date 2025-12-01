package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {
        "teams", "projects", "department", "voterDetails",
        "panDetails", "drivingLicense", "passportDetails", "aadhaarCardDetails", "degreeCertificates", "tasks"
})
@ToString(exclude = {
        "teams", "projects", "department", "voterDetails",
        "panDetails", "drivingLicense", "passportDetails", "aadhaarCardDetails", "degreeCertificates", "tasks"
})
public class Employee {

    @Id
    private String employeeId;

    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String maritalStatus;
    private String bloodGroup;
    private String physicallyHandicapped;
    private String nationality;
    private String jobTitlePrimary;
    private String jobTitleSecondary;
    private String gender;
    private String workEmail;
    private String personalEmail;
    private String mobileNumber;
    private String workNumber;
    private String street;
    private String city;
    private String state;
    private String district;
    private String zip;
    private String country;

    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;

    private String employeeImage;

    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;
    private LocalDate dateOfLeaving;
    private String inProbation;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private String probationPolicy;
    private String workingType;
    private String timeType;
    private String contractStatus;
    private LocalDate contractStartDate;

    private List<String> skills;
    private String location;

    @Enumerated(EnumType.STRING)
    private Role role;


    @ManyToMany(mappedBy = "employees")
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeProject> employeeProjects = new ArrayList<>();


    @ManyToOne(optional = true)
    @JoinColumn(name = "department_id", nullable = true)
    @JsonBackReference
    private Department department;


    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("aadhaar-ref")
    private AadhaarCardDetails aadhaarCardDetails;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("pan")
    private PanDetails panDetails;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("driving")
    private DrivingLicense drivingLicense;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("passport")
    private PassportDetails passportDetails;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("voter")
    private VoterDetails voterDetails;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("degree")
    private List<DegreeCertificates> degreeCertificates;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("work")
    private List<WorkExperienceDetails> workExperienceDetails;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("achieve")
    private List<Achievements> achievements;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("task")
    private List<Task> tasks;

    @ManyToMany(mappedBy = "teamLeads")
    private List<Project> projects;
}