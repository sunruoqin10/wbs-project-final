-- ===============================================================
-- 2026-06-11: 角色管理改造 v2
-- 1. sys_user 增加 managed_dept_codes / managed_company_cd（部门项目负责人管辖范围）
-- 2. sys_user 增加 token_version（角色变更后强制重新登录）
-- 3. sys_project 增加 dept_code + 索引（项目归属部门）
-- 4. 新建 sys_role_change_log（角色变更审计）
-- 5. 数据迁移：旧 project-manager 角色降级为 member
-- 来源 plan: docs/superpowers/plans/1-2-cached-hedgehog.md
-- 注：sys_user / sys_project 在鉴权热路径上，加 ALGORITHM=INPLACE, LOCK=NONE
-- ===============================================================

-- ============ 1. sys_user 新增字段 ============
ALTER TABLE sys_user
  ADD COLUMN managed_dept_codes JSON DEFAULT NULL
    COMMENT '该用户作为部门项目负责人管理的部门编码列表(JSON 数组),仅 role=dept-project-manager 有效',
  ADD COLUMN managed_company_cd VARCHAR(20) DEFAULT NULL
    COMMENT '该用户作为部门项目负责人的公司编码,需与 user.company_cd 一致',
  ADD COLUMN token_version INT NOT NULL DEFAULT 0
    COMMENT 'JWT 版本号,角色/管辖范围变更时 +1,AuthInterceptor 校验不一致则 401',
  ALGORITHM=INPLACE, LOCK=NONE;

-- ============ 2. sys_project 新增字段 ============
ALTER TABLE sys_project
  ADD COLUMN dept_code VARCHAR(50) DEFAULT NULL
    COMMENT '项目归属部门编码(对应 mdm_if_or_a.ORG_CD),用于部门负责人数据范围过滤',
  ADD INDEX idx_dept_code (dept_code),
  ALGORITHM=INPLACE, LOCK=NONE;

-- ============ 3. 角色变更审计表 ============
CREATE TABLE IF NOT EXISTS sys_role_change_log (
    id                       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id                  VARCHAR(20)  NOT NULL                COMMENT '被操作用户 id',
    old_role                 VARCHAR(50)  DEFAULT NULL            COMMENT '变更前角色',
    new_role                 VARCHAR(50)  DEFAULT NULL            COMMENT '变更后角色',
    old_managed_dept_codes   JSON         DEFAULT NULL            COMMENT '变更前管辖部门编码',
    new_managed_dept_codes   JSON         DEFAULT NULL            COMMENT '变更后管辖部门编码',
    old_managed_company_cd   VARCHAR(20)  DEFAULT NULL            COMMENT '变更前管辖公司编码',
    new_managed_company_cd   VARCHAR(20)  DEFAULT NULL            COMMENT '变更后管辖公司编码',
    changed_by               VARCHAR(20)  NOT NULL                COMMENT '操作人 id',
    changed_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason                   VARCHAR(500) DEFAULT NULL            COMMENT '变更原因',
    INDEX idx_user_id (user_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与部门管辖范围变更历史';

-- ============ 4. 数据迁移：废弃旧 project-manager ============
-- 旧 project-manager 全量降级为 member，由管理员在 Team 页手工调整为 dept-project-manager
UPDATE sys_user SET role = 'member', token_version = token_version + 1 WHERE role = 'project-manager';

-- ============ 5. 数据迁移：把现有 dept-project-manager 的 managed_dept_codes 默认填为自身 dept_code ============
-- 仅当 managed_dept_codes 为空时填充，避免覆盖已设置的值
UPDATE sys_user
   SET managed_dept_codes = JSON_ARRAY(dept_code),
       managed_company_cd = company_cd
 WHERE role = 'dept-project-manager'
   AND (managed_dept_codes IS NULL OR JSON_LENGTH(managed_dept_codes) = 0)
   AND dept_code IS NOT NULL;
