package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hrms.project.configuration.TaskUpdateId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdate {

    @EmbeddedId
    private TaskUpdateId id;
    private LocalDateTime updatedDate;
    private String changes;
    private String note;
    private List<String> relatedLinks;
    private List<String> relatedFileLinks;
    private String reviewedBy;

    private String remark;

    @ManyToOne
    @MapsId("taskId")
    @JoinColumns({
            @JoinColumn(name = "task_id", referencedColumnName = "task_id"),
            @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    })
    @JsonBackReference("task-update")
    private Task task;

}
