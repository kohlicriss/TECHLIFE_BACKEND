package com.hrms.project.configuration;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateId implements Serializable {

    private TaskId taskId;

    private Long updateNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskUpdateId that)) return false;
        return Objects.equals(taskId, that.taskId) && Objects.equals(updateNumber, that.updateNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, updateNumber);
    }
}
