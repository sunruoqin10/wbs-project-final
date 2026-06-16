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
    private String createdBy;            // 项目创建者(独立于 owner / member)
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

    // === 角色管理 v2 扩展字段（2026-06-11） ===
    private String deptCode;               // 项目归属部门编码(对应 mdm_if_or_a.ORG_CD),用于部门负责人数据范围过滤

    // === PM / Dept-PM 变更交接(2026-06-16 新增) ===
    private Boolean needsHandover;          // 来自 sys_project.needs_handover(0/1 -> Boolean)
    private String previousOwnerId;         // 来自 sys_project.previous_owner_id
}
