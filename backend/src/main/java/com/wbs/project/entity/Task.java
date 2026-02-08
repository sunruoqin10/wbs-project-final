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
}
