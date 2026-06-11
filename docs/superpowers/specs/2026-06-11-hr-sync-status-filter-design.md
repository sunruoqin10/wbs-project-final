# 人事同步 - 在职状态过滤与离职标记 - 设计文档

**日期**: 2026-06-11

## 概述

扩展现有的人事同步流程（`POST /api/users/sync-hr`），让 `sys_user` 反映 `mdm_if_pa_a.ACT_CLSS_CD` 字段的分类：

- **C** = 在职（active）
- **H** = 休职（on leave）
- **T** = 离职（resigned）

具体行为：
1. **同步范围**：`mdm_if_pa_a` 中 `ACT_CLSS_CD IN ('C','H')` 的记录才被同步进 `sys_user`。
2. **离职标记**：`mdm_if_pa_a` 中 `ACT_CLSS_CD = 'T'` 或在 `mdm_if_pa_a` 中**完全找不到对应 EMP_NUM** 的 `sys_user` 用户，标记 `status = 'T'`。
3. **复活**：当一个 `status = 'T'` 的用户在 `mdm_if_pa_a` 中重新出现为 `C` 或 `H`，下一次同步会将其 `status` 改回 `'C'` 或 `'H'`。
4. **显示过滤**：`GET /api/users` 接口只返回 `status != 'T'` 的用户，离职用户从前端 Team 视图、成员选择器等所有用户列表中消失。

## 背景

现有 `syncHrInsert` / `syncHrUpdate` 同步了 9 个字段但**完全忽略 `ACT_CLSS_CD`**——所有 `EMP_NAM` 和 `EMAIL_ADDR` 非空的记录都会被插入或更新。`sys_user` 表没有在职状态字段，离职用户需要手动删除（`DELETE /api/users/{id}`）。

业务需求：HR 系统的"在职/休职/离职"分类是员工状态的唯一权威来源，应用层必须按此分类显示员工。

## 数据库改动

`sys_user` 表新增 1 列：

| 列名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `status` | VARCHAR(20) | `'C'` | 在职状态: C=在职, H=休职, T=离职 |

### DDL 语句（落到 `backend/add_user_status_column.sql`）

> **与上一份 spec 的差异说明**：2026-06-10 的 spec（5 个 HR 字段扩展）显式要求"管理员手动 `ALTER`，不落 SQL 文件"。本次破例落 SQL 文件，理由：(a) 文件命名与已有的 `add_password_column.sql` / `add_delay_notification_table.sql` 一致；(b) 让变更可复现、可审计；(c) 上一份 spec 当初的"不落文件"决定是出于保守，本 spec 经团队评估后改为落文件。后续 spec 若有类似 DDL，默认沿用本次"落文件"惯例。

```sql
ALTER TABLE sys_user
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'C'
  COMMENT '在职状态: C=在职, H=休职, T=离职'
  AFTER password;
```

> 已有用户由 `DEFAULT 'C'` 自动填充为「在职」，对存量数据无影响。

## 后端改动

### 1. `entity/User.java`

新增字段：

```java
private String status; // 在职状态: C=在职, H=休职, T=离职
```

放在 `password` 字段之后，与 DB 列顺序保持一致。

### 2. `mapper/UserMapper.xml`

#### 2.1 `BaseResultMap` 新增 1 条 `<result>` 映射
#### 2.2 `Base_Column_List` 新增 `status` 列

#### 2.3 `syncHrInsert` 增加过滤 + status 赋值

```sql
INSERT IGNORE INTO sys_user
  (id, name, chinese_nam, email, role, department,
   dept_code, sub_org_cd, sub_org_nam, company_cd,
   password, status)
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
  '1',
  IFNULL(p.ACT_CLSS_CD, 'C')          -- ★ 新增：来自 MDM 分类
FROM mdm_if_pa_a p
LEFT JOIN mdm_if_or_a o  ON p.ORG_CD    = o.ORG_CD
LEFT JOIN mdm_if_or_a o2 ON p.SUB_ORG_CD = o2.ORG_CD
WHERE p.EMP_NAM IS NOT NULL
  AND p.EMAIL_ADDR IS NOT NULL
  AND p.ACT_CLSS_CD IN ('C','H');      -- ★ 新增：只同步 C/H
```

#### 2.4 `syncHrUpdate` 增加过滤 + status 赋值（**含 T → C/H 复活**）

```sql
UPDATE sys_user u
INNER JOIN mdm_if_pa_a p ON u.id = p.EMP_NUM COLLATE utf8mb4_unicode_ci
LEFT  JOIN mdm_if_or_a o  ON p.ORG_CD     = o.ORG_CD
LEFT  JOIN mdm_if_or_a o2 ON p.SUB_ORG_CD  = o2.ORG_CD
SET u.name        = p.EMP_NAM,
    u.chinese_nam = IFNULL(p.CHINESE_NAM, ''),
    u.email       = p.EMAIL_ADDR,
    u.department  = o.ORG_NAM,
    u.dept_code   = IFNULL(p.ORG_CD, ''),
    u.sub_org_cd  = IFNULL(p.SUB_ORG_CD, ''),
    u.sub_org_nam = IFNULL(p.SUB_ORG_NAM, ''),
    u.company_cd  = IFNULL(p.COMPANY_CD, ''),
    u.status      = IFNULL(p.ACT_CLSS_CD, 'C'),  -- ★ 新增：含 T→C/H 复活
    u.updated_at  = NOW()
WHERE p.EMP_NAM IS NOT NULL
  AND p.EMAIL_ADDR IS NOT NULL
  AND p.ACT_CLSS_CD IN ('C','H');      -- ★ 新增：只处理 C/H
```

#### 2.5 `syncHrMarkResigned` — 新增 mapper SQL

```sql
UPDATE sys_user u
LEFT JOIN mdm_if_pa_a p ON u.id = p.EMP_NUM COLLATE utf8mb4_unicode_ci
SET u.status = 'T',
    u.updated_at = NOW()
WHERE u.status != 'T'
  AND (p.EMP_NUM IS NULL OR p.ACT_CLSS_CD = 'T');
```

- `LEFT JOIN` 让 MDM 中缺失的记录也能命中（`p.EMP_NUM IS NULL`）
- `WHERE u.status != 'T'` 避免无意义的更新

#### 2.6 所有"列表形"查询统一增加离职过滤

`status != 'T'` 过滤需要作用到**所有**返回多行的 `UserMapper` 查询，否则 PM chooser、delay-notification scheduler、批量查找等会继续出现离职用户。需要修改的 SQL 段：

| Mapper 方法 | 用途 | 是否 JOIN | 过滤子句 |
|---|---|---|---|
| `selectAll` | `GET /api/users` 无关键字 | 否 | `WHERE u.status != 'T'` |
| `searchUsers` | `GET /api/users?keyword=...` 分页 | 否 | `AND u.status != 'T'` |
| `countSearchUsers` | `searchUsers` 分页总数 | 否 | `AND status != 'T'` ⚠️ 无 `u.` 别名 |
| `selectByRole` | `UserService.getManagers()`（delay-notification scheduler、PM chooser） | 否 | `AND u.status != 'T'` |
| `selectByDepartment` | 部门维度用户查询（无 controller 直接调用） | 否 | `AND u.status != 'T'` |
| `selectByIds` | `POST /api/users/batch`（文档/任务/项目反查用户名） | 否 | `AND u.status != 'T'` |

> ⚠️ **`countSearchUsers` 没有 `u.` 别名**（无 JOIN），需要写 `AND status != 'T'`，不能照搬 `AND u.status != 'T'`。其余查询虽然当前没有 JOIN，但保持 `u.` 前缀以与未来可能的 JOIN 兼容。

**不在过滤范围**的查询（明确**不**加过滤）：

- `selectById`、`selectByEmail`、`selectByName` —— 用于登录 / 单点查找。如果 T 用户尝试登录，**当前仍然可以登录**（详见"行为矩阵"一节）。这是本次范围外决策，后续如需阻断登录，由独立 PR 处理。

#### 2.7 `countAdminNotInMdm` — admin 误标预警查询

```sql
SELECT COUNT(*) FROM sys_user u
WHERE u.role = 'admin'
  AND u.status != 'T'                              -- 已离职的不算
  AND NOT EXISTS (
    SELECT 1 FROM mdm_if_pa_a p
    WHERE p.EMP_NUM = u.id COLLATE utf8mb4_unicode_ci
  );
```

> 排除已经离职的 admin（避免历史 `u10000001` 之类反复触发）；只统计"当前在岗但 MDM 中缺失"的 admin。`EMP_NUM` 走 `COLLATE utf8mb4_unicode_ci` 与 `syncHrUpdate` 保持一致。

### 3. `mapper/UserMapper.java`

新增两个方法签名（与现有 `int syncHrInsert();` 风格保持一致）：

```java
int syncHrMarkResigned();
int countAdminNotInMdm();
```

### 4. `service/UserService.java`

修改 `syncHrData()`：

```java
@Transactional
public java.util.Map<String, Integer> syncHrData() {
    int inserted = userMapper.syncHrInsert();
    int updated = userMapper.syncHrUpdate();
    int resigned = userMapper.syncHrMarkResigned();
    java.util.Map<String, Integer> result = new java.util.HashMap<>();
    result.put("inserted", inserted);
    result.put("updated", updated);
    result.put("resigned", resigned);
    return result;
}
```

> **保持 `Map<String, Integer>` 返回类型**——三个数字字段全是 `Integer`，无需将签名放宽为 `Map<String, Object>`，Controller 侧不需要任何修改。

#### 4.1 同步前安全检查：admin 误标预警

在 `syncHrData` 入口处、**任何写操作之前**，新增一次轻量级 `SELECT`：

```java
int adminNotInMdm = userMapper.countAdminNotInMdm(); // 见 §2.7
if (adminNotInMdm > 0) {
    throw new com.wbs.project.exception.BusinessException(
        /* code */ 409,
        "检测到 " + adminNotInMdm + " 名管理员不在 MDM 中，继续同步将被标记为离职。请先在 MDM 中维护管理员记录，或手动设置其 status='C' 后再重试。"
    );
}
```

> **设计取舍**：选择**阻断 + 抛 `BusinessException`**（不是仅警告）而不是"警告但继续执行"，理由：
> - `POST /api/users/sync-hr` 是低频管理员操作（每天/每周手动触发一次），不构成性能影响。
> - 阻断可让操作员在 Settings → 人事同步 看到清晰的错误消息（`GlobalExceptionHandler` 统一映射为 `Result.error`）。
> - "警告但继续"会让前端 toast 难以传达风险——管理员可能忽略一个数字，而抛错会让同步**完全不执行**，迫使人工介入。
> - 如需"警告但继续"模式，可在后续版本加一个 `?force=true` 查询参数绕过本检查（本次不做）。

### 5. `controller/UserController.java`

**不需要任何改动**。`Result<Map<String, Integer>>` 已经能容纳 `inserted` / `updated` / `resigned` 三个数字字段。

## 前端改动

### 1. `types/index.ts` — `User` interface

在 `chineseNam` 之后增加：

```ts
status?: string; // C=在职, H=休职, T=离职（后端已过滤 T，前端不展示）
```

### 2. `services/api.ts` — `syncHrUsers()`

返回类型从 `Map` 数字字段扩展为含 `resigned`：

```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number }> { ... }
```

### 3. `views/Settings.vue` — `handleSyncHr`

将 toast 文案补上 `resigned`：

```ts
message: t('settings.hrSync.syncSuccessWithCount', {
  inserted: result.inserted,
  updated: result.updated,
  resigned: result.resigned,
}),
```

### 4. `i18n/locales/zh.ts`（及 ko.ts）

调整 `settings.hrSync.syncSuccessWithCount` 文案：

| key | zh | ko |
|---|---|---|
| `syncSuccessWithCount` | 同步完成：新增 {inserted} 人，更新 {updated} 人，标记离职 {resigned} 人 | 동기 완료: {inserted}명 추가, {updated}명 업데이트, {resigned}명 퇴직 처리 |

> **没有 `en.ts` 翻译文件**——`frontend/src/i18n/locales/` 下只有 `zh.ts` / `ko.ts` / `index.ts`，CLAUDE.md 描述的"zh / ko / en"i18n 中 `en` 实际缺失（这是项目现存的非本次 spec 范围的状态）。本次只动 `zh.ts` + `ko.ts` 两个文件。

### 5. 不改动的部分（**重要**）

- **`Team.vue` 的 `filteredUsers` computed** — 不加离职过滤。后端 `GET /api/users` 已过滤 `status != 'T'`，前端无需感知，也不应再加一道过滤（避免重复逻辑、未来维护成本）。
- **`useUserStore.parseUserData`** — 不改。后端保证返回的 User 必带 `status` 字段（DB `NOT NULL` + DEFAULT 'C'），前端无 fallback 需求。
- **成员选择器 (`MemberSelector`、`ProjectModal` 等)** — 不改。它们读 `userStore.users`，而 store 已被后端过滤。

## 行为矩阵

| 同步前 sys_user 状态 | MDM 中该 EMP_NUM 的状态 | 同步后 sys_user 状态 | 同步后是否对前端可见 |
|---|---|---|---|
| 无此用户（EMP_NUM） | `C` | 新建，`status='C'` | ✅ 可见 |
| 无此用户（EMP_NUM） | `H` | 新建，`status='H'` | ✅ 可见 |
| 无此用户（EMP_NUM） | `T` | **不创建** | — |
| 无此用户（EMP_NUM） | 不在 MDM | **不创建** | — |
| `status='C'` | `C` | 维持 `C`，其他字段回填 | ✅ 可见 |
| `status='C'` | `H` | 改为 `H` | ✅ 可见 |
| `status='C'` | `T` | 改为 `T` | ❌ 隐藏 |
| `status='C'` | 不在 MDM | 改为 `T` | ❌ 隐藏 |
| `status='H'` | `C` | 改为 `C` | ✅ 可见 |
| `status='H'` | `H` | 维持 `H` | ✅ 可见 |
| `status='H'` | `T` | 改为 `T` | ❌ 隐藏 |
| `status='H'` | 不在 MDM | 改为 `T` | ❌ 隐藏 |
| `status='T'` | `C` | **复活为 `C`** | ✅ 可见 |
| `status='T'` | `H` | **复活为 `H`** | ✅ 可见 |
| `status='T'` | `T` | 维持 `T` | ❌ 隐藏 |
| `status='T'` | 不在 MDM | 维持 `T` | ❌ 隐藏 |
| `status='C'`（管理员） | 不在 MDM | **同步被阻断**（见 §4.1） | — |

> ✅ **admin 误标已被阻断**：上一版 spec 把管理员误标列为"已知限制"，本次通过 §4.1 的 `countAdminNotInMdm` 预检**在写操作之前**抛 `BusinessException(409, ...)`。只要有任何"在岗但 MDM 缺失"的 admin，同步**根本不执行**，操作员必须先在 MDM 维护或手动 `UPDATE sys_user SET status = 'C' WHERE id = '<admin_id>'`，然后重试。
> 配套的"管理员保护名单"（自动跳过 role='admin' 的标记）不在本次范围，但当前阻断策略已消除最坏情况。

## 验证方式

1. **DDL 验证**：
   - 执行 `backend/add_user_status_column.sql`。
   - `DESC sys_user` 确认 `status` 列存在，默认值 `'C'`。
   - `SELECT COUNT(*) FROM sys_user WHERE status = 'C'` 应当等于当前用户总数。
2. **后端编译**：`mvn -pl . test -DfailIfNoTests=false` 通过。
3. **前端类型**：`npm run build` 通过（含 `vue-tsc`）。
4. **运行时（用户手动测）**：
   - 场景 A：MDM 中新员工 `EMP_NUM=X`，`ACT_CLSS_CD='C'` → 调 `POST /api/users/sync-hr`，返回 `inserted >= 1`，`GET /api/users` 中可见该员工。
   - 场景 B：把 `X` 在 MDM 改为 `'T'` → 调同步，返回 `resigned >= 1`，`GET /api/users` 中不可见。
   - 场景 C：把 `X` 改回 `'C'` → 调同步，`status` 回 `'C'`，前端再次可见。
   - 场景 D：sys_user 中存在但 MDM 中完全没有 EMP_NUM（非 admin）→ 调同步，被标记为 `'T'`。
   - 场景 E：sys_user 中存在 admin 但 MDM 缺失 → 调同步，**应返回 409 错误且 `inserted/updated/resigned` 全为 0**（阻断）。
   - 场景 F：登录被标 T 的非 admin 用户 → 应当**仍能登录**（本次范围外，T 用户不被阻断登录，仅不展示）。

## 部署 Runbook

按以下顺序在生产执行：

```sql
-- 1. 备份
CREATE TABLE sys_user_backup_20260611 AS SELECT * FROM sys_user;

-- 2. 应用 DDL
SOURCE backend/add_user_status_column.sql;

-- 3. 校验默认填充
SELECT COUNT(*) AS total,
       SUM(status = 'C') AS active,
       SUM(status = 'H') AS onleave,
       SUM(status = 'T') AS resigned
FROM sys_user;
-- 预期：active == total, onleave == 0, resigned == 0（新建列全为默认 'C'）
```

部署代码后，**第一次手动同步前**先跑诊断：

```sql
-- 4. 同步前：admin 误标风险扫描
SELECT id, role, status FROM sys_user
WHERE role = 'admin' AND status != 'T'
  AND id NOT IN (SELECT EMP_NUM FROM mdm_if_pa_a);
-- 若返回 0 行：安全可同步
-- 若返回 N 行：需在 MDM 维护或手动 UPDATE 后再同步（否则 §4.1 会阻断）
```

同步完成后再跑一次校验：

```sql
-- 5. 同步后：状态分布
SELECT status, role, COUNT(*) AS cnt FROM sys_user
GROUP BY status, role ORDER BY status, role;
-- 预期：T 行只应来自 (a) MDM 标记 T 的用户 (b) 同步前在 sys_user 但 MDM 缺失的非 admin 用户
```

## 风险与回滚

- **回滚 DDL**：`ALTER TABLE sys_user DROP COLUMN status;`（单语句）。
- **回滚代码**：`git revert` 即可。本次改动横跨 10 个文件：DDL SQL、entity、mapper xml、mapper interface、service、frontend types、frontend services、Settings.vue、i18n（zh + ko 共 2 个），但 entity/i18n 改动很小、mapper 改动集中。
- **数据风险**：
  - 已有用户 status 默认填 `'C'`，无破坏。
  - 下一次同步后，未在 MDM 中找到的非 admin 用户会被标 `'T'`，符合预期。
  - admin 用户会被 §4.1 预检阻断，不会被误标。
  - **强烈建议在生产执行同步前备份 `sys_user` 表**（见部署 Runbook 第 1 步）。
- **范围限定**：本改动只影响用户同步和用户列表 API；不动项目、任务、工时、周报、组织架构等模块。
- **跨文件落地顺序**：本次改动横跨 10 个文件（DDL + 后端 4 个 + 前端 5 个），**必须** 1 个 PR 落地。如果分批合并，任何中间态都会让"前端拿到 status 字段但 mapper 不存在"或"DDL 未执行但 service 已调用"出现 schema 不一致。建议在 PR 描述里引用本 spec 的 §2.6 + §4.1 作为 reviewer checklist。

## 不在本次范围内

- 离职用户的 `joinedAt` / `leftAt` 时间戳记录
- 离职用户在历史项目/任务/周报中的"已离职"角标显示
- 管理员/PM 的白名单保护机制（**改由 §4.1 预检阻断，不在写入时跳过**）
- 阻断 T 用户登录（**T 用户本次保留登录能力**，仅不展示）
- 同步频率优化（仍为手动触发）
- Team / Settings 前端页面的 status 列展示
- 删除已离职 N 天以上的用户（数据归档）
- `?force=true` 查询参数绕过 §4.1 预检（**当前必须人工修复，不支持绕过**）
