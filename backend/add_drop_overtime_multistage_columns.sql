-- ===============================================================
-- 2026-06-14: 清理 t_overtime_record 的多级审批死字段
-- approval_stage / first_approver_id / first_approved_at /
-- first_reject_reason / second_approver_id / second_approved_at
--
-- 这 6 个字段在 Java 实体 / Mapper XML / Service / 前端类型里
-- 完全不被引用,仅 commit 6013a64 之前的 1 条手工测试数据残留。
--
-- 新建的 t_overtime_approval_log(commit 6013a64)已经替代其审计
-- 职责,本表删字段不再丢失真实业务审计信息。
--
-- 主表 approver_id / approved_at / reject_reason 仍保留
-- 作为"最终审批人",不受影响。
-- ===============================================================

ALTER TABLE t_overtime_record
    DROP COLUMN approval_stage,
    DROP COLUMN first_approver_id,
    DROP COLUMN first_approved_at,
    DROP COLUMN first_reject_reason,
    DROP COLUMN second_approver_id,
    DROP COLUMN second_approved_at;