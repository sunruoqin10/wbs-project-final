package com.wbs.project.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 加班记录DTO
 */
@Data
public class OvertimeDTO {

    /**
     * 加班记录ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 任务ID（可选）
     */
    private String taskId;

    /**
     * 加班日期
     */
    private LocalDate overtimeDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 加班时长（小时）
     */
    private BigDecimal hours;

    /**
     * 加班类型：weekday_workday（工作日加班）、weekday_holiday（节假日工作日加班）、weekend（周末加班）、holiday（法定节假日加班）
     */
    private String overtimeType;

    /**
     * 加班原因
     */
    private String reason;

    /**
     * 状态：pending（待审批）、approved（已批准）、rejected（已拒绝）
     */
    private String status;

    /**
     * 审批人ID
     */
    private String approverId;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 补偿类型：salary（加班费）、time_off（调休）
     */
    private String compensationType;

    // ==================== 创建/更新请求 ====================

    /**
     * 创建加班记录请求
     */
    @Data
    public static class CreateRequest {
        private String userId;
        private String projectId;
        private String taskId;
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String overtimeType;
        private String reason;
        private String compensationType;
    }

    /**
     * 更新加班记录请求
     */
    @Data
    public static class UpdateRequest {
        private String projectId;
        private String taskId;
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String overtimeType;
        private String reason;
        private String compensationType;
    }

    /**
     * 审批请求
     */
    @Data
    public static class ApproveRequest {
        private String approverId;
        private Boolean approved;
        private String rejectReason;
    }

    // ==================== 统计相关 ====================

    /**
     * 加班统计信息
     */
    @Data
    public static class OvertimeStats {
        private Integer totalRecords;
        private Integer pendingRecords;
        private Integer approvedRecords;
        private Integer rejectedRecords;
        private BigDecimal totalHours;
        private BigDecimal pendingHours;
        private BigDecimal approvedHours;
        private List<ProjectOvertimeStats> byProject;
    }

    /**
     * 用户加班统计
     */
    @Data
    public static class UserOvertimeStats {
        private String userId;
        private String userName;
        private Integer recordCount;
        private BigDecimal totalHours;
    }

    /**
     * 项目加班统计
     */
    @Data
    public static class ProjectOvertimeStats {
        private String projectId;
        private String projectName;
        private Integer recordCount;
        private BigDecimal totalHours;
    }

    /**
     * 日期加班统计
     */
    @Data
    public static class DateOvertimeStats {
        private LocalDate date;
        private BigDecimal totalHours;
    }

    /**
     * 类型加班统计
     */
    @Data
    public static class TypeOvertimeStats {
        private String overtimeType;
        private BigDecimal totalHours;
    }

    /**
     * 任务加班统计
     */
    @Data
    public static class TaskOvertimeStats {
        private String taskId;
        private String taskName;
        private String assigneeId;
        private String assigneeName;
        private Integer recordCount;
        private BigDecimal totalHours;
    }
}
