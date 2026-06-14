package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 加班审批历史日志
 *
 * 2026-06-14: 新建 — 多角色都可审批(dept-pm / pm / owner / admin),
 * 每次审批/拒批动作记录一行,用于审计追溯。
 * 不动主表 t_overtime_record.approver_id(仍为"最终结果")。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OvertimeApprovalLog {
    private String id;
    private String overtimeId;
    private String approverId;
    /** 审批人当时的角色(admin / project-manager / dept-project-manager / project-owner) */
    private String approverRole;
    /** 操作类型: approve / reject */
    private String action;
    /** 拒绝原因(仅 reject 时) */
    private String rejectReason;
    private LocalDateTime approvedAt;
}