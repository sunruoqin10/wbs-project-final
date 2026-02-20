package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    private String id;
    private String name;
    private String description;
    private String status;
    private String priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private String ownerId;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 前端扩展字段，不直接映射到数据库，通过 memberIds 字段名传递
    private List<String> memberIds;

    // 延期相关字段（运行时计算）
    private Integer delayedTasks;          // 延期任务数
    private Integer totalDelayedDays;      // 总延期天数
    private Boolean isDelayed;             // 是否有延期任务
    private Integer estimatedHours;        // 预估工时
}
