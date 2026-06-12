package com.wbs.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * PM 项目分配请求 DTO(2026-06-12 新增)
 * PUT /api/users/{id}/managed-projects
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagedProjectsRequest {
    private List<String> managedProjectIds;
}
