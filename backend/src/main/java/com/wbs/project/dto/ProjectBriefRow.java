package com.wbs.project.dto;

import lombok.Data;

/**
 * ProjectMapper.selectProjectBriefRowsByOwnerId 专用 row 类型(spec §4.4.1)
 * 不暴露给 Controller,Service 层 map 到 ProjectBrief
 */
@Data
public class ProjectBriefRow {
    private String id;
    private String name;
    private String status;
    private String deptCode;
    private String previousOwnerId;
    private Boolean needsHandover;
}
