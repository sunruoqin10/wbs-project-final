-- ===============================================================
-- 2026-06-16: sys_role_change_log 增加 managed_project_ids 字段
-- 来源 spec: docs/superpowers/specs/2026-06-16-jpstn-role-default-design.md
-- 范围: HR 推断 BF→PM 与手工切 PM 时,把 managed_project_ids 一并记入审计
-- 现状: 2026-06-12 引入 PM 时漏记,本次顺手补
-- ⚠️ 必须先执行本 SQL 再启动服务,否则 MyBatis resultMap 字段映射失败
-- 要求 MySQL 5.6+(ALGORITHM=INPLACE, LOCK=NEE 语法支持)
-- ===============================================================
ALTER TABLE sys_role_change_log
  ADD COLUMN old_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更前管辖项目 ID(JSON 数组)' AFTER new_managed_company_cd,
  ADD COLUMN new_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更后管辖项目 ID(JSON 数组)' AFTER old_managed_project_ids,
  ALGORITHM=INPLACE, LOCK=NONE;