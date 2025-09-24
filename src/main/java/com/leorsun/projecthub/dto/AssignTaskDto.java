package com.leorsun.projecthub.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTaskDto {
    private Long assigneeId; // nullable to unassign
}

