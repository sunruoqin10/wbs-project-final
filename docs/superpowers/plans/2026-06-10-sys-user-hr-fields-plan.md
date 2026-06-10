# sys_user HR 字段扩展 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `sys_user` 表扩展 5 个来自 MDM 中间表的字段（`dept_code` / `sub_org_cd` / `sub_org_nam` / `company_cd` / `chinese_nam`），并接入现有的人事同步流程（`POST /api/users/sync-hr`），让新增用户和回填老用户都能正确携带这 5 个字段，同时把字段暴露到前端 `User` interface。

**Architecture:** 沿用项目现有的「Controller → Service → MyBatis Mapper」分层结构，只改动 `User` 实体、Mapper XML 和前端 TS 类型；不动 Service、Controller 业务逻辑（同步入口 `POST /api/users/sync-hr` 行为不变）。同步 SQL 的 `INSERT IGNORE` 和 `UPDATE` 各自加 5 列；NULL 一律写空串（已与用户确认接受空串覆盖老值的语义）。

**Tech Stack:** Spring Boot 3.2.0 + MyBatis + MySQL 8 / Vue 3 + TypeScript + Vite

**参考文档**：[spec: 2026-06-10-sys-user-hr-fields-design.md](../specs/2026-06-10-sys-user-hr-fields-design.md)

---

## 文件结构

### 修改文件
| 文件 | 改动 |
|---|---|
| `backend/src/main/java/com/wbs/project/entity/User.java` | 新增 5 个字段（`deptCode` / `subOrgCd` / `subOrgNam` / `companyCd` / `chineseNam`） |
| `backend/src/main/resources/mapper/UserMapper.xml` | `resultMap` + `Base_Column_List` + `insert` + `syncHrInsert` + `syncHrUpdate` 全部加 5 列 |
| `frontend/src/types/index.ts` | `User` interface 新增 5 个字段（`string`，全部 optional 以保证向后兼容） |

### 手动执行（用户跑，不在 plan 的代码任务内）
| 步骤 | 操作 |
|---|---|
| ALTER TABLE | 用户在 MySQL 客户端跑 ALTER 语句（见 Task 0） |

---

## 前置说明

- **后端测试套件为空**（CLAUDE.md 已确认）。本计划的「测试」步骤用 `mvn -pl . test -DfailIfNoTests=false` 确认编译通过，加上 `mvn clean install -DskipTests` 跑实际编译链路。
- **前端无测试 runner**。验证用 `npm run build`（含 `vue-tsc` 类型检查）。
- **运行时验证**靠调 `POST /api/users/sync-hr` 接口 + 查 `sys_user` 表。
- **TDD 调整**：因无现成测试框架，本计划用「编译/类型检查 + 运行时 smoke test」替代单元测试。每完成一个文件改动后立即编译，确保错误定位精准。

---

## Task 0: 手动 ALTER sys_user 表

**Files:** 无（手动执行）

**执行者：用户**（按 spec 中已确认的方案「我直接 ALTER（不落文件）」）

- [ ] **Step 1: 在 MySQL 客户端执行 ALTER**

```sql
USE db_webwbs;

ALTER TABLE sys_user
  ADD COLUMN dept_code   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '部门code' AFTER department,
  ADD COLUMN sub_org_cd  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '父部门code' AFTER dept_code,
  ADD COLUMN sub_org_nam VARCHAR(128) NOT NULL DEFAULT '' COMMENT '父部门名'   AFTER sub_org_cd,
  ADD COLUMN company_cd  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '公司code(2700/8400)' AFTER sub_org_nam,
  ADD COLUMN chinese_nam VARCHAR(128) NOT NULL DEFAULT '' COMMENT '中文姓名'   AFTER company_cd;
```

- [ ] **Step 2: 验证表结构**

```sql
DESC sys_user;
```

预期：能看到 `dept_code / sub_org_cd / sub_org_nam / company_cd / chinese_nam` 5 列，`Default` 都是 `''`，`Null` 都是 `NO`，`Comment` 含中文说明。

- [ ] **Step 3: 验证老用户默认值**

```sql
SELECT id, name, dept_code, chinese_nam FROM sys_user LIMIT 5;
```

预期：所有行的 `dept_code / chinese_nam` 都是空串 `''`，无 `NULL`。

---

## Task 1: 后端 User 实体新增 5 字段

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/entity/User.java`

- [ ] **Step 1: 在 User.java 中添加 5 个字段**

完整修改后的 `User.java`：

```java
package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private String role;
    private String department;
    private String skills; // JSON字符串存储技能列表
    private String password; // 明文密码
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // === HR 同步扩展字段（2026-06-10 来自 MDM 中间表） ===
    private String deptCode;     // 部门 code（来自 mdm_if_pa_a.ORG_CD）
    private String subOrgCd;     // 父部门 code（来自 mdm_if_pa_a.SUB_ORG_CD）
    private String subOrgNam;    // 父部门名（来自 mdm_if_pa_a.SUB_ORG_NAM）
    private String companyCd;    // 公司 code（来自 mdm_if_pa_a.COMPANY_CD，2700/8400）
    private String chineseNam;   // 中文姓名（来自 mdm_if_pa_a.CHINESE_NAM）
}
```

- [ ] **Step 2: 编译后端确认无报错**

```bash
cd backend
mvn -pl . compile
```

预期：`BUILD SUCCESS`。如果 `User` 在其它地方被使用，编译会自动校验 getter/setter（Lombok `@Data` 自动生成）有无冲突。

---

## Task 2: MyBatis Mapper 改动（5 处）

**Files:**
- Modify: `backend/src/main/resources/mapper/UserMapper.xml`

> 5 处改动按下面顺序执行：resultMap → Base_Column_List → insert → syncHrInsert → syncHrUpdate。每步都立即编译，能精准定位错误。

- [ ] **Step 1: 修改 `resultMap`，新增 5 条映射**

在 `BaseResultMap` 内的 `</resultMap>` 之前增加：

```xml
        <result column="dept_code"   property="deptCode"/>
        <result column="sub_org_cd"  property="subOrgCd"/>
        <result column="sub_org_nam" property="subOrgNam"/>
        <result column="company_cd"  property="companyCd"/>
        <result column="chinese_nam" property="chineseNam"/>
```

完整 resultMap 应为：

```xml
    <resultMap id="BaseResultMap" type="com.wbs.project.entity.User">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="email" property="email"/>
        <result column="avatar" property="avatar"/>
        <result column="role" property="role"/>
        <result column="department" property="department"/>
        <result column="dept_code"   property="deptCode"/>
        <result column="sub_org_cd"  property="subOrgCd"/>
        <result column="sub_org_nam" property="subOrgNam"/>
        <result column="company_cd"  property="companyCd"/>
        <result column="chinese_nam" property="chineseNam"/>
        <result column="skills" property="skills"/>
        <result column="password" property="password"/>
        <result column="joined_at" property="joinedAt"/>
        <result column="created_at" property="createdAt"/>
        <result column="updated_at" property="updatedAt"/>
    </resultMap>
```

- [ ] **Step 2: 修改 `Base_Column_List`**

把现有 `Base_Column_List` 改为：

```xml
    <sql id="Base_Column_List">
        id, name, email, avatar, role, department,
        dept_code, sub_org_cd, sub_org_nam, company_cd, chinese_nam,
        skills, password, joined_at, created_at, updated_at
    </sql>
```

- [ ] **Step 3: 编译确认 1+2 无误**

```bash
cd backend
mvn -pl . compile
```

预期：`BUILD SUCCESS`。任意 select 查询现在会自动带回 5 个新字段。

- [ ] **Step 4: 修改通用 `insert` 语句，加 5 个占位符**

把现有 `<insert id="insert">` 改为：

```xml
    <insert id="insert" parameterType="com.wbs.project.entity.User">
        INSERT INTO sys_user
          (id, name, email, avatar, role, department,
           dept_code, sub_org_cd, sub_org_nam, company_cd, chinese_nam,
           skills, password, joined_at, created_at, updated_at)
        VALUES
          (#{id}, #{name}, #{email}, #{avatar}, #{role}, #{department},
           #{deptCode}, #{subOrgCd}, #{subOrgNam}, #{companyCd}, #{chineseNam},
           #{skills}, #{password}, #{joinedAt}, #{createdAt}, #{updatedAt})
    </insert>
```

- [ ] **Step 5: 修改 `syncHrInsert`**

把现有 `syncHrInsert` 整段替换为：

```xml
    <!-- 人事数据同步：插入新用户 -->
    <insert id="syncHrInsert">
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
            NOW(),
            NOW(),
            NOW(),
            '1'
        FROM mdm_if_pa_a p
        LEFT JOIN mdm_if_or_a o ON p.ORG_CD = o.ORG_CD
        WHERE p.EMP_NAM IS NOT NULL
          AND p.EMAIL_ADDR IS NOT NULL
    </insert>
```

- [ ] **Step 6: 修改 `syncHrUpdate`**

把现有 `syncHrUpdate` 整段替换为：

```xml
    <!-- 人事数据同步：更新已有用户（回填 5 个新字段，NULL 用空串覆盖） -->
    <update id="syncHrUpdate">
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
          AND p.EMAIL_ADDR IS NOT NULL
    </update>
```

- [ ] **Step 7: 完整编译验证后端**

```bash
cd backend
mvn clean install -DskipTests
```

预期：`BUILD SUCCESS`。任何 MyBatis 绑定错误（resultMap column 不匹配、占位符拼写错）都会在这一步暴露。

---

## Task 3: 前端 User interface 新增 5 字段

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1: 在 `User` interface 中加 5 个可选字段**

把现有：

```ts
export interface User {
  id: string;
  name: string;
  email: string;
  avatar: string;
  role: 'admin' | 'project-manager' | 'member' | 'viewer';
  department: string;
  skills: string[];
  password?: string;
  joinedAt: string;
}
```

改为：

```ts
export interface User {
  id: string;
  name: string;
  email: string;
  avatar: string;
  role: 'admin' | 'project-manager' | 'member' | 'viewer';
  department: string;
  skills: string[];
  password?: string;
  joinedAt: string;
  // === HR 同步扩展字段（2026-06-10 来自 MDM 中间表，后端在 sync-hr 时回填） ===
  deptCode?: string;
  subOrgCd?: string;
  subOrgNam?: string;
  companyCd?: string;
  chineseNam?: string;
}
```

> 用 `?:` 可选字段，向后兼容：老数据没这 5 个键时，前端读 `user.deptCode` 拿到 `undefined`，不会触发类型错误或运行时空指针。

- [ ] **Step 2: 类型检查 + 构建**

```bash
cd frontend
npm run build
```

预期：`vue-tsc` 0 错误 + `vite build` 成功。`vue-tsc` 会校验所有引用 `User` 类型的地方（`stores/user.ts`、`views/Team.vue` 等）是否仍兼容。

---

## Task 4: 运行时验证（手动 smoke test）

**Files:** 无（curl/Postman + MySQL 客户端）

> 此 Task 验证 SQL 行为是否符合 spec。任何「编译通过但运行时炸」的问题（MDM 字段名拼错、`COLLATE` 不匹配、Mapper XML 解析错误）都在这一步暴露。

- [ ] **Step 1: 启动后端**

```bash
cd backend
mvn spring-boot:run
```

> 按 CLAUDE.md 的「Service Startup」规则，**这是用户明确要求启动的验证场景**，所以可以启动；验证完按需关闭。
>
> 启动日志正常后继续。

- [ ] **Step 2: 调用同步接口**

```bash
curl -X POST http://localhost:8084/api/users/sync-hr \
  -H "Authorization: <你的 JWT>" \
  -H "X-User-Id: <你的用户ID>" \
  -H "X-User-Role: admin"
```

预期响应：

```json
{
  "code": 200,
  "message": "人事数据同步完成",
  "data": { "inserted": <N>, "updated": <M> }
}
```

- [ ] **Step 3: 验证新增用户 5 个新字段有值**

```sql
SELECT id, name, chinese_nam, dept_code, sub_org_cd, sub_org_nam, company_cd
FROM sys_user
WHERE created_at > NOW() - INTERVAL 5 MINUTE
ORDER BY created_at DESC
LIMIT 10;
```

预期：新同步进来的用户，5 个新字段根据 MDM 实际值填入（不是空串的应能看到具体值；MDM 某字段为 NULL 的会是空串 `''`，符合设计）。

- [ ] **Step 4: 验证老用户 5 个字段被回填**

```sql
-- 找一个老用户（created_at 早于本次同步）
SELECT id, name, dept_code, chinese_nam, updated_at
FROM sys_user
WHERE updated_at > NOW() - INTERVAL 5 MINUTE
  AND created_at < NOW() - INTERVAL 1 HOUR
LIMIT 5;
```

预期：`updated_at` 是刚刷新的，且 5 个新字段已根据 MDM 值回填（如果 MDM 那边有数据）。

- [ ] **Step 5: 验证 API 返回的 JSON 含 5 个字段**

```bash
curl http://localhost:8084/api/users/<某用户ID> \
  -H "Authorization: <JWT>" \
  -H "X-User-Id: <ID>" \
  -H "X-User-Role: admin"
```

预期响应 JSON 顶层有 `deptCode` / `subOrgCd` / `subOrgNam` / `companyCd` / `chineseNam` 5 个键，值为字符串。

- [ ] **Step 6: 关闭后端**

后端启动的命令行窗口按 `Ctrl+C` 关闭。

---

## Task 5: 提交改动

**Files:** 全部修改文件

- [ ] **Step 1: 查看改动**

```bash
git status
git diff --stat
```

预期改动文件：
- `backend/src/main/java/com/wbs/project/entity/User.java`
- `backend/src/main/resources/mapper/UserMapper.xml`
- `frontend/src/types/index.ts`

- [ ] **Step 2: 暂存并提交**

```bash
git add backend/src/main/java/com/wbs/project/entity/User.java
git add backend/src/main/resources/mapper/UserMapper.xml
git add frontend/src/types/index.ts
git commit -m "feat(user): extend sys_user with 5 HR fields from MDM

为 sys_user 扩展 5 个 MDM 中间表字段:
- dept_code / sub_org_cd / sub_org_nam (组织架构层级)
- company_cd (公司 code: 2700/8400)
- chinese_nam (中文姓名)

接入现有的人事同步流程,新增用户和回填老用户都
会携带这 5 个字段;NULL 一律用空串覆盖。

改动:
- User.java: 新增 5 个字段
- UserMapper.xml: resultMap/Base_Column_List/insert/syncHrInsert/syncHrUpdate
- types/index.ts: User interface 新增 5 个可选字段

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

- [ ] **Step 3: 确认提交**

```bash
git log --oneline -3
```

预期：最新一条是上面的 `feat(user): extend sys_user with 5 HR fields from MDM`，前面两条分别是：
- `docs(spec): add sys_user HR fields extension design`
- `feat(scheduler): ...`（或其它最近的提交）

---

## 完成定义 (Definition of Done)

- [ ] DB 已 ALTER（5 列存在，默认值 `''`）
- [ ] `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] `npm run build` vue-tsc + vite build 都成功
- [ ] `POST /api/users/sync-hr` 调通，返回 `{inserted, updated}` 计数
- [ ] 新用户和老用户 5 个新字段在 DB 里有值
- [ ] API 返回的 JSON 含 5 个新键
- [ ] 改动已 commit

## 不在本次范围（防 scope creep）

- Team.vue / Settings.vue 加 UI 列（用户未要求）
- 给新字段加专门的编辑接口（同步即唯一来源）
- 删除/离职用户处理（不在本次处理）
- 跨子公司（2700/8400）的权限/数据隔离（不在本次处理）
