-- add_pm_changeover_2026_06_16.sql
-- 配套设计:docs/superpowers/specs/2026-06-16-pm-dept-pm-changeover-design.md §3
-- MySQL >= 8.0.29(ADD COLUMN IF NOT EXISTS 需要)

ALTER TABLE sys_project
  ADD COLUMN IF NOT EXISTS needs_handover TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '1=该项目的 owner 已离职/无效,需 admin 重新指派',
  ADD COLUMN IF NOT EXISTS previous_owner_id VARCHAR(16) NULL
    COMMENT '最近一次交接/冻结前的 owner,仅作审计/UI 可选展示';

CREATE INDEX IF NOT EXISTS idx_needs_handover ON sys_project (needs_handover, status);
-- 不为 previous_owner_id 建索引(YAGNI,见 spec §3.1)

CREATE TABLE IF NOT EXISTS sys_project_handover_log (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id    VARCHAR(32)     NOT NULL,
  handover_type VARCHAR(32)     NOT NULL COMMENT 'PM_HANDOVER / DEPT_PM_HANDOVER / DEPT_MERGE / RESIGN_FREEZE',
  from_user_id  VARCHAR(16)     NULL     COMMENT '原 PM/Dept-PM;DEPT_MERGE 时为原 dept-pm;RESIGN_FREEZE 时为离职人',
  to_user_id    VARCHAR(16)     NULL     COMMENT '继任者;DEPT_MERGE 时为新 dept-pm',
  from_dept_code VARCHAR(32)    NULL     COMMENT 'DEPT_MERGE 时填旧 dept_code',
  to_dept_code  VARCHAR(32)    NULL     COMMENT 'DEPT_MERGE 时填新 dept_code',
  reason        VARCHAR(255)    NULL,
  operator_id   VARCHAR(16)     NOT NULL COMMENT '操作人(admin / dept-pm / HR 同步脚本)',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_project (project_id, created_at),
  INDEX idx_from_user (from_user_id, created_at),
  INDEX idx_to_user (to_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '项目交接/冻结审计';
