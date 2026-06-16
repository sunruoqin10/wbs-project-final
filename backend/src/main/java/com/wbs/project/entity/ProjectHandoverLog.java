package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectHandoverLog {
    private Long id;
    private String projectId;
    private String handoverType;       // PM_HANDOVER / DEPT_PM_HANDOVER / DEPT_MERGE / RESIGN_FREEZE
    private String fromUserId;
    private String toUserId;
    private String fromDeptCode;
    private String toDeptCode;
    private String reason;
    private String operatorId;
    private LocalDateTime createdAt;
}