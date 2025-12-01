// TaskUpdateSimpleDTO.java
package com.hrms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateSimpleDTO {
    private Long updateNumber;
    private String changes;
    private LocalDateTime updatedDate;
}
