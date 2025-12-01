package com.hrms.project.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hrms.project.configuration.TaskId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")

public class Task {

    @EmbeddedId
    private TaskId id;

    private String title;
    private String description;
    private String createdBy;
    private String assignedTo;
    private String status;
    private String priority;
    private LocalDate  createdDate;
    private LocalDate completedDate;
    private LocalDate  dueDate;
    private Integer rating;
    private String remark;
    private String completionNote;
    private List<String> relatedLinks;
    private List<String> attachedFileLinks;

    @ManyToOne
    @JoinColumn(name="employee_id")
     @JsonBackReference("task")
    private Employee employee;

    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name = "project_id", referencedColumnName = "projectId")
    private Project project;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("task-update")
    private List<TaskUpdate> updateHistory;

    @ManyToOne
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;


}
