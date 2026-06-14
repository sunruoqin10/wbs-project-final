-- ===============================================================
-- 2026-06-14: 新建 t_overtime_approval_log (加班审批日志)
-- 用于审计每次审批/拒批动作,保留审批人 / 角色 / 时间 / 原因
-- 多角色都可审批(dept-pm / pm / owner / admin),需要追溯具体审批人
-- 来源 plan: docs/superpowers/plans/velvety-painting-frost.md
-- ===============================================================

CREATE TABLE IF NOT EXISTS t_overtime_approval_log (
    id              VARCHAR(50)  NOT NULL PRIMARY KEY              COMMENT '日志ID(oal+uuid8)',
    overtime_id     VARCHAR(50)  NOT NULL                          COMMENT '加班记录ID(t_overtime_record.id)',
    approver_id     VARCHAR(20)  NOT NULL                          COMMENT '审批人ID(sys_user.id)',
    approver_role   VARCHAR(50)  DEFAULT NULL                      COMMENT '审批人当时的角色(admin/project-manager/dept-project-manager/project-owner)',
    action          VARCHAR(20)  NOT NULL                          COMMENT '操作: approve / reject',
    reject_reason   VARCHAR(500) DEFAULT NULL                      COMMENT '拒绝原因(仅 reject 时)',
    approved_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    INDEX idx_overtime_id (overtime_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approved_at (approved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班审批历史日志(审计追溯)';