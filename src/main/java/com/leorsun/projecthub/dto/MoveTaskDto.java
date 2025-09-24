package com.leorsun.projecthub.dto;

import com.leorsun.projecthub.model.TaskStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveTaskDto {
    private TaskStatus status; // optional
    private Integer orderIndex; // optional
}

