package com.wbs.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeeklyReportApprovalLogDTO {
    private Long id;
    private String reportId;
    private String approverId;
    private String approverName;     // join sys_user.name
    private String approverAvatar;   // join sys_user.avatar_url
    private String approverRole;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
