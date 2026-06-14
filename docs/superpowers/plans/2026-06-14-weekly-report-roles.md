# 周报管理 —— 4 角色数据可见 + 审批口径对齐 实施计划

> **For agentic workers:** REQUIRED: Use `superpowers:subagent-driven-development` (if subagents available) or `superpowers:executing-plans` to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把周报模块的"谁能看 / 谁能审"从 Controller 硬编码 + Java 内存过滤 + 4 处口径不一致,收敛到 `PermissionService` 单一入口的 5 档判定(照搬加班),并新增 `sys_weekly_report_approval_log` 审计表与审批记录面板。

**Architecture:** 后端权限决策走 `PermissionService.canXxxWeeklyReport` 5 档判定;数据范围走 `getAccessibleWeeklyReportUserIds` 返回 IN 集合;Controller 撕掉所有硬编码白名单;`WeeklyReportService.approveReport` 主流程末尾 try/catch 写日志,失败不回滚。前端 `permission` store 镜像同款 5 档,所有按钮显隐统一走 store。

**Tech Stack:** Spring Boot 3.2 + MyBatis + MySQL(`utf8mb4`)、Vue 3 + TS + Pinia。无 ESLint / Checkstyle / 测试运行器,验证靠 `mvn clean install -DskipTests` + `npx vue-tsc` + `npm run build` + 手工 SQL 角色账号脚本。

**Spec 引用:** `docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md`(下文凡涉及完整代码块的位置都给出 §x.y 引用,执行者请按 spec 内容逐字落盘)。

---

## ⚠️ 关键约束(执行者必读)

1. **`CLAUDE.md` 禁止任何形式的自动 commit / push**。本计划里所有"COMMIT GATE"步骤的形式都是 ——
   - 整理 `git status` 与拟用 commit message;
   - **暂停 / 等待用户明确说出"提交" / "commit" / "确认提交"**;
   - 用户回复"提交"才执行 `git add` + `git commit`;
   - 收到任何其它回答(包括"OK"、"看起来不错"等)**都不算提交许可**,继续等待。
2. **`CLAUDE.md` 禁止自动启动后端 / 前端服务**。`mvn spring-boot:run` / `npm run dev` / `npm run preview` 一律不能自启;只能用 `mvn clean install -DskipTests` / `npx vue-tsc` / `npm run build` 这类非交互命令做验证。
3. **后端 `Result.message` 用中文**(`CLAUDE.md` 明文要求)。新增报错文案统一用中文。
4. **不引入新 linter / formatter / test runner**(`CLAUDE.md`)。
5. **没有自动化测试可写**:后端 `src/test/` 空,前端无 runner。本计划用"build verifies + 手工 SQL 脚本"替代 TDD。

---

## 文件结构

### 新建文件(8)

| 路径 | 职责 |
|---|---|
| `backend/add_weekly_report_approval_log_table.sql` | DDL |
| `backend/src/main/java/com/wbs/project/entity/WeeklyReportApprovalLog.java` | Entity |
| `backend/src/main/java/com/wbs/project/dto/WeeklyReportApprovalLogDTO.java` | 出参 DTO(带 approverName/avatar) |
| `backend/src/main/java/com/wbs/project/mapper/WeeklyReportApprovalLogMapper.java` | Mapper interface |
| `backend/src/main/resources/mapper/WeeklyReportApprovalLogMapper.xml` | Mapper XML(insert / selectByReportId) |
| `backend/src/main/java/com/wbs/project/service/WeeklyReportApprovalLogService.java` | Service(`log()` + `listByReport()` + `resolveApproverRole()`) |
| `frontend/src/components/weeklyReport/WeeklyReportApprovalLog.vue` | 详情页审批记录面板 |
| (已存在) `docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md` | spec 文档,本期一并提交 |
| (本文件) `docs/superpowers/plans/2026-06-14-weekly-report-roles.md` | 实施计划,本期一并提交 |

### 修改文件(13)

| 路径 | 改动范围 |
|---|---|
| `backend/src/main/java/com/wbs/project/service/PermissionService.java` | 重写 `canViewWeeklyReport`(L343-346)+ `canApproveWeeklyReport`(L351-360);新增 `canEditWeeklyReport` / `canDeleteWeeklyReport` / `getAccessibleWeeklyReportUserIds` |
| `backend/src/main/java/com/wbs/project/controller/WeeklyReportController.java` | 删除内联 `hasPermission` 与硬编码白名单;`getReports` / `getReportById` / `updateReport` / `deleteReport` / `approveReport` 全部走 PermissionService;新增 `GET /{id}/approval-logs` |
| `backend/src/main/java/com/wbs/project/service/WeeklyReportService.java` | `approveReport` 加 reject comment 必填校验 + 审批日志触点;新增 `getReportsByUserIds(List<String>)` |
| `backend/src/main/java/com/wbs/project/mapper/WeeklyReportMapper.java` | 新增 `selectByUserIds(List<String>)` |
| `backend/src/main/resources/mapper/WeeklyReportMapper.xml` | 新增 `<select id="selectByUserIds">` |
| `frontend/src/stores/permission.ts` | 重写 `canViewWeeklyReport` / `canApproveWeeklyReport`;新增 `canEditWeeklyReport` / `canDeleteWeeklyReport`(均取 `WeeklyReportPermissionInput`) |
| `frontend/src/stores/weeklyReport.ts` | user/role 切换 `watch` 重拉数据 |
| `frontend/src/views/WeeklyReports.vue` | 删除前端二次角色 filter,仅留 UI 层 status/keyword filter |
| `frontend/src/views/WeeklyReportDetail.vue` | 删除内联 `canApprove`;挂载 `WeeklyReportApprovalLog.vue` |
| `frontend/src/components/weeklyReport/WeeklyReportCard.vue` | `canEdit/canDelete/canApprove` 改走 `permissionStore` |
| `frontend/src/components/weeklyReport/WeeklyReportComment.vue` | `canDelete` 按"评论作者 / admin / 周报可见者"判定 |
| `frontend/src/services/api.ts` | `weeklyReport.getApprovalLogs(reportId: string)` |
| `frontend/src/types/index.ts` | 新增 `WeeklyReportApprovalLog` 接口 |
| `frontend/src/i18n/locales/zh.ts` | `weeklyReports.approvalHistory` / `weeklyReports.action.*` / `roles.project_owner` |
| `frontend/src/utils/constants.ts` | `USER_ROLE_OPTIONS` 补 `dept-project-manager` |

---

## 任务清单

> 4 个 commit 粒度:① DDL → ② 后端 → ③ 前端 → ④ docs(spec + plan)。每个 commit 前都有 **COMMIT GATE** 暂停点。

---

### Task 1: 创建审批日志 DDL 文件

**Files:**
- Create: `backend/add_weekly_report_approval_log_table.sql`

- [ ] **Step 1.1: 写 DDL 文件**

把以下内容**逐字写入**`backend/add_weekly_report_approval_log_table.sql`(对应 spec §5.1):

```sql
-- backend/add_weekly_report_approval_log_table.sql
-- 周报审批操作日志表(只读审计,不替代主表 approver_id / approve_time)
-- 2026-06-14: 配合周报 4 角色数据可见 + 审批口径对齐 引入

CREATE TABLE IF NOT EXISTS sys_weekly_report_approval_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    report_id     VARCHAR(64)  NOT NULL                COMMENT '周报 ID -> sys_weekly_report.id (String,见 WeeklyReport.id)',
    approver_id   VARCHAR(8)   NOT NULL                COMMENT '审批人 user_id',
    approver_role VARCHAR(32)  NOT NULL                COMMENT '审批人当时角色快照: admin / dept-project-manager / project-manager / project-owner',
    action        VARCHAR(16)  NOT NULL                COMMENT 'approve / reject',
    comment       TEXT         NULL                    COMMENT '审批意见;reject 时必填',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_id        (report_id),
    KEY idx_approver_created (approver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='周报审批操作日志(只读审计;不替代主表 approver_id/approve_time)';
```

- [ ] **Step 1.2: 不要自己跑 mysql**

按 `CLAUDE.md` 不启服务规则,执行 DDL 是用户的事。**记录、不执行**:
```
执行方式(由用户在 MySQL 客户端运行):
  mysql -uroot -proot db_webwbs < backend/add_weekly_report_approval_log_table.sql
```
继续后续步骤,不依赖表已建立。

---

### Task 2: COMMIT GATE ①

- [ ] **Step 2.1: 整理 commit ① 范围**

仅 `backend/add_weekly_report_approval_log_table.sql`。

- [ ] **Step 2.2: 向用户展示拟用 commit message**

```
feat(weekly-report): 新增 sys_weekly_report_approval_log 审计表

照搬加班审批日志的 schema,为周报审批新增独立审计表;不加 DB 外键
(按 CLAUDE.md 在 Java 层校验)。VARCHAR(64) report_id 对齐
WeeklyReport.id (String) 类型。

Spec: docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md §5.1
```

- [ ] **Step 2.3: 暂停等用户明确说"提交"**

**只有**用户回复"提交" / "commit" / "确认提交"才执行 ——
```bash
git add backend/add_weekly_report_approval_log_table.sql
git commit -m "feat(weekly-report): 新增 sys_weekly_report_approval_log 审计表

照搬加班审批日志的 schema,为周报审批新增独立审计表;不加 DB 外键
(按 CLAUDE.md 在 Java 层校验)。VARCHAR(64) report_id 对齐
WeeklyReport.id (String) 类型。

Spec: docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md §5.1"
```

收到"OK / 看起来不错 / 这个 message 写得对"等回答**都不算许可**,继续等。

---

### Task 3: 新建审批日志 Entity 与 DTO

**Files:**
- Create: `backend/src/main/java/com/wbs/project/entity/WeeklyReportApprovalLog.java`
- Create: `backend/src/main/java/com/wbs/project/dto/WeeklyReportApprovalLogDTO.java`

- [ ] **Step 3.1: 写 Entity**

逐字写入 `backend/src/main/java/com/wbs/project/entity/WeeklyReportApprovalLog.java`(spec §5.2):

```java
package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyReportApprovalLog {
    private Long id;
    private String reportId;       // 对齐 WeeklyReport.id (String)
    private String approverId;
    private String approverRole;   // admin / dept-project-manager / project-manager / project-owner
    private String action;         // approve / reject
    private String comment;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3.2: 写 DTO**

逐字写入 `backend/src/main/java/com/wbs/project/dto/WeeklyReportApprovalLogDTO.java`:

```java
package com.wbs.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeeklyReportApprovalLogDTO {
    private Long id;
    private String reportId;
    private String approverId;
    private String approverName;     // join sys_user.name
    private String approverAvatar;   // join sys_user.avatar_url
    private String approverRole;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
```

> **不要**做 `@JsonProperty` / 注解魔法 —— 仓库的 DTO 模式是 plain POJO + `@Data`,见 `dto/` 同目录其它 DTO。

---

### Task 4: 新建审批日志 Mapper(接口 + XML)

**Files:**
- Create: `backend/src/main/java/com/wbs/project/mapper/WeeklyReportApprovalLogMapper.java`
- Create: `backend/src/main/resources/mapper/WeeklyReportApprovalLogMapper.xml`

- [ ] **Step 4.1: Mapper 接口**

逐字写入 `backend/src/main/java/com/wbs/project/mapper/WeeklyReportApprovalLogMapper.java`:

```java
package com.wbs.project.mapper;

import com.wbs.project.dto.WeeklyReportApprovalLogDTO;
import com.wbs.project.entity.WeeklyReportApprovalLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeeklyReportApprovalLogMapper {

    int insert(WeeklyReportApprovalLog row);

    List<WeeklyReportApprovalLogDTO> selectByReportId(@Param("reportId") String reportId);
}
```

- [ ] **Step 4.2: Mapper XML**

逐字写入 `backend/src/main/resources/mapper/WeeklyReportApprovalLogMapper.xml`(spec §5.3):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.wbs.project.mapper.WeeklyReportApprovalLogMapper">

    <insert id="insert"
            parameterType="com.wbs.project.entity.WeeklyReportApprovalLog"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO sys_weekly_report_approval_log
            (report_id, approver_id, approver_role, action, comment, created_at)
        VALUES
            (#{reportId}, #{approverId}, #{approverRole}, #{action}, #{comment}, #{createdAt})
    </insert>

    <select id="selectByReportId"
            resultType="com.wbs.project.dto.WeeklyReportApprovalLogDTO">
        SELECT
            l.id,
            l.report_id     AS reportId,
            l.approver_id   AS approverId,
            u.name          AS approverName,
            u.avatar_url    AS approverAvatar,
            l.approver_role AS approverRole,
            l.action,
            l.comment,
            l.created_at    AS createdAt
        FROM sys_weekly_report_approval_log l
        LEFT JOIN sys_user u ON u.id = l.approver_id
        WHERE l.report_id = #{reportId}
        ORDER BY l.created_at ASC
    </select>

</mapper>
```

---

### Task 5: 新建 WeeklyReportApprovalLogService

**Files:**
- Create: `backend/src/main/java/com/wbs/project/service/WeeklyReportApprovalLogService.java`

- [ ] **Step 5.1: 写 Service 类**

逐字写入(spec §5.4 完整内容):

```java
package com.wbs.project.service;

import com.wbs.project.dto.WeeklyReportApprovalLogDTO;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.entity.WeeklyReportApprovalLog;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.mapper.WeeklyReportApprovalLogMapper;
import com.wbs.project.mapper.WeeklyReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportApprovalLogService {

    private final WeeklyReportApprovalLogMapper logMapper;
    private final UserMapper userMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final ProjectMapper projectMapper;

    /** 主流程已成功;写日志失败仅 ERROR,不抛出 */
    public void log(String reportId, String approverId, String action, String comment) {
        try {
            WeeklyReport r = weeklyReportMapper.selectById(reportId);
            WeeklyReportApprovalLog row = new WeeklyReportApprovalLog();
            row.setReportId(reportId);
            row.setApproverId(approverId);
            row.setApproverRole(resolveApproverRole(approverId,
                    r == null ? null : r.getProjectId()));
            row.setAction(action);
            row.setComment(comment);
            row.setCreatedAt(LocalDateTime.now());
            logMapper.insert(row);
        } catch (Exception e) {
            log.error("写入周报审批日志失败 reportId={} approver={}", reportId, approverId, e);
        }
    }

    public List<WeeklyReportApprovalLogDTO> listByReport(String reportId) {
        return logMapper.selectByReportId(reportId);
    }

    /** 与 OvertimeService.resolveApproverRole:376-388 同形(顺序不可调) */
    private String resolveApproverRole(String approverId, String projectId) {
        User approver = userMapper.selectById(approverId);
        if (approver == null) return "unknown";
        String role = approver.getRole();
        if ("admin".equals(role) || "project-manager".equals(role)) {
            return role;
        }
        if (projectId != null) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && approverId.equals(project.getOwnerId())) {
                return "project-owner";
            }
        }
        return "dept-project-manager";
    }
}
```

---

### Task 6: 扩展 `WeeklyReportMapper`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/WeeklyReportMapper.java`
- Modify: `backend/src/main/resources/mapper/WeeklyReportMapper.xml`

- [ ] **Step 6.1: 接口加 `selectByUserIds`**

在 `WeeklyReportMapper.java` 的 `selectByProjectId` 下面追加一行(保留现有签名风格):

```java
List<WeeklyReport> selectByUserIds(@Param("userIds") List<String> userIds);
```

如果 imports 里没有 `java.util.List`,无须改(已有 `import java.util.List;`)。

- [ ] **Step 6.2: XML 加 `<select id="selectByUserIds">`**

在 `WeeklyReportMapper.xml` 内追加(放在 `selectByProjectId` 之后,保持顺序),spec §4.4:

```xml
<select id="selectByUserIds" resultMap="WeeklyReportMap">
    SELECT * FROM sys_weekly_report
    WHERE user_id IN
    <foreach collection="userIds" item="uid" open="(" separator="," close=")">
        #{uid}
    </foreach>
    ORDER BY week_start DESC, created_at DESC
</select>
```

> `resultMap="WeeklyReportMap"` 已存在于该 XML 顶部,直接引用即可。

---

### Task 7: 改写 `PermissionService`(5 方法)

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/PermissionService.java`

- [ ] **Step 7.1: 重写 `canViewWeeklyReport`(L343-346)**

把原方法体替换为:

```java
public boolean canViewWeeklyReport(String userId, String reportId) {
    if (isAdmin(userId)) return true;
    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;
    if (userId != null && userId.equals(report.getUserId())) return true;     // 自己始终能看

    // submitter 不在可见范围内 → 拒绝
    User submitter = userMapper.selectById(report.getUserId());
    if (submitter == null) return false;
    String submitterDept = submitter.getDeptCode();
    String projectId = report.getProjectId();

    if (isProjectOwner(userId, projectId)) return true;
    if (isManagedProject(userId, projectId)) return true;
    if (isDeptManagerOf(userId, submitterDept)) return true;
    return false;
}
```

如果类里还没有 `weeklyReportMapper` 字段,在类顶部 fields 区域加(注意已有 `@RequiredArgsConstructor`,直接 `private final` 即可):

```java
private final WeeklyReportMapper weeklyReportMapper;
```

import:

```java
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.mapper.WeeklyReportMapper;
```

如果 `isDeptManagerOf(approverId, deptCode)` 不存在,改用现有 `isDeptManagerOfUser(approverId, submitterUserId)` 或 `isDeptManager(approverId, submitterDept)` —— 请先 grep 一次 PermissionService 确认实际方法名,以仓库为准。

- [ ] **Step 7.2: 重写 `canApproveWeeklyReport`(L351-360)**

替换原方法体为 spec §4.2 5 档判定(注意签名仍是 `(String, String)`):

```java
public boolean canApproveWeeklyReport(String approverId, String reportId) {
    User approver = userMapper.selectById(approverId);
    if (approver == null) return false;
    if ("admin".equals(approver.getRole())) return true;

    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;
    User submitter = userMapper.selectById(report.getUserId());
    if (submitter == null) return false;

    String submitterRole = submitter.getRole();
    String submitterDept = submitter.getDeptCode();
    String projectId = report.getProjectId();

    if ("project-manager".equals(submitterRole)
            || "dept-project-manager".equals(submitterRole)
            || isProjectOwner(submitter.getId(), projectId)) {
        return isDeptManagerOf(approverId, submitterDept);
    }

    if (isProjectOwner(approverId, projectId)) return true;
    if (isManagedProject(approverId, projectId)) return true;
    if (isDeptManagerOf(approverId, submitterDept)) return true;
    return false;
}
```

- [ ] **Step 7.3: 新增 `canEditWeeklyReport`**

紧接 canApprove 后追加:

```java
public boolean canEditWeeklyReport(String userId, String reportId) {
    if (isAdmin(userId)) return true;
    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;
    if (!userId.equals(report.getUserId())) return false;       // 仅本人
    String s = report.getStatus();
    return "draft".equals(s) || "rejected".equals(s);
}
```

- [ ] **Step 7.4: 新增 `canDeleteWeeklyReport`**

```java
public boolean canDeleteWeeklyReport(String userId, String reportId) {
    if (isAdmin(userId)) return true;
    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;
    if (!userId.equals(report.getUserId())) return false;
    return "draft".equals(report.getStatus());
}
```

- [ ] **Step 7.5: 新增 `getAccessibleWeeklyReportUserIds`**

放在 `getAccessibleOvertimeUserIds` 同区域(spec §4.3,注意 PM/owner 不重复跑 `selectIdsByOwner`):

```java
public List<String> getAccessibleWeeklyReportUserIds(String userId) {
    User u = userMapper.selectById(userId);
    if (u == null) return Collections.emptyList();
    if ("admin".equals(u.getRole())) return null;        // 不限

    Set<String> ids = new HashSet<>();
    ids.add(userId);                                      // 自己始终能看

    if ("dept-project-manager".equals(u.getRole())) {
        List<String> depts = parseManagedDeptCodes(u);    // 已存在的 helper
        if (!depts.isEmpty()) {
            ids.addAll(userMapper.selectIdsByDeptCodes(depts));
        }
    }
    if ("project-manager".equals(u.getRole())) {
        List<String> pids = parseManagedProjectIds(u);    // 已存在的 helper
        List<String> ownerPids = projectMapper.selectIdsByOwner(userId);
        Set<String> projectIds = new HashSet<>();
        projectIds.addAll(pids);
        projectIds.addAll(ownerPids);
        if (!projectIds.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(
                    new ArrayList<>(projectIds)));
        }
    } else {
        // 非 PM 的普通 owner:role 不是 PM 但有 owner 关系
        // (上面 PM 分支已合并 owner 项目,这里不重复调用 selectIdsByOwner)
        List<String> ownedProjects = projectMapper.selectIdsByOwner(userId);
        if (!ownedProjects.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(ownedProjects));
        }
    }

    return new ArrayList<>(ids);
}
```

imports 补:

```java
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.mapper.WeeklyReportMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
```

- [ ] **Step 7.6: 本地 grep 验证 helper 方法名**

```bash
grep -n "isDeptManagerOf\|parseManagedDeptCodes\|parseManagedProjectIds\|selectIdsByDeptCodes\|selectIdsByOwner\|selectMemberIdsByProjectIds" backend/src/main/java/com/wbs/project/service/PermissionService.java
```
若任何方法名不存在,**以仓库实际签名为准** —— 不要发明新方法;若 helper 命名不同(例如 `isDeptManager` 而非 `isDeptManagerOf`),修改 Step 7.1-7.2 的引用。

---

### Task 8: 改写 `WeeklyReportService`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/WeeklyReportService.java`

- [ ] **Step 8.1: 注入 ApprovalLogService**

`@RequiredArgsConstructor` 已存在,在 fields 区追加:

```java
private final WeeklyReportApprovalLogService approvalLogService;
```

- [ ] **Step 8.2: `approveReport`(L114-130)末尾加 reject comment 必填 + 日志触点**

把方法体改成(spec §5.5、§5.7):

```java
@Transactional
public WeeklyReport approveReport(String id, String approverId, String approveComment, boolean approved) {
    WeeklyReport report = getReportById(id);
    if (report == null) {
        throw new RuntimeException("周报不存在");
    }
    if (!ReportStatus.SUBMITTED.getCode().equals(report.getStatus())) {
        throw new RuntimeException("只能审批已提交状态的周报");
    }
    if (!approved && (approveComment == null || approveComment.trim().isEmpty())) {
        throw new RuntimeException("驳回意见为必填项");
    }
    report.setStatus(approved ? ReportStatus.APPROVED.getCode() : ReportStatus.REJECTED.getCode());
    report.setApproverId(approverId);
    report.setApproveComment(approveComment);
    report.setApproveTime(LocalDateTime.now());
    report.setUpdatedAt(LocalDateTime.now());
    weeklyReportMapper.update(report);

    // 审计日志:try/catch 已在 ApprovalLogService 内部,失败不回滚主流程
    approvalLogService.log(id, approverId, approved ? "approve" : "reject", approveComment);

    return report;
}
```

> `RuntimeException("驳回意见为必填项")` 会落到 `GlobalExceptionHandler`,前端拿到 `Result.error("驳回意见为必填项")`。若仓库已有 `BusinessException(code, message)` 习惯且该 service 其它地方在用,**改成 `throw new BusinessException(400, "驳回意见为必填项")`** 保持一致 —— grep `BusinessException` 看一下用法即可决定。

- [ ] **Step 8.3: 新增 `getReportsByUserIds`**

紧接 `getReportsByProjectId` 后追加:

```java
public List<WeeklyReport> getReportsByUserIds(List<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
        return List.of();
    }
    return weeklyReportMapper.selectByUserIds(userIds);
}
```

---

### Task 9: 改写 `WeeklyReportController`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/controller/WeeklyReportController.java`

- [ ] **Step 9.1: 注入 PermissionService + ApprovalLogService**

类顶部 fields 追加:

```java
private final com.wbs.project.service.PermissionService permissionService;
private final com.wbs.project.service.WeeklyReportApprovalLogService approvalLogService;
```

imports 同步:

```java
import com.wbs.project.dto.WeeklyReportApprovalLogDTO;
import com.wbs.project.service.PermissionService;
import com.wbs.project.service.WeeklyReportApprovalLogService;
```

- [ ] **Step 9.2: 删除 `hasPermission`(L35-48)整段**

直接整段删,后面所有调用点都会换。

- [ ] **Step 9.3: 改写 `getReports`(L50-98)**

把方法体改成:

```java
@GetMapping
public Result<List<WeeklyReport>> getReports(
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);

    List<String> visibleUserIds = currentUserId == null
            ? null
            : permissionService.getAccessibleWeeklyReportUserIds(currentUserId);

    List<WeeklyReport> reports;
    if (visibleUserIds == null) {
        reports = weeklyReportService.getAllReports();              // admin / 兜底
    } else if (visibleUserIds.isEmpty()) {
        reports = java.util.Collections.emptyList();
    } else {
        reports = weeklyReportService.getReportsByUserIds(visibleUserIds);
    }

    if (userId != null) {
        reports = reports.stream().filter(r -> userId.equals(r.getUserId())).toList();
    }
    if (projectId != null) {
        reports = reports.stream().filter(r -> projectId.equals(r.getProjectId())).toList();
    }
    if (status != null) {
        reports = reports.stream().filter(r -> status.equals(r.getStatus())).toList();
    }
    if (startDate != null) {
        reports = reports.stream().filter(r -> !r.getWeekStart().isBefore(startDate)).toList();
    }
    if (endDate != null) {
        reports = reports.stream().filter(r -> !r.getWeekEnd().isAfter(endDate)).toList();
    }
    return Result.success(reports);
}
```

- [ ] **Step 9.4: 改写 `getReportById`(L100-114)**

```java
@GetMapping("/{id}")
public Result<WeeklyReport> getReportById(@PathVariable String id, HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    WeeklyReport report = weeklyReportService.getReportById(id);
    if (report == null) {
        return Result.error("周报不存在");
    }
    if (currentUserId != null && !permissionService.canViewWeeklyReport(currentUserId, id)) {
        return Result.error("您没有权限查看此周报");
    }
    return Result.success(report);
}
```

- [ ] **Step 9.5: 改写 `updateReport`(L132-148)**

```java
@PutMapping("/{id}")
public Result<WeeklyReport> updateReport(@PathVariable String id, @RequestBody WeeklyReport report, HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    WeeklyReport existing = weeklyReportService.getReportById(id);
    if (existing == null) {
        return Result.error("周报不存在");
    }
    if (currentUserId != null && !permissionService.canEditWeeklyReport(currentUserId, id)) {
        return Result.error("您没有权限编辑此周报");
    }
    report.setId(id);
    WeeklyReport updated = weeklyReportService.updateReport(report);
    return Result.success("周报更新成功", updated);
}
```

- [ ] **Step 9.6: 改写 `deleteReport`(L150-165)**

```java
@DeleteMapping("/{id}")
public Result<Void> deleteReport(@PathVariable String id, HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    WeeklyReport existing = weeklyReportService.getReportById(id);
    if (existing == null) {
        return Result.error("周报不存在");
    }
    if (currentUserId != null && !permissionService.canDeleteWeeklyReport(currentUserId, id)) {
        return Result.error("您没有权限删除此周报");
    }
    weeklyReportService.deleteReport(id);
    return Result.success();
}
```

- [ ] **Step 9.7: 改写 `approveReport`(L184-221) —— 删除硬编码白名单**

```java
@PostMapping("/{id}/approve")
public Result<WeeklyReport> approveReport(
        @PathVariable String id,
        @RequestBody ApproveRequest approveRequest,
        HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    if (currentUserId == null) {
        return Result.error("请先登录");
    }
    WeeklyReport existing = weeklyReportService.getReportById(id);
    if (existing == null) {
        return Result.error("周报不存在");
    }
    if (!permissionService.canApproveWeeklyReport(currentUserId, id)) {
        return Result.error("无权审批此周报");
    }
    WeeklyReport approved = weeklyReportService.approveReport(
            id, currentUserId,
            approveRequest.getApproveComment(),
            approveRequest.getApproved()
    );
    String message = approveRequest.getApproved() ? "周报已审批通过" : "周报已拒绝";
    return Result.success(message, approved);
}
```

> 注意:把原本拼在 controller 里的 reject-comment 校验、`project.ownerId == self` 等全部删干净;它们已经在 PermissionService 与 WeeklyReportService 里。

- [ ] **Step 9.8: 新增 `GET /{id}/approval-logs`**

放在 `approveReport` 之后:

```java
@GetMapping("/{id}/approval-logs")
public Result<List<WeeklyReportApprovalLogDTO>> getApprovalLogs(
        @PathVariable String id,
        HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    WeeklyReport existing = weeklyReportService.getReportById(id);
    if (existing == null) {
        return Result.error("周报不存在");
    }
    if (currentUserId != null && !permissionService.canViewWeeklyReport(currentUserId, id)) {
        return Result.error("无权查看此周报的审批记录");
    }
    return Result.success(approvalLogService.listByReport(id));
}
```

- [ ] **Step 9.9: `getProjectReports`(L233 起) / `getMyReports` 等其它端点**

`getMyReports`(L223-231)只校验 `currentUserId.equals(userId)`,**保持不变**。

`getProjectReports`(L233 起)目前也用 `hasPermission`,**改成走 `permissionService.canViewWeeklyReport(currentUserId, report.getId())`**:把现有 stream filter 替换为 ——

```java
@GetMapping("/project/{projectId}")
public Result<List<WeeklyReport>> getProjectReports(@PathVariable String projectId, HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    List<WeeklyReport> reports = weeklyReportService.getReportsByProjectId(projectId);
    if (currentUserId != null) {
        final String uid = currentUserId;
        reports = reports.stream()
                .filter(r -> permissionService.canViewWeeklyReport(uid, r.getId()))
                .toList();
    }
    return Result.success(reports);
}
```

> 实际写时:**先 Read 这个方法的当前完整内容**,确认要替换的内联 if/else 范围,再 Edit。

---

### Task 10: 后端编译验证

- [ ] **Step 10.1: 跑 `mvn clean install -DskipTests`**

```bash
cd C:/Users/sunru/Desktop/其他/wbs-project-final/backend
mvn clean install -DskipTests
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 10.2: 若失败 → 修复**

常见失败:
- `cannot find symbol method isDeptManagerOf` → grep 实际方法名,改 Step 7.1-7.2
- `WeeklyReportMapper.selectByUserIds is undefined` → 检查 Step 6.1 是否漏写
- Lombok 报错 → 检查 entity 是否漏 `@Data`

修完再跑直至 BUILD SUCCESS。

---

### Task 11: COMMIT GATE ②

- [ ] **Step 11.1: 整理 commit ② 范围**

```bash
git status
```

应只含 backend/ 下与本期相关的新增 / 修改文件。**不应**有 frontend 改动。

- [ ] **Step 11.2: 向用户展示 commit ② message**

```
feat(weekly-report): PermissionService 5 档判定 + Controller 收口 + 审批日志

- PermissionService 新增 canView/canEdit/canDelete/canApprove + getAccessible
  WeeklyReportUserIds,审批 5 档判定与 OvertimeService.validateApprover 同形
  (admin / 同部门 dept-pm / project-owner / 项目内 PM / submitter dept-pm 兜底)
- WeeklyReportController 撕掉硬编码白名单与 Java 内存 hasPermission 过滤,
  全部走 PermissionService;新增 GET /{id}/approval-logs
- WeeklyReportService.approveReport 加 reject comment 必填 + 审批日志触点
  (try/catch 在 ApprovalLogService 内部,失败不回滚主流程)
- 新增 WeeklyReportApprovalLog entity/dto/mapper/service,与加班审批日志对称
- WeeklyReportMapper 新增 selectByUserIds,以 IN 过滤替代 Java 内存 stream

Spec: docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md §4-§5
```

- [ ] **Step 11.3: 暂停等用户明确说"提交"**

只有用户回复"提交"才执行 `git add backend/` + `git commit -m ...`。

---

### Task 12: 前端 —— 类型 + API

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/services/api.ts`

- [ ] **Step 12.1: `types/index.ts` 加 `WeeklyReportApprovalLog`**

文件末尾或与 `WeeklyReport` 相邻位置追加(spec §6.8):

```ts
export interface WeeklyReportApprovalLog {
  id: number                    // 日志表自增 PK
  reportId: string              // 对齐 WeeklyReport.id (String)
  approverId: string
  approverName: string
  approverAvatar?: string
  approverRole: 'admin' | 'dept-project-manager' | 'project-manager' | 'project-owner'
  action: 'approve' | 'reject'
  comment?: string
  createdAt: string
}
```

- [ ] **Step 12.2: `services/api.ts` 加 `getApprovalLogs`**

在 `api.ts` 的 weeklyReport 命名空间(或同等 weekly-report 模块)追加 ——

```ts
async getApprovalLogs(reportId: string): Promise<WeeklyReportApprovalLog[]> {
  return request<WeeklyReportApprovalLog[]>(
    'GET',
    `/weekly-reports/${reportId}/approval-logs`,
  )
}
```

> 先 Read `api.ts` 找到现有 `getApprovalLogs` 的命名空间风格(可能是 `weeklyReportApi`, `apiService.weeklyReport`, 或顶层函数),按现有模式追加,不要发明新模式。`import { WeeklyReportApprovalLog } from '@/types'` 在文件顶补。

---

### Task 13: 前端 —— `permission` store 重写

**Files:**
- Modify: `frontend/src/stores/permission.ts`

- [ ] **Step 13.1: 删除现有 `canViewWeeklyReport` / `canApproveWeeklyReport`**

L310-312 附近的旧实现(`canEditProject` 派生)整段替换。

- [ ] **Step 13.2: 写入 4 个新方法 + 类型**

(spec §6.2 完整)。在 `permission.ts` 顶部加类型:

```ts
import type { ReportStatus } from '@/types'

interface WeeklyReportPermissionInput {
  id: string
  userId: string
  projectId: string
  status: ReportStatus
}
```

把 4 个方法写到 `canApproveOvertime` 附近(保持与加班权限方法的就近放置):

```ts
function canViewWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (currentRole.value === 'admin') return true
  if (report.userId === userStore.user.id) return true

  const submitter = userStore.userById(report.userId)
  if (isProjectOwner(userStore.user.id, report.projectId)) return true
  if (isManagedProject(userStore.user.id, report.projectId)) return true
  if (isDeptManagerOf(userStore.user.id, submitter?.deptCode)) return true
  return false
}

function canApproveWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (report.status !== 'submitted') return false
  if (report.userId === userStore.user.id) return false
  if (currentRole.value === 'admin') return true

  const submitter = userStore.userById(report.userId)
  const submitterRole = submitter?.role
  if (submitterRole === 'project-manager'
      || submitterRole === 'dept-project-manager'
      || isProjectOwner(report.userId, report.projectId)) {
    return isDeptManagerOf(userStore.user.id, submitter?.deptCode)
  }
  if (isProjectOwner(userStore.user.id, report.projectId)) return true
  if (isManagedProject(userStore.user.id, report.projectId)) return true
  if (isDeptManagerOf(userStore.user.id, submitter?.deptCode)) return true
  return false
}

function canEditWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (currentRole.value === 'admin') return true
  if (report.userId !== userStore.user.id) return false
  return report.status === 'draft' || report.status === 'rejected'
}

function canDeleteWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (currentRole.value === 'admin') return true
  if (report.userId !== userStore.user.id) return false
  return report.status === 'draft'
}
```

注意 `isProjectOwner` / `isManagedProject` / `isDeptManagerOf` 是同文件已有 helper —— 先 grep 确认实际命名;若名称不同,以仓库实际为准修改调用。

- [ ] **Step 13.3: 在 store return 里暴露 4 个新方法**

确保 `return { ... canViewWeeklyReport, canApproveWeeklyReport, canEditWeeklyReport, canDeleteWeeklyReport }`。

---

### Task 14: 前端 —— `weeklyReport` store 加 user/role 切换 watch

**Files:**
- Modify: `frontend/src/stores/weeklyReport.ts`

- [ ] **Step 14.1: 顶部 import 与 store 引用**

```ts
import { watch } from 'vue'
import { useUserStore } from './user'
import { usePermissionStore } from './permission'
```

- [ ] **Step 14.2: 在 store setup 内加 watch**

```ts
const userStore = useUserStore()
const permissionStore = usePermissionStore()

watch(
  () => `${userStore.user?.id ?? ''}|${permissionStore.currentRole ?? ''}`,
  () => { fetchReports() },
  { immediate: false }
)
```

> `fetchReports` 是该 store 中已有的拉取方法 —— 若实际命名是 `loadReports` / `getReports`,按实际改。

---

### Task 15: 前端 —— 列表 / 详情 / 卡片 / 评论 改写

**Files:**
- Modify: `frontend/src/views/WeeklyReports.vue`
- Modify: `frontend/src/views/WeeklyReportDetail.vue`
- Modify: `frontend/src/components/weeklyReport/WeeklyReportCard.vue`
- Modify: `frontend/src/components/weeklyReport/WeeklyReportComment.vue`

- [ ] **Step 15.1: `WeeklyReports.vue` 删除前端二次角色过滤**

找到 L271-290 附近的 `filteredReports` computed,把按角色字面量再 filter 的分支删干净(spec §6.3),保留 status / keyword UI filter:

```ts
const filteredReports = computed(() => {
  let list = store.reports
  if (filters.value.status) {
    list = list.filter(r => r.status === filters.value.status)
  }
  if (filters.value.keyword) {
    list = list.filter(r => matchKeyword(r, filters.value.keyword))
  }
  return list
})
```

如果 `matchKeyword` 现实里是 inline 比较,保留 inline,不要造新函数。

- [ ] **Step 15.2: `WeeklyReportCard.vue` 改走 permission store**

把 L81-92 附近的 `canEdit` / `canDelete` / `canApprove` 全部替换为(spec §6.4):

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { usePermissionStore } from '@/stores/permission'

const props = defineProps<{ report: WeeklyReport }>()
const perm = usePermissionStore()

const canEdit    = computed(() => perm.canEditWeeklyReport(props.report))
const canDelete  = computed(() => perm.canDeleteWeeklyReport(props.report))
const canApprove = computed(() => perm.canApproveWeeklyReport(props.report))
</script>
```

模板里 `v-if="canEdit"` / `v-if="canDelete"` / `v-if="canApprove"` 不动。

- [ ] **Step 15.3: `WeeklyReportDetail.vue` 删内联 `canApprove`**

L298-302 的内联 `canApprove` computed 整段删,改成引用 `permissionStore.canApproveWeeklyReport(report.value)`。

同时,在详情页正文区底部、评论区上方,挂载新组件:

```vue
<WeeklyReportApprovalLog :reportId="report.id" />
```

`import WeeklyReportApprovalLog from '@/components/weeklyReport/WeeklyReportApprovalLog.vue'` 在 script 顶部加。

- [ ] **Step 15.4: `WeeklyReportComment.vue.canDelete` 调整**

L58-60 的 `canDelete` 改为:

```ts
const canDelete = computed(() => {
  if (!user.value) return false
  if (currentRole.value === 'admin') return true
  if (comment.userId === user.value.id) return true       // 自己写的评论
  return perm.canViewWeeklyReport(report)                  // 否则:本周报对当前用户可见 → 可删除自己评论以外的?保守按"否"
})
```

> 实际策略遵循 spec §3.1 矩阵"评论权限按可见范围";具体写法可能要根据现有 props 调整 —— 若组件接收不到 `report`,只接收 `comment`,那"判断当前用户是否对该周报可见"需要走 API 或 store。**简化:** 保持 admin + 自己评论可删,**其他人评论一律不能删**(与现状相比仅去除"删别人评论"的入口,不引入新可见性 API);如未来需要补"可见者可删全部"再扩展。

最终改成:

```ts
const canDelete = computed(() => {
  if (!user.value) return false
  if (currentRole.value === 'admin') return true
  return comment.userId === user.value.id
})
```

---

### Task 16: 前端 —— 新建 `WeeklyReportApprovalLog.vue`

**Files:**
- Create: `frontend/src/components/weeklyReport/WeeklyReportApprovalLog.vue`

- [ ] **Step 16.1: 复刻 OvertimeApprovalLog 的形态**

先快速 Read `frontend/src/components/overtime/OvertimeApprovalLog.vue`(若存在),按它的 template / script 结构改造为周报版。

- [ ] **Step 16.2: 写入组件**

```vue
<template>
  <div v-if="logs.length" class="approval-log mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded">
    <h3 class="text-base font-semibold mb-3">{{ $t('weeklyReports.approvalHistory') }}</h3>
    <div v-for="row in logs" :key="row.id" class="log-row flex items-start gap-3 mb-3">
      <img v-if="row.approverAvatar" :src="row.approverAvatar" class="w-8 h-8 rounded-full" />
      <div class="flex-1">
        <div class="flex items-center gap-2 flex-wrap text-sm">
          <span class="font-medium">{{ row.approverName }}</span>
          <span class="px-2 py-0.5 rounded text-xs bg-blue-100 text-blue-700">
            {{ $t('roles.' + row.approverRole.replaceAll('-', '_')) }}
          </span>
          <span
            class="px-2 py-0.5 rounded text-xs"
            :class="row.action === 'approve' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'"
          >
            {{ $t('weeklyReports.action.' + row.action) }}
          </span>
          <time class="text-xs text-gray-500">{{ formatDateTime(row.createdAt) }}</time>
        </div>
        <p v-if="row.comment" class="text-sm text-gray-700 dark:text-gray-300 mt-1">{{ row.comment }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import apiService from '@/services/api'
import type { WeeklyReportApprovalLog } from '@/types'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ reportId: string }>()
const logs = ref<WeeklyReportApprovalLog[]>([])

async function load() {
  if (!props.reportId) return
  try {
    logs.value = await apiService.weeklyReport.getApprovalLogs(props.reportId)
  } catch (e) {
    logs.value = []
  }
}

onMounted(load)
watch(() => props.reportId, load)
</script>
```

> `apiService.weeklyReport.getApprovalLogs` 的访问路径以 Task 12 的 api.ts 实际写法为准。`formatDateTime` 若仓库中不存在,改用 `new Date(row.createdAt).toLocaleString()`;先 grep `formatDateTime` 看实际位置。

---

### Task 17: 前端 —— i18n + constants

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`
- Modify: `frontend/src/utils/constants.ts`

- [ ] **Step 17.1: 加 i18n keys**

在 `frontend/src/i18n/locales/zh.ts` 的 `weeklyReports` 命名空间下追加:

```ts
approvalHistory: '审批记录',
action: {
  approve: '通过',
  reject: '驳回',
},
```

如果 `roles` 命名空间不存在 `project_owner`,补一行:

```ts
roles: {
  // ... 已有
  project_owner: '项目负责人',
}
```

ko / en 文件**本期不动**(spec §7 freeze)。

- [ ] **Step 17.2: `USER_ROLE_OPTIONS` 补 dept-pm**

`frontend/src/utils/constants.ts` L28-33 替换为:

```ts
export const USER_ROLE_OPTIONS = [
  { value: 'admin',                  labelKey: 'roles.admin' },
  { value: 'dept-project-manager',   labelKey: 'roles.dept_project_manager' },
  { value: 'project-manager',        labelKey: 'roles.project_manager' },
  { value: 'member',                 labelKey: 'roles.member' },
  { value: 'viewer',                 labelKey: 'roles.viewer' },
]
```

---

### Task 18: 前端编译验证

- [ ] **Step 18.1: 跑 `npx vue-tsc`**

```bash
cd C:/Users/sunru/Desktop/其他/wbs-project-final/frontend
npx vue-tsc --noEmit
```

Expected: 0 errors。

- [ ] **Step 18.2: 跑 `npm run build`**

```bash
npm run build
```

Expected: `vite build` 完成,有 `dist/` 输出。

- [ ] **Step 18.3: 若有 TS 错误 → 修复并重跑**

常见:
- `Property 'userById' does not exist` → 检查 `stores/user.ts` 是否真的 export 了 `userById`(应该有,L92)
- `Property 'weeklyReport' does not exist on apiService` → 与 Task 12 实际 api.ts 模式不一致
- `Type 'string | undefined' is not assignable to ...` → 给 helper 加 `?? false`

**注意**:`frontend/dist/index.html` 在 build 后会变动,但**不要 stage**(CLAUDE.md)。

---

### Task 19: COMMIT GATE ③

- [ ] **Step 19.1: 整理 commit ③ 范围**

```bash
git status
```

应只含 frontend/src/ 下与本期相关的改动。**不要 stage** `frontend/dist/index.html`。

- [ ] **Step 19.2: 向用户展示 commit ③ message**

```
feat(weekly-report): 前端口径统一 + 审批日志面板

- permission store 重写 canView/canApprove,新增 canEdit/canDelete WeeklyReport,
  4 个方法接收 WeeklyReportPermissionInput,与后端 5 档判定同形
- WeeklyReports.vue 删除前端二次角色 filter(数据已被后端按
  getAccessibleWeeklyReportUserIds 过滤),仅保留 status / keyword UI filter
- WeeklyReportCard/Detail/Comment 删除内联硬编码,统一走 permissionStore
- 新组件 WeeklyReportApprovalLog.vue,详情页拉 GET /{id}/approval-logs 展示
  审批历史(含审批人角色快照)
- types 加 WeeklyReportApprovalLog;api.ts 加 weeklyReport.getApprovalLogs
- i18n/zh 加 approvalHistory / action.approve / action.reject / roles.project_owner
- USER_ROLE_OPTIONS 补 dept-project-manager(原漏)
- stores/weeklyReport 加 user/role 切换 watch 重拉

Spec: docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md §6
```

- [ ] **Step 19.3: 暂停等用户明确说"提交"**

只有用户回复"提交"才执行 `git add frontend/src/ frontend/src/...` + `git commit -m ...`。

---

### Task 20: COMMIT GATE ④ —— 归档 spec + plan

- [ ] **Step 20.1: 确认两份 doc 已在工作树**

```bash
git status -- docs/superpowers/
```

应看到:
- `docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md` (untracked)
- `docs/superpowers/plans/2026-06-14-weekly-report-roles.md` (untracked, 本文档)

- [ ] **Step 20.2: 向用户展示 commit ④ message**

```
docs(spec): 周报 4 角色数据可见 + 审批口径对齐 设计与实施计划

- spec(§1-§10):背景、4 角色矩阵、5 档审批判定、审批日志表、前端口径统一、
  验证 / 风险 / 实施顺序 / 决策日志,附与加班模块的对照表
- plan: Task 1-21,按 4 个 commit 粒度组织,所有 commit gate 严守
  CLAUDE.md「不自动提交」规则;验证靠 mvn build + vue-tsc + npm run build
  + 手工 SQL 角色账号脚本

Spec 经 2 轮 spec-document-reviewer 评审通过
```

- [ ] **Step 20.3: 暂停等用户明确说"提交"**

```bash
git add docs/superpowers/
git commit -m "docs(spec): 周报 4 角色数据可见 + 审批口径对齐 设计与实施计划
..."
```

---

### Task 21: 手工 smoke 验证 runbook(交付给用户)

> 这部分由**用户**在他本地完成,plan 仅提供 runbook。子代理执行到这里就停。

- [ ] **Step 21.1: 准备 6 个测试账号**

打开 MySQL 客户端,执行(同 spec §8.1):

```sql
-- 注意:这些账号仅用于本地 smoke;勿提交到生产
INSERT INTO sys_user (id, name, role, dept_code, managed_dept_codes, managed_project_ids, status)
VALUES
  ('T0000001', 'T_ADMIN',  'admin',                 NULL,     NULL,            NULL,           'C'),
  ('T0000002', 'T_DEPT_A', 'dept-project-manager',  'DEPT_A', '["DEPT_A"]',    NULL,           'C'),
  ('T0000003', 'T_DEPT_B', 'dept-project-manager',  'DEPT_B', '["DEPT_B"]',    NULL,           'C'),
  ('T0000004', 'T_PM1',    'project-manager',       'DEPT_A', NULL,            '["P001"]',     'C'),
  ('T0000005', 'T_OWNER',  'member',                'DEPT_A', NULL,            NULL,           'C'),
  ('T0000006', 'T_MEMBER', 'member',                'DEPT_A', NULL,            NULL,           'C');

-- 项目 / 成员视具体 schema 补:
-- P001 / P002 / P003 三个项目,owner 分别为 ?/T_PM1/T_OWNER
-- T_PM1 / T_OWNER / T_MEMBER 加进相应项目的 sys_project_member
-- 几条周报(T_MEMBER 在 P003、T_PM1 在 P001)
```

- [ ] **Step 21.2: 跑通 spec §8.2 的 10 个场景**

由用户启动后端 + 前端,按 spec §8.2 表手工验证:可见范围 / dept-pm 审批 PM / 自审拒绝 / 跨部门不可见 / 驳回 comment 必填 / 审批日志 API 权限 / 按钮显隐 / 不变项回归 / EXPLAIN 走索引。

- [ ] **Step 21.3: 出现 bug → 反馈,不要直接改**

任何场景不通过,把症状回报用户,由用户决定回滚 / 修复路径(不在本 plan 自动修复)。

---

## 附录:常见踩坑预案

| 现象 | 排查 |
|---|---|
| `mvn` 报 `cannot find symbol method isDeptManagerOf` | grep `PermissionService.java` 中实际的"判定 dept-pm 是否管辖该部门"方法名;按实际改 Task 7 引用 |
| `vue-tsc` 报 `userById` 不存在 | 看 `stores/user.ts:92-94`,确认 export 暴露;Task 13 的调用必须与 store export 一致 |
| 后端启动后所有 dept-pm 仍看不到部门周报 | 大概率是 `parseManagedDeptCodes(u)` 解析 JSON 失败 → 把 user 的 `managed_dept_codes` 列打印出来确认 JSON 格式 |
| 审批日志表写不进去 | DDL 是否执行了(`SHOW TABLES LIKE 'sys_weekly_report_approval_log'`);若无,在 MySQL 客户端跑 `add_weekly_report_approval_log_table.sql` |
| 前端按钮该显示但没出 | 该 user 的 `submitter` 在前端缓存里没命中 → 刷新 `team` 页 / 重新登录 |

## 附录:与加班模块对照(执行参考)

| 维度 | 加班(已有) | 周报(本期实现) |
|---|---|---|
| 5 档判定方法 | `OvertimeService.validateApprover:422-479` | `PermissionService.canApproveWeeklyReport`(Task 7.2) |
| 数据范围方法 | `PermissionService.getAccessibleOvertimeUserIds:732-780` | `PermissionService.getAccessibleWeeklyReportUserIds`(Task 7.5) |
| 审批日志表 | `t_overtime_approval_log` | `sys_weekly_report_approval_log`(Task 1) |
| 角色快照方法 | `OvertimeService.resolveApproverRole:376-388` | `WeeklyReportApprovalLogService.resolveApproverRole`(Task 5.1) |
| 详情页面板 | `components/overtime/OvertimeApprovalLog.vue` | `components/weeklyReport/WeeklyReportApprovalLog.vue`(Task 16) |
| 控制器入口 | 走 `validateApprover` | 走 `canApproveWeeklyReport`(Task 9.7) |
| reject comment 必填 | `OvertimeService.approveRecord:196` | `WeeklyReportService.approveReport`(Task 8.2) |

---

**Plan 完成。等用户启动 `superpowers:subagent-driven-development` 或人工按 Task 顺序执行。**
