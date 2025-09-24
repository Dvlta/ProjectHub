package com.leorsun.projecthub.dto;

import com.leorsun.projecthub.model.TaskPriority;
import com.leorsun.projecthub.model.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskDto {
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long assigneeId; // nullable to unassign
}

