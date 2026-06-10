# 人事同步字段扩展 - 设计文档

**日期**: 2026-06-10

## 概述

为 `sys_user` 表扩展 5 个来自 MDM 中间表的字段，并将字段接入现有的人事同步流程（`POST /api/users/sync-hr`），让新增用户和老用户都能正确携带这 5 个字段。

## 背景

现有 `syncHrInsert` / `syncHrUpdate` 只同步了 `EMP_NAM → name`、`ORG_NAM → department` 两个字段。业务需要把组织架构的完整层级（部门 code、所属公司、上下级部门）以及中文姓名同步进来。

## 数据库改动

`sys_user` 表新增 5 列（管理员手动 `ALTER`，不落 SQL 文件）：

| 列名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `dept_code` | VARCHAR(64) | `''` | 部门 code（来自 `mdm_if_pa_a.ORG_CD`） |
| `sub_org_cd` | VARCHAR(64) | `''` | 父部门 code（来自 `mdm_if_pa_a.SUB_ORG_CD`） |
| `sub_org_nam` | VARCHAR(128) | `''` | 父部门名（来自 `mdm_if_pa_a.SUB_ORG_NAM`） |
| `company_cd` | VARCHAR(32) | `''` | 公司 code（来自 `mdm_if_pa_a.COMPANY_CD`，取值 `2700`/`8400`） |
| `chinese_nam` | VARCHAR(128) | `''` | 中文姓名（来自 `mdm_if_pa_a.CHINESE_NAM`） |

### ALTER 语句（管理员执行）

```sql
ALTER TABLE sys_user
  ADD COLUMN dept_code   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '部门code' AFTER department,
  ADD COLUMN sub_org_cd  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '父部门code' AFTER dept_code,
  ADD COLUMN sub_org_nam VARCHAR(128) NOT NULL DEFAULT '' COMMENT '父部门名'   AFTER sub_org_cd,
  ADD COLUMN company_cd  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '公司code(2700/8400)' AFTER sub_org_nam,
  ADD COLUMN chinese_nam VARCHAR(128) NOT NULL DEFAULT '' COMMENT '中文姓名'   AFTER company_cd;
```

> 注：`chinese_nam` 列在 `company_cd` 之后插入。已存在用户因有 `DEFAULT ''`，5 个新列会被默认填入空串，符合「NULL 用空串覆盖」的设计。

## 后端改动

### 1. `entity/User.java`

新增 5 个字段（保持与 DB 列的 snake_case ↔ camelCase 映射，命名与 MDM 源字段保持一致用 `nam` 而非 `name`）：

```java
private String deptCode;
private String subOrgCd;
private String subOrgNam;
private String companyCd;
private String chineseNam;
```

### 2. `mapper/UserMapper.xml`

#### 2.1 `BaseResultMap` 新增 5 条 `<result>` 映射
#### 2.2 `Base_Column_List` 新增 5 个列
#### 2.3 `insert` 通用插入语句新增 5 个占位符
#### 2.4 `syncHrInsert` 改为：

```sql
INSERT IGNORE INTO sys_user
  (id, name, chinese_nam, email, role, department,
   dept_code, sub_org_cd, sub_org_nam, company_cd,
   joined_at, created_at, updated_at, password)
SELECT
  p.EMP_NUM,
  p.EMP_NAM,
  IFNULL(p.CHINESE_NAM, ''),
  p.EMAIL_ADDR,
  'member',
  o.ORG_NAM,
  IFNULL(p.ORG_CD, ''),
  IFNULL(p.SUB_ORG_CD, ''),
  IFNULL(p.SUB_ORG_NAM, ''),
  IFNULL(p.COMPANY_CD, ''),
  NOW(), NOW(), NOW(),
  '1'
FROM mdm_if_pa_a p
LEFT JOIN mdm_if_or_a o ON p.ORG_CD = o.ORG_CD
WHERE p.EMP_NAM IS NOT NULL
  AND p.EMAIL_ADDR IS NOT NULL;
```

#### 2.5 `syncHrUpdate` 改为（回填老用户，**NULL 用空串覆盖**）：

```sql
UPDATE sys_user u
INNER JOIN mdm_if_pa_a p ON u.id = p.EMP_NUM COLLATE utf8mb4_unicode_ci
LEFT JOIN mdm_if_or_a o ON p.ORG_CD = o.ORG_CD
SET u.name        = p.EMP_NAM,
    u.chinese_nam = IFNULL(p.CHINESE_NAM, ''),
    u.email       = p.EMAIL_ADDR,
    u.department  = o.ORG_NAM,
    u.dept_code   = IFNULL(p.ORG_CD, ''),
    u.sub_org_cd  = IFNULL(p.SUB_ORG_CD, ''),
    u.sub_org_nam = IFNULL(p.SUB_ORG_NAM, ''),
    u.company_cd  = IFNULL(p.COMPANY_CD, ''),
    u.updated_at  = NOW()
WHERE p.EMP_NAM IS NOT NULL
  AND p.EMAIL_ADDR IS NOT NULL;
```

## 前端改动

### `types/index.ts` 的 `User` interface

```ts
export interface User {
  // ... 现有字段
  deptCode: string;
  subOrgCd: string;
  subOrgNam: string;
  companyCd: string;
  chineseNam: string;
}
```

不在 `Team.vue` / `Settings.vue` 主动加 UI（避免越界改动），仅 API 暴露。

## 行为矩阵

| 场景 | dept_code / chinese_nam / ... | 现有用户受影响 |
|---|---|---|
| 新 MDM 用户 | 同步时取 `IFNULL(p.X, '')`，非空时填入真实值 | — |
| 老用户被回填 | `syncHrUpdate` 用 `IFNULL(p.X, '')` 覆盖 | 老值会被空串覆盖（已确认） |
| 老用户无对应 MDM 记录 | 不会被 syncHrUpdate 命中 | 保持 ALTER 时默认的 `''` |
| MDM 字段为 NULL | DB 写入 `''` | — |

## 验证方式

1. **DB 层**：手动跑 `ALTER TABLE`，`DESC sys_user` 确认 5 列存在，默认值 `''`。
2. **后端编译**：`mvn -pl . test -DfailIfNoTests=false` 通过。
3. **前端类型**：`npm run build` 通过（含 `vue-tsc`）。
4. **运行时**：
   - 调用 `POST /api/users/sync-hr`，看返回 `inserted` / `updated` 数量。
   - `SELECT * FROM sys_user WHERE id = '<某工号>'` 确认 5 个新字段已写入。
   - 选一个**老用户**（同步前 5 个新字段为 `''`），再调一次同步，确认 5 个字段被 MDM 值回填。
5. **前端**：`GET /api/users/<id>` 拿到的 JSON 含 `deptCode / chineseNam` 等键。

## 风险与回滚

- **回滚**：直接 `ALTER TABLE sys_user DROP COLUMN ...` 即可（DDL 一次性），前端的 `User` interface 字段不传时会被 `JsonIgnoreProperties(ignoreUnknown = true)` 兼容，但删除 TS 字段后必须重新构建。
- **数据风险**：老用户的 5 个新字段会被空串覆盖（用户已确认接受此行为）。
- **范围限定**：本改动只影响 `User` 实体 / 同步 SQL / TS 类型，不动 Service、Controller、其它业务模块（其它模块按需后续接入这 5 个新字段）。

## 不在本次范围内

- Team / Settings 前端页面的列展示（用户未要求）
- 给这些新字段加单独的编辑接口（同步接口即唯一来源）
- 删除逻辑（离职用户不在本次处理范围）
- 现有用户按角色/部门做精细化过滤（不在本次处理范围）
