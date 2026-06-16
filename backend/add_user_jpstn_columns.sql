-- ===============================================================
-- 2026-06-16: sys_user 增加 jpstn_cd / jpstn_nam(HR 同步职级)
-- 来源 spec: docs/superpowers/specs/2026-06-16-hr-sync-jpstn-design.md
-- 命名约定: 与 backend/add_user_status_column.sql 保持一致
-- 注: sys_user 在鉴权热路径上,加 ALGORITHM=INPLACE, LOCK=NONE
--   以避免 INPLACE 回退为 EXCLUSIVE 锁导致请求被卡
-- ===============================================================
ALTER TABLE sys_user
  ADD COLUMN jpstn_cd VARCHAR(50) DEFAULT NULL
    COMMENT '职级 code(来自 mdm_if_pa_a.JPSTN_CD)' AFTER company_cd,
  ADD COLUMN jpstn_nam VARCHAR(100) DEFAULT NULL
    COMMENT '职级名(来自 mdm_if_pa_a.JPSTN_NAM)' AFTER jpstn_cd,
  ALGORITHM=INPLACE, LOCK=NONE;
