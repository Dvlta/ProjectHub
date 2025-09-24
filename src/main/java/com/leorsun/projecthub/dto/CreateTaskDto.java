package com.leorsun.projecthub.dto;

import com.leorsun.projecthub.model.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTaskDto {
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long assigneeId; // optional
}

