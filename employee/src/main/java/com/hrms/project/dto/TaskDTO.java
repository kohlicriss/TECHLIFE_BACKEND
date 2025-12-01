package com.hrms.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDTO {


    private String projectId;

    private String id;


    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 255, message = "Description can be up to 255 characters")
    private String description;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    @NotBlank(message = "Assigned to is required")
    private String assignedTo;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Not Started|In Progress|Completed|On Hold)$", message = "Invalid task status")
    private String status;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(High|Medium|Low)$", message = "Priority must be High, Medium, or Low")
    private String priority;

    @NotNull(message = "Created date is required")
    @PastOrPresent(message = "Created date cannot be in the future")
    private LocalDate createdDate;

    @FutureOrPresent(message = "Completed date must be today or in the future")
    private LocalDate completedDate;

    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot be more than 5")
    private Integer rating;

    @Size(max = 200, message = "Remark can be up to 200 characters")
    private String remark;

    @Size(max = 200, message = "Completion note can be up to 200 characters")
    private String completionNote;

    private List<@NotBlank(message = "Link cannot be blank") String> relatedLinks;

    private List<@NotBlank(message = "File link cannot be blank") String> attachedFileLinks;


}
