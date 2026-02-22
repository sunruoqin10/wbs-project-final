package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Permission {
    private String id;
    private String code;
    private String name;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
