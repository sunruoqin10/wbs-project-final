package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RolePermission {
    private Long id;
    private String role;
    private String permissionId;
    private LocalDateTime createdAt;
}
