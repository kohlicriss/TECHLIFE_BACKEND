package com.hrms.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskUpdateDTO {

    private Long id;

    @NotBlank(message = "Changes description is required")
    @Size(max = 255, message = "Changes description can be up to 255 characters")
    private String changes;

    @Size(max = 200, message = "Note can be up to 200 characters")
    private String note;

    private List<@NotBlank(message = "Related link cannot be blank") String> relatedLinks;

    private List<@NotBlank(message = "Related file link cannot be blank") String> relatedFileLinks;

    @NotNull(message = "Updated date is required")
    @PastOrPresent(message = "Updated date cannot be in the future")
    private LocalDateTime updatedDate;


    private String reviewedBy;

    @Size(max = 200, message = "Remark can be up to 200 characters")
    private String remark;
}
