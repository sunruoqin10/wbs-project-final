-- ===============================================================
-- 2026-06-11: sys_user 增加 status 字段（HR 同步在职状态）
-- 来源 spec: docs/superpowers/specs/2026-06-11-hr-sync-status-filter-design.md
-- 命名约定: 与 backend/add_password_column.sql 等历史文件保持一致
-- 注：sys_user 在鉴权热路径上，加 ALGORITHM=INPLACE, LOCK=NONE 提示
--   以避免 INPLACE 回退为 EXCLUSIVE 锁导致请求被卡
-- ===============================================================
ALTER TABLE sys_user
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'C'
  COMMENT '在职状态: C=在职, H=休职, T=离职'
  AFTER password,
  ALGORITHM=INPLACE, LOCK=NONE;
