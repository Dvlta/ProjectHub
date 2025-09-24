package com.leorsun.projecthub.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectDto {
    private String name;
    private String key; // optional; generated if null
    private String description;
}

