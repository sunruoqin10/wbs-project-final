-- =====================================================
-- 新增实际开始/结束日期字段
-- 用于跟踪任务的实际执行时间线，与计划日期对比
-- 日期: 2026-06-12
-- =====================================================

ALTER TABLE sys_task
  ADD COLUMN actual_start_date DATE DEFAULT NULL COMMENT '实际开始日期（状态首次变为 in-progress 时自动记录）',
  ADD COLUMN actual_end_date   DATE DEFAULT NULL COMMENT '实际结束日期（状态首次变为 done 时自动记录）';

-- 为已有数据回填：已完成的任务，实际结束日期暂用 planned end_date
-- UPDATE sys_task SET actual_end_date = end_date WHERE status = 'done';
-- 取消注释上行以回填已有已完成任务
