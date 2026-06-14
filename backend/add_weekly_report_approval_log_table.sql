-- backend/add_weekly_report_approval_log_table.sql
-- 周报审批操作日志表(只读审计,不替代主表 approver_id / approve_time)
-- 2026-06-14: 配合周报 4 角色数据可见 + 审批口径对齐 引入

CREATE TABLE IF NOT EXISTS sys_weekly_report_approval_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    report_id     VARCHAR(64)  NOT NULL                COMMENT '周报 ID -> sys_weekly_report.id (String,见 WeeklyReport.id)',
    approver_id   VARCHAR(8)   NOT NULL                COMMENT '审批人 user_id',
    approver_role VARCHAR(32)  NOT NULL                COMMENT '审批人当时角色快照: admin / dept-project-manager / project-manager / project-owner',
    action        VARCHAR(16)  NOT NULL                COMMENT 'approve / reject',
    comment       TEXT         NULL                    COMMENT '审批意见;reject 时必填',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_id        (report_id),
    KEY idx_approver_created (approver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='周报审批操作日志(只读审计;不替代主表 approver_id/approve_time)';