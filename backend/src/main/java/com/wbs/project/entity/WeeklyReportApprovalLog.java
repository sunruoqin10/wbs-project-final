package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyReportApprovalLog {
    private Long id;
    private String reportId;       // 对齐 WeeklyReport.id (String)
    private String approverId;
    private String approverRole;   // admin / dept-project-manager / project-manager / project-owner
    private String action;         // approve / reject
    private String comment;
    private LocalDateTime createdAt;
}
