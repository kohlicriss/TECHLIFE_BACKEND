package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AllTaskDTO {


    @NotBlank(message="Id is required")
    @Pattern(regexp="^[TEAM]\\d{4}$",message="")
    private String id;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Pending|In Progress|Completed|On Hold)$", message = "Status must be one of: Pending, In Progress, Completed, On Hold")
    private String status;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(Low|Medium|High|Critical)$", message = "Priority must be one of: Low, Medium, High, Critical")
    private String priority;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date cannot be in the past")
    private LocalDate dueDate;

    @NotBlank(message = "Project ID is required")
    private String projectId;
    private String assignedTo;
    private String createdBy;
}