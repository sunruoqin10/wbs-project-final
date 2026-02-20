package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 加班记录实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvertimeRecord {
    private String id;
    private String userId;
    private String projectId;
    private String taskId;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hours;
    private String overtimeType;
    private String reason;
    private String status;
    private String approverId;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private String compensationType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段（运行时关联查询）
    private String userName;        // 用户姓名
    private String projectName;     // 项目名称
    private String taskName;        // 任务名称
    private String approverName;    // 审批人姓名
}
