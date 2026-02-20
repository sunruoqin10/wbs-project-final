package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    private String id;
    private String projectId;
    private String parentTaskId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String assigneeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 延期相关字段
    private LocalDate originalEndDate;  // 原始结束日期
    private Integer delayedDays;        // 累计延期天数
    private String delayReason;         // 延期原因
    private Integer delayCount;         // 延期次数
    private LocalDate lastDelayDate;    // 最后延期日期
    private Boolean isDelayed;          // 是否已延期（运行时计算）
}
