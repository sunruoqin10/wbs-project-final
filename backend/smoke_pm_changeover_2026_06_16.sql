-- smoke_pm_changeover_2026_06_16.sql
-- 用法:mysql -uroot -proot db_webwbs < smoke_pm_changeover_2026_06_16.sql
-- 验证 DDL 已生效 + 数据可读(纯只读,不修改任何数据)
-- 配套:docs/superpowers/specs/2026-06-16-pm-dept-pm-changeover-design.md
--       docs/superpowers/plans/2026-06-16-pm-dept-pm-changeover-plan.md

-- 1. 字段是否存在
SELECT 'sys_project.needs_handover' AS field, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'db_webwbs' AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'needs_handover';

SELECT 'sys_project.previous_owner_id' AS field, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'db_webwbs' AND TABLE_NAME = 'sys_project' AND COLUMN_NAME = 'previous_owner_id';

-- 2. 索引
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'db_webwbs' AND TABLE_NAME = 'sys_project' AND INDEX_NAME = 'idx_needs_handover';

-- 3. 审计表
SELECT 'sys_project_handover_log exists' AS check_, COUNT(*) AS row_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'db_webwbs' AND TABLE_NAME = 'sys_project_handover_log';

-- 4. 审计表结构
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'db_webwbs' AND TABLE_NAME = 'sys_project_handover_log'
ORDER BY ORDINAL_POSITION;

-- 5. 现有数据示例(项目侧)— 需要交接的项目
SELECT id, name, owner_id, needs_handover, previous_owner_id
FROM sys_project
WHERE needs_handover = 1
LIMIT 10;

-- 6. 现有数据示例(用户侧)— PM / Dept-PM
SELECT id, name, role, company_cd,
       SUBSTRING(IFNULL(managed_project_ids, '[]'), 1, 80) AS managed_project_ids_preview,
       SUBSTRING(IFNULL(managed_dept_codes, '[]'), 1, 80) AS managed_dept_codes_preview
FROM sys_user
WHERE role IN ('project-manager', 'dept-project-manager')
  AND status = 'C'
LIMIT 10;

-- 7. 审计表样例(若有数据)
SELECT id, project_id, handover_type, from_user_id, to_user_id,
       from_dept_code, to_dept_code, operator_id, created_at
FROM sys_project_handover_log
ORDER BY id DESC
LIMIT 10;

-- 8. 整体统计
SELECT 'projects needing handover' AS metric, COUNT(*) AS cnt
FROM sys_project
WHERE needs_handover = 1 AND status NOT IN ('completed','cancelled')
UNION ALL
SELECT 'total projects', COUNT(*) FROM sys_project
UNION ALL
SELECT 'handover log total', COUNT(*) FROM sys_project_handover_log;