package com.leorsun.projecthub.dto;

import com.leorsun.projecthub.model.ProjectRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteRequestDto {
    private String email;
    private ProjectRole role; // ADMIN, MEMBER, VIEWER
}

