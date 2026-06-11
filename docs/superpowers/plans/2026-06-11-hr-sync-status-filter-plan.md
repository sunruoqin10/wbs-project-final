# 人事同步 - 在职状态过滤与离职标记 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `sys_user` 反映 `mdm_if_pa_a.ACT_CLSS_CD` 字段的分类（C=在职 / H=休职 / T=离职），并把 T 状态用户从所有「列表形」查询结果中过滤掉，阻断 admin 误标场景。

**Architecture:** 沿用项目现有的「Controller → Service → MyBatis Mapper」分层结构，**最小化**改动：新增 1 列 `status`，新增 2 个 mapper 方法（`syncHrMarkResigned` + `countAdminNotInMdm`），在 `syncHrData` 入口处加 admin 误标预检，对所有 list-shaped SQL 加 `WHERE status != 'T'`。前端只动 `User` interface、API 返回类型、toast 文案、两个 i18n 文件；**不动 Team 视图**（后端已过滤，前端无需感知）。

**Tech Stack:** Spring Boot 3.2.0 + MyBatis + MySQL 8 / Vue 3 + TypeScript + Vite / Vue I18n

**参考文档**：[spec: 2026-06-11-hr-sync-status-filter-design.md](../specs/2026-06-11-hr-sync-status-filter-design.md)

---

## 文件结构

### 修改文件（10 个）
| 文件 | 改动 |
|---|---|
| `backend/add_user_status_column.sql` | **新建** DDL 文件 |
| `backend/src/main/java/com/wbs/project/entity/User.java` | 新增 `status` 字段（`password` 之后） |
| `backend/src/main/java/com/wbs/project/mapper/UserMapper.java` | 新增 2 个方法签名 |
| `backend/src/main/resources/mapper/UserMapper.xml` | resultMap/Base_Column_List + 6 处 list-shaped SQL 加 `WHERE status != 'T'` + 1 处 syncHrUpdate 加 `status` 赋值 + 新增 2 个 SQL 段（syncHrMarkResigned, countAdminNotInMdm） + syncHrInsert 加 `status` + syncHrInsert/syncHrUpdate 加 `AND ACT_CLSS_CD IN ('C','H')` |
| `backend/src/main/java/com/wbs/project/service/UserService.java` | `syncHrData()` 改 Map 类型 + 加预检 |
| `frontend/src/types/index.ts` | `User` interface 新增 `status?: string` |
| `frontend/src/services/api.ts` | `syncHrUsers()` 返回类型加 `resigned: number` |
| `frontend/src/views/Settings.vue` | `handleSyncHr` toast 传 `resigned` |
| `frontend/src/i18n/locales/zh.ts` | `syncSuccessWithCount` 文案增加 `{resigned}` |
| `frontend/src/i18n/locales/ko.ts` | 同上（韩文） |

> 不存在 `frontend/src/i18n/locales/en.ts`，spec 已说明本次不动。

### 手动执行（用户跑，不在 plan 的代码任务内）
| 步骤 | 操作 |
|---|---|
| 备份 `sys_user` | `CREATE TABLE sys_user_backup_20260611 AS SELECT * FROM sys_user;` |
| 应用 DDL | `SOURCE backend/add_user_status_column.sql;` |
| 同步后校验 | `SELECT status, role, COUNT(*) FROM sys_user GROUP BY status, role;` |

---

## 前置说明

- **后端测试套件为空**（CLAUDE.md 已确认）。本计划用 `mvn clean install -DskipTests` 跑实际编译链路做验证。
- **前端无测试 runner**。验证用 `npm run build`（含 `vue-tsc` 类型检查）。
- **TDD 调整**：因无现成测试框架，本计划用「编译/类型检查 + 显式手测 SQL」替代单元测试。每完成一个文件改动后立即编译，确保错误定位精准。
- **CLAUDE.md 规则**：不要主动跑 `mvn spring-boot:run` / `npm run dev` 等长进程；不主动 `git commit`（每步 commit 都需要用户显式 "提交" 指令）。
- **跨文件落地顺序**：本计划 10 个文件改动**必须** 1 个 PR 落地。任何中间态都会让 schema 不一致（见 spec "跨文件落地顺序" 一节）。

---

## Task 0: 手动 DDL（备份 + ALTER）

**Files:** 无（手动执行）

**执行者：用户**（按 spec "部署 Runbook" 步骤 1+2）

- [ ] **Step 1: 备份 `sys_user` 表**

```bash
mysql -h localhost -u root -p db_webwbs -e \
  "CREATE TABLE sys_user_backup_20260611 AS SELECT * FROM sys_user;"
```

预期：表 `sys_user_backup_20260611` 创建成功。

- [ ] **Step 2: 验证 DDL 脚本存在**

```bash
ls "C:/Users/sunru/Desktop/其他/wbs-project-final/backend/add_user_status_column.sql"
```

预期：文件存在。如果不存在，先完成 Task 1 后再回这里跑 DDL。

> 这一步放在 Task 0 是因为 DDL 不依赖任何代码改动，可以独立执行；如果你想等代码完成后再应用 DDL，跳到 Task 5 之前回来执行即可。

---

## Task 1: 新建 DDL 文件

**Files:**
- Create: `backend/add_user_status_column.sql`

- [ ] **Step 1: 写入 DDL 文件**

```sql
-- ===============================================================
-- 2026-06-11: sys_user 增加 status 字段（HR 同步在职状态）
-- 来源 spec: docs/superpowers/specs/2026-06-11-hr-sync-status-filter-design.md
-- 命名约定: 与 backend/add_password_column.sql 等历史文件保持一致
-- ===============================================================
ALTER TABLE sys_user
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'C'
  COMMENT '在职状态: C=在职, H=休职, T=离职'
  AFTER password;
```

- [ ] **Step 2: 验证文件可读**

```bash
cat "C:/Users/sunru/Desktop/其他/wbs-project-final/backend/add_user_status_column.sql"
```

预期：能看到上面那段 ALTER。

> 提示：此时**不要**在数据库上跑这个文件——等其他 9 个代码文件就位后再统一跑 DDL（spec "跨文件落地顺序" 段落说明）。如果你已经在 Task 0 Step 1 备份过表，可以在这里把 DDL 提前应用；否则就等所有代码改完。

---

## Task 2: User 实体新增 status 字段

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/entity/User.java`

- [ ] **Step 1: 在 `User.java` 中 `password` 字段之后新增 `status`**

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
    private String status;  // 在职状态: C=在职, H=休职, T=离职（2026-06-11 来自 MDM.ACT_CLSS_CD）
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
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/backend"
mvn -pl . compile
```

预期：`BUILD SUCCESS`。Lombok `@Data` 会自动为新字段生成 getter/setter。

---

## Task 3: UserMapper.java 接口新增 2 个方法

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/UserMapper.java`

- [ ] **Step 1: 读取当前文件**

```bash
grep -n "syncHrInsert\|syncHrUpdate" "C:/Users/sunru/Desktop/其他/wbs-project-final/backend/src/main/java/com/wbs/project/mapper/UserMapper.java"
```

预期：能看到 `int syncHrInsert();` 和 `int syncHrUpdate();` 两行（参考 explore agent 报告，行号 ~92、~98）。

- [ ] **Step 2: 在 `syncHrUpdate();` 之后新增 2 个方法签名**

定位到 `int syncHrUpdate();` 那一行，在它后面（保持现有风格：单行方法 + 空行）插入：

```java
    int syncHrMarkResigned();
    int countAdminNotInMdm();
```

修改后片段示例：

```java
    int syncHrInsert();

    int syncHrUpdate();

    int syncHrMarkResigned();

    int countAdminNotInMdm();
```

- [ ] **Step 3: 编译确认无报错**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/backend"
mvn -pl . compile
```

预期：`BUILD SUCCESS`（即使 XML 里还没写对应 SQL，Java 接口先编译通过即可）。

---

## Task 4: UserMapper.xml 全部 SQL 改动

**Files:**
- Modify: `backend/src/main/resources/mapper/UserMapper.xml`

> 本任务改动量大（resultMap + Base_Column_List + 2 个 sync SQL + 1 个新 syncHrMarkResigned + 1 个新 countAdminNotInMdm + 6 处 list-shaped 加过滤），按下面 9 个 step 顺序执行，每步都编译。

- [ ] **Step 1: 修改 `resultMap`，新增 `status` 映射**

定位 `<result column="password" property="password"/>`，**在它之后**插入：

```xml
        <result column="status" property="status"/>
```

- [ ] **Step 2: 修改 `Base_Column_List`，在 `password` 之后加 `status`**

定位到 `Base_Column_List` 块（包含 `password` 那一行），把整段改为：

```xml
    <sql id="Base_Column_List">
        id, name, email, avatar, role, department,
        dept_code, sub_org_cd, sub_org_nam, company_cd, chinese_nam,
        skills, password, status,
        joined_at, created_at, updated_at
    </sql>
```

- [ ] **Step 3: 编译确认 1+2 无误**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/backend"
mvn -pl . compile
```

预期：`BUILD SUCCESS`。

- [ ] **Step 4: 修改通用 `insert` 语句，加 `status` 占位符**

定位到 `<insert id="insert">`，在列名段 `password,` 之后加 `status,`，在 VALUES 段 `#{password},` 之后加 `#{status},`。

修改后片段：

```xml
    <insert id="insert" parameterType="com.wbs.project.entity.User">
        INSERT INTO sys_user
          (id, name, email, avatar, role, department,
           dept_code, sub_org_cd, sub_org_nam, company_cd, chinese_nam,
           skills, password, status,
           joined_at, created_at, updated_at)
        VALUES
          (#{id}, #{name}, #{email}, #{avatar}, #{role}, #{department},
           #{deptCode}, #{subOrgCd}, #{subOrgNam}, #{companyCd}, #{chineseNam},
           #{skills}, #{password}, #{status},
           #{joinedAt}, #{createdAt}, #{updatedAt})
    </insert>
```

- [ ] **Step 5: 修改 `syncHrInsert`，加过滤 + status 赋值**

定位到 `<insert id="syncHrInsert">`，整段替换为（注意比原版多 1 列 `status` + WHERE 加 `ACT_CLSS_CD IN ('C','H')`）：

```xml
    <!-- 人事数据同步：插入新用户（仅同步 ACT_CLSS_CD IN C/H） -->
    <insert id="syncHrInsert">
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
            IFNULL(p.ACT_CLSS_CD, 'C')
        FROM mdm_if_pa_a p
        LEFT JOIN mdm_if_or_a o  ON p.ORG_CD    = o.ORG_CD
        LEFT JOIN mdm_if_or_a o2 ON p.SUB_ORG_CD = o2.ORG_CD
        WHERE p.EMP_NAM IS NOT NULL
          AND p.EMAIL_ADDR IS NOT NULL
          AND p.ACT_CLSS_CD IN ('C','H');
    </insert>
```

- [ ] **Step 6: 修改 `syncHrUpdate`，加过滤 + status 赋值**

定位到 `<update id="syncHrUpdate">`，整段替换为：

```xml
    <!-- 人事数据同步：更新已有用户（仅处理 C/H，含 T→C/H 复活） -->
    <update id="syncHrUpdate">
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
            u.status      = IFNULL(p.ACT_CLSS_CD, 'C'),
            u.updated_at  = NOW()
        WHERE p.EMP_NAM IS NOT NULL
          AND p.EMAIL_ADDR IS NOT NULL
          AND p.ACT_CLSS_CD IN ('C','H');
    </update>
```

- [ ] **Step 7: 新增 `syncHrMarkResigned` SQL**

在 `syncHrUpdate` 之后插入新段：

```xml
    <!-- 人事数据同步：标记离职（MDM 中为 T 或缺失），含复活由 syncHrUpdate 负责 -->
    <update id="syncHrMarkResigned">
        UPDATE sys_user u
        LEFT JOIN mdm_if_pa_a p ON u.id = p.EMP_NUM COLLATE utf8mb4_unicode_ci
        SET u.status = 'T',
            u.updated_at = NOW()
        WHERE u.status != 'T'
          AND (p.EMP_NUM IS NULL OR p.ACT_CLSS_CD = 'T');
    </update>
```

- [ ] **Step 8: 新增 `countAdminNotInMdm` SQL**

紧接 `syncHrMarkResigned` 之后：

```xml
    <!-- 同步前安全检查：在岗 admin 但 MDM 中缺失（用于阻断误标） -->
    <select id="countAdminNotInMdm" resultType="int">
        SELECT COUNT(*) FROM sys_user u
        WHERE u.role = 'admin'
          AND u.status != 'T'
          AND NOT EXISTS (
            SELECT 1 FROM mdm_if_pa_a p
            WHERE p.EMP_NUM = u.id COLLATE utf8mb4_unicode_ci
          );
    </select>
```

- [ ] **Step 9: 给 6 个 list-shaped SQL 加 `WHERE status != 'T'`**

⚠️ **关键：6 段 SQL 改法略有差异，详见 spec §2.6 表格。**

| 方法 | 改法 |
|---|---|
| `selectAll` | 末尾追加 `WHERE u.status != 'T'`（替换或追加现有 WHERE，保留原 ORDER BY） |
| `searchUsers` | 末尾追加 `AND u.status != 'T'`（此 SQL 无 JOIN，可写 `AND status != 'T'`，但为统一起见写 `AND u.status != 'T'`） |
| `countSearchUsers` | 末尾追加 `AND status != 'T'`（**无 `u.` 别名**！此 SQL 无 JOIN） |
| `selectByRole` | 末尾追加 `AND u.status != 'T'` |
| `selectByDepartment` | 末尾追加 `AND u.status != 'T'` |
| `selectByIds` | 末尾追加 `AND u.status != 'T'` |

示例（针对 `selectAll`）：

修改前（假设）：

```xml
<select id="selectAll" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM sys_user u
    ORDER BY created_at DESC
</select>
```

修改后：

```xml
<select id="selectAll" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM sys_user u
    WHERE u.status != 'T'
    ORDER BY created_at DESC
</select>
```

**具体每个方法请打开 `UserMapper.xml` 查看现有 SQL 形态后再加过滤。** 不要凭印象粘贴——必须确认现有 WHERE 子句是否存在、`u.` 别名是否存在（`countSearchUsers` 无 `u.` 别名）。

- [ ] **Step 10: 编译 + 跑全链路 build 确认 4-9 无误**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/backend"
mvn clean install -DskipTests
```

预期：`BUILD SUCCESS`。如果失败，对照报错行号回到对应 step 修改。

---

## Task 5: UserService.syncHrData 加预检 + resigned 步骤

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/UserService.java`

- [ ] **Step 1: 定位现有 `syncHrData` 方法**

```bash
grep -n "syncHrData" "C:/Users/sunru/Desktop/其他/wbs-project-final/backend/src/main/java/com/wbs/project/service/UserService.java"
```

预期：能看到 3 行（注释 + `@Transactional` + 方法签名 + body）。行号 ~200-214（参考 explore agent 报告）。

- [ ] **Step 2: 替换 `syncHrData` 方法体**

把整段（含注释）替换为：

```java
    /**
     * 同步人事数据
     * 从 mdm_if_pa_a 和 mdm_if_or_a 表同步数据到 sys_user
     * 流程：插入新用户 → 更新已有用户 → 标记离职用户
     * 同步前会阻断「在岗 admin 但 MDM 缺失」的情况，避免误标管理员为离职
     * @return 包含 inserted / updated / resigned 数量的 Map
     */
    @Transactional
    public java.util.Map<String, Integer> syncHrData() {
        // 1. 同步前安全检查：阻断 admin 误标
        int adminNotInMdm = userMapper.countAdminNotInMdm();
        if (adminNotInMdm > 0) {
            throw new com.wbs.project.exception.BusinessException(
                409,
                "检测到 " + adminNotInMdm + " 名管理员不在 MDM 中，继续同步将被标记为离职。"
                + "请先在 MDM 中维护管理员记录，或手动设置其 status='C' 后再重试。"
            );
        }

        // 2. 同步流程
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

- [ ] **Step 3: 编译后端**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/backend"
mvn clean install -DskipTests
```

预期：`BUILD SUCCESS`。`BusinessException` 的 import 路径已在 Task 4 编译时确认存在（spec §4.1 已验证路径正确）。

---

## Task 6: 前端 User interface 新增 status

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1: 定位 User interface**

```bash
grep -n "chineseNam\|deptCode" "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend/src/types/index.ts"
```

预期：能看到 User interface 中已有的 `chineseNam?: string;` 字段。

- [ ] **Step 2: 在 `chineseNam?: string;` 之后新增 `status` 字段**

在 `chineseNam?: string;` 那一行**之后**插入：

```ts
  status?: string; // C=在职, H=休职, T=离职（后端已过滤 T，前端不展示）
```

- [ ] **Step 3: 前端类型检查**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend"
npx vue-tsc --noEmit
```

预期：无报错（exit code 0）。

---

## Task 7: api.ts 扩展 syncHrUsers 返回类型

**Files:**
- Modify: `frontend/src/services/api.ts`

- [ ] **Step 1: 定位 syncHrUsers 方法**

```bash
grep -n "syncHrUsers\|sync-hr" "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend/src/services/api.ts"
```

预期：能在 ~402 行附近看到该方法（参考 explore agent 报告）。

- [ ] **Step 2: 修改 `syncHrUsers` 返回类型**

把现有签名（参考形态）：

```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number }> {
  return this.post('/users/sync-hr', {});
}
```

改为：

```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number }> {
  return this.post('/users/sync-hr', {});
}
```

- [ ] **Step 3: 前端类型检查**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend"
npx vue-tsc --noEmit
```

预期：无报错。

---

## Task 8: Settings.vue toast 传 resigned

**Files:**
- Modify: `frontend/src/views/Settings.vue`

- [ ] **Step 1: 定位 `handleSyncHr` 中的 toast 文案**

```bash
grep -n "syncSuccessWithCount\|handleSyncHr" "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend/src/views/Settings.vue"
```

预期：能看到 ~549 行（参考 explore agent 报告）的 `t('settings.hrSync.syncSuccessWithCount', { inserted: ..., updated: ... })`。

- [ ] **Step 2: 在 toast 参数对象中加 `resigned`**

修改前：

```ts
message: t('settings.hrSync.syncSuccessWithCount', { inserted: result.inserted, updated: result.updated }),
```

修改后：

```ts
message: t('settings.hrSync.syncSuccessWithCount', {
  inserted: result.inserted,
  updated: result.updated,
  resigned: result.resigned,
}),
```

- [ ] **Step 3: 前端类型检查**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend"
npx vue-tsc --noEmit
```

预期：无报错。

---

## Task 9: i18n 文案加 {resigned} 占位符

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`
- Modify: `frontend/src/i18n/locales/ko.ts`

- [ ] **Step 1: 修改 `zh.ts` 的 `syncSuccessWithCount`**

```bash
grep -n "syncSuccessWithCount" "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend/src/i18n/locales/zh.ts"
```

定位到 ~728 行（参考 explore agent 报告），把：

```ts
syncSuccessWithCount: '同步完成：新增 {inserted} 人，更新 {updated} 人',
```

改为：

```ts
syncSuccessWithCount: '同步完成：新增 {inserted} 人，更新 {updated} 人，标记离职 {resigned} 人',
```

- [ ] **Step 2: 修改 `ko.ts` 的 `syncSuccessWithCount`**

```bash
grep -n "syncSuccessWithCount" "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend/src/i18n/locales/ko.ts"
```

定位到对应行，把：

```ts
syncSuccessWithCount: '동기화 완료: {inserted}명 추가, {updated}명 업데이트',
```

改为：

```ts
syncSuccessWithCount: '동기 완료: {inserted}명 추가, {updated}명 업데이트, {resigned}명 퇴직 처리',
```

> 注：原 key 写的是 "동기화 완료" 还是 "동기 완료"、是否带 "명" 等细节，请以 `ko.ts` 实际值为准，**只加 `, {resigned}명 퇴직 처리` 后缀**即可，不要改其他字。

- [ ] **Step 3: 前端全量 build 确认**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final/frontend"
npm run build
```

预期：`✓ built in ...` 成功输出，`vue-tsc` 通过。

---

## Task 10: 应用 DDL（如果之前没跑）

**Files:** 无（手动执行）

- [ ] **Step 1: 在 MySQL 客户端应用 DDL**

如果你在 Task 0 跳过了 DDL 执行（或 DDL 文件在 Task 1 才创建），现在所有 9 个代码文件就位了，可以跑 DDL：

```bash
mysql -h localhost -u root -p db_webwbs < \
  "C:/Users/sunru/Desktop/其他/wbs-project-final/backend/add_user_status_column.sql"
```

- [ ] **Step 2: 验证表结构**

```sql
DESC sys_user;
```

预期：能看到 `status VARCHAR(20) NOT NULL DEFAULT 'C' COMMENT '在职状态: ...' AFTER password`。

- [ ] **Step 3: 验证默认填充**

```sql
SELECT COUNT(*) AS total, SUM(status='C') AS active FROM sys_user;
```

预期：`active == total`（所有已有用户都被默认填为 `'C'` 在职）。

- [ ] **Step 4: 同步前 admin 风险扫描**

```sql
SELECT id, role, status FROM sys_user
WHERE role='admin' AND status != 'T'
  AND id NOT IN (SELECT EMP_NUM FROM mdm_if_pa_a);
```

- 若返回 0 行：安全可同步（进入 Task 11）
- 若返回 N 行：先在 MDM 维护或 `UPDATE sys_user SET status='C' WHERE id='<id>'` 后再同步

---

## Task 11: 运行时手测（用户执行，不在代码任务内）

**Files:** 无

- [ ] **Step 1: 重启后端 + 前端**（如果未运行）

如果之前后端在跑，**必须重启**以加载新 SQL 和 Java 代码。详见 CLAUDE.md "Service Startup" 规则——**不**自动启动服务。

- [ ] **Step 2: 触发同步（场景 A：新 C 员工）**

```bash
# 假设有一个测试用的 MDM 记录
mysql -h localhost -u root -p db_webwbs -e \
  "INSERT INTO mdm_if_pa_a (EMP_NUM, EMP_NAM, EMAIL_ADDR, ACT_CLSS_CD) VALUES ('TEST001', '测试员工', 'test001@company.com', 'C');"
```

通过前端 Settings → 人事同步 → "开始同步" 按钮触发。

预期：toast 显示 `同步完成：新增 1 人，更新 0 人，标记离职 0 人`。

- [ ] **Step 3: 触发同步（场景 B：改为 T）**

```sql
UPDATE mdm_if_pa_a SET ACT_CLSS_CD='T' WHERE EMP_NUM='TEST001';
```

再次同步。

预期：toast 显示 `同步完成：新增 0 人，更新 0 人，标记离职 1 人`。`GET /api/users` 列表中 `TEST001` 不可见。

- [ ] **Step 4: 触发同步（场景 C：复活为 C）**

```sql
UPDATE mdm_if_pa_a SET ACT_CLSS_CD='C' WHERE EMP_NUM='TEST001';
```

再次同步。

预期：toast 显示 `同步完成：新增 0 人，更新 1 人，标记离职 0 人`（syncHrUpdate 把 status 改回 C）。`GET /api/users` 列表中 `TEST001` 再次可见。

- [ ] **Step 5: 触发同步（场景 E：admin 误标阻断）**

在 sys_user 中手动插入一个 admin：

```sql
INSERT INTO sys_user (id, name, role, password, status) VALUES ('TESTADMIN', '测试admin', 'admin', '1', 'C');
```

触发同步。

预期：**同步被阻断**，后端返回 409 错误，前端 toast 显示 `检测到 1 名管理员不在 MDM 中...`。

清理：

```sql
DELETE FROM sys_user WHERE id='TESTADMIN';
DELETE FROM mdm_if_pa_a WHERE EMP_NUM='TEST001';
```

- [ ] **Step 6: 同步后状态分布校验**

```sql
SELECT status, role, COUNT(*) AS cnt FROM sys_user GROUP BY status, role ORDER BY status, role;
```

预期：与同步前相比，只有预期的行变化（无意外的 'T'）。

---

## Task 12: 提议 commit 并合并 PR

**Files:** 全部 10 个文件已修改/新建

- [ ] **Step 1: 查看 diff 概览**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final"
git status --short
```

预期：看到 10 个 modified/new 文件。

- [ ] **Step 2: 提议 commit message 并等待用户确认**

按 CLAUDE.md 规则，**不**自动 commit。向用户提议：

```
feat(hr-sync): filter C/H, mark T users, block admin mislabeling

Adds status column to sys_user mirroring mdm_if_pa_a.ACT_CLSS_CD:
- C (在职) / H (休职) are synced; T (离职) are not
- Existing sys_user rows whose EMP_NUM is missing from MDM are
  soft-marked as T (skipped if already T)
- All list-shaped user queries filter out T rows; login is unchanged
- A pre-sync countAdminNotInMdm guard throws 409 if any in-service
  admin is missing from MDM, preventing accidental admin lockout

Touches 10 files: 1 DDL + 4 backend + 5 frontend.
Spec: docs/superpowers/specs/2026-06-11-hr-sync-status-filter-design.md
Plan: docs/superpowers/plans/2026-06-11-hr-sync-status-filter-plan.md
```

- [ ] **Step 3: 等待用户 "提交" / "commit" 指令后执行 commit**

```bash
cd "C:/Users/sunru/Desktop/其他/wbs-project-final"
git add \
  backend/add_user_status_column.sql \
  backend/src/main/java/com/wbs/project/entity/User.java \
  backend/src/main/java/com/wbs/project/mapper/UserMapper.java \
  backend/src/main/resources/mapper/UserMapper.xml \
  backend/src/main/java/com/wbs/project/service/UserService.java \
  frontend/src/types/index.ts \
  frontend/src/services/api.ts \
  frontend/src/views/Settings.vue \
  frontend/src/i18n/locales/zh.ts \
  frontend/src/i18n/locales/ko.ts
git commit -m "feat(hr-sync): filter C/H, mark T users, block admin mislabeling" \
           -m "Adds status column to sys_user mirroring mdm_if_pa_a.ACT_CLSS_CD:" \
           -m "- C (在职) / H (休职) are synced; T (离职) are not" \
           -m "- Existing sys_user rows whose EMP_NUM is missing from MDM are" \
           -m "  soft-marked as T (skipped if already T)" \
           -m "- All list-shaped user queries filter out T rows; login is unchanged" \
           -m "- A pre-sync countAdminNotInMdm guard throws 409 if any in-service" \
           -m "  admin is missing from MDM, preventing accidental admin lockout" \
           -m "Touches 10 files: 1 DDL + 4 backend + 5 frontend." \
           -m "Spec: docs/superpowers/specs/2026-06-11-hr-sync-status-filter-design.md" \
           -m "Plan: docs/superpowers/plans/2026-06-11-hr-sync-status-filter-plan.md"
```

预期：commit 成功，10 个文件就位。

---

## 故障排查

| 症状 | 排查 |
|---|---|
| 后端编译失败 `BusinessException cannot be resolved` | 确认 `com.wbs.project.exception.BusinessException` 存在；如不存在说明 spec 路径错了，stop & 回用户。 |
| `npm run build` 失败 `Property 'resigned' does not exist on type ...` | 检查 `frontend/src/services/api.ts` 的 `syncHrUsers` 返回类型是否已加 `resigned: number`。 |
| 运行时 `Unknown column 'u.status'` | DDL 未应用，回到 Task 0/10 跑 DDL。 |
| 运行时同步返回 `inserted: 0, updated: 0, resigned: 0` 但实际有变更 | 检查 MDM 中 `EMP_NAM` 和 `EMAIL_ADDR` 是否非空（这是 syncHrInsert/syncHrUpdate 的 WHERE 条件之一）。 |
| 同步被 409 阻断但 admin 看起来都在 MDM | 跑 `SELECT id FROM sys_user WHERE role='admin'` 列出所有 admin id，然后到 `mdm_if_pa_a` 里 `SELECT DISTINCT EMP_NUM` 取并集比较；可能存在空格、大小写或 collate 不一致。 |
| i18n key 找不到 | 确认 `syncSuccessWithCount` 在 `zh.ts` 和 `ko.ts` 中都存在（spec 已确认无 `en.ts`）。 |
| T 用户仍能登录 | 符合 spec 设计——本次**不**阻断 T 用户登录，仅不展示。 |

---

## 范围外（提醒）

按 spec 明确，本次**不**做：
- 阻断 T 用户登录
- 管理员白名单（已用 §4.1 预检阻断代替）
- `?force=true` 绕过预检
- 离职时间戳 / 历史记录"已离职"角标
- 同步频率优化（仍手动触发）
- 删除离职 N 天以上的用户（数据归档）
- Team / Settings 页面的 status 列展示
