# 周报管理 —— 4 角色数据可见范围 + 审批链路对齐 设计文档

| 字段 | 值 |
|---|---|
| 日期 | 2026-06-14 |
| 作者 | sunruoqin10(用户)+ Claude 协作 |
| 状态 | 待用户复核 → 待 spec-reviewer 评审 |
| 适用版本 | `wbs-project-final` master 分支(基于 commit `086002a`) |
| 关联模块 | 周报(WeeklyReport)、权限(PermissionService)、加班(参考模板) |
| Scope 关键字 | 数据可见范围分层 / 审批口径放开 / 审批日志审计 |

---

## 1. 背景

### 1.1 现状问题

- **现有 5 个角色**(`UserRole.java:17-21`):`admin / dept-project-manager / project-manager / member / viewer`;但**周报模块只对 `admin` 和 `project-manager` 有专门处理**。
- **审批口径 4 处不一致**:
  - `WeeklyReportController.approveReport`(L184-220):硬编码 `admin || project-manager` 且 `project.ownerId == self`,**直接拒绝 dept-pm**;
  - `PermissionService.canApproveWeeklyReport`(L351-360):走 `canEditProject(userId, projectId)`,实际放行的范围**比单纯"dept-pm"更宽** —— 还包含 `isManagedProject` 命中的 PM、项目创建者(`createdBy`)、项目 owner。换言之**今天 dept-pm 已被 Service 层放行,只是 Controller 层拒了**;
  - `views/WeeklyReportDetail.vue:298-302`:硬编码 `admin / project-manager`;
  - `stores/permission.ts:310-312`:`canEditProject(projectId)` 派生;
- **数据可见过滤靠 Java 内存 stream**(`WeeklyReportController.getReports`):非 admin/PM 取全表后 filter,量上来直接崩。
- **dept-pm 无法用周报模块管理本部门人员**:列表里看不到本部门成员的周报。
- **加班模块(`OvertimeService.validateApprover`)已迭代 7 轮收敛**为成熟的 5 档审批判定 + 审批日志表 + 审批角色快照,可直接照搬。

### 1.2 业务诉求(锁定的 5 个决策)

| # | 决策 | 选项 |
|---|------|------|
| 1 | 目标范围 | 数据可见范围按 4 角色分层 |
| 2 | dept-pm 范围口径 | 按 `submitter.dept_code`(同加班) |
| 3 | PM / owner 范围口径 | `managed_project_ids ∪ owner 项目` 并集 |
| 4 | 项目成员可见范围 | 仅自己 |
| 5 | 审批联动 | 可见 + 审批 一套放开 |

### 1.3 不在本期范围

详见 §7「不做清单」。

---

## 2. 角色识别口径

> "4 角色"是用户口径(部门项目负责人 / 项目经理 / 项目负责人 / 项目成员),**仓库中并非都是 `role` 字段**:

| 用户口径 | 仓库判定 |
|---|---|
| 部门项目负责人 | `user.role == 'dept-project-manager'` 且 `JSON_CONTAINS(user.managed_dept_codes, X)` |
| 项目经理 | `user.role == 'project-manager'` 且 `JSON_CONTAINS(user.managed_project_ids, P)` |
| 项目负责人 | `project.owner_id == user.id`(关系,不依赖 role) |
| 项目成员 | `sys_project_member` 行存在 `(P, user.id)`(关系,不依赖 role) |

`viewer` 角色在本设计中**视同 `member`**(只看自己;无审批/编辑/删除权)。

---

## 3. 权限矩阵(契约)

### 3.1 操作矩阵

| 操作 \\ 身份 | admin | 部门项目负责人(dept-pm) | 项目经理(PM) | 项目负责人(owner) | 项目成员(member/viewer) |
|---|---|---|---|---|---|
| **查看列表** | 全部(草稿除外,见下) | `submitter.dept_code ∈ managed_dept_codes`(草稿除外) | `report.project_id ∈ managed_project_ids` ∪ `project.owner_id == self` 项目下所有周报(草稿除外) | owner 自己项目下所有周报(草稿除外) | 仅自己(含自己所有草稿) |
| **查看详情** | ✅(草稿除外) | ✅(同范围,草稿除外) | ✅(同范围,草稿除外) | ✅(同范围,草稿除外) | 仅自己(含自己所有草稿) |
| **草稿状态补充规则(2026-06-14)** | ❌ 看不到别人草稿 | ❌ 看不到别人草稿 | ❌ 看不到别人草稿 | ❌ 看不到别人草稿 | 仅自己 |
| **审批 / 驳回** | ✅ | ✅(同范围) | ✅(同范围) | ✅(同范围) | ❌ |
| **编辑** | ✅ | ❌ | ❌ | ❌ | 仅自己 + `status ∈ {draft, rejected}` |
| **删除** | ✅ | ❌ | ❌ | ❌ | 仅自己 + `status == draft` |
| **评论** | ✅ | ✅(范围内) | ✅(范围内) | ✅(范围内) | 仅自己的周报 |
| **创建 / 提交** | ✅ | ✅(自己也要交) | ✅(自己也要交) | ✅(自己也要交) | ✅ |

### 3.2 自己的周报谁审 —— PM / owner / dept-pm 提交者特殊分支

照搬加班 `OvertimeService.validateApprover:439-449` 的逻辑,防自审 + PM 互批:

| 提交者身份 | 谁能审 |
|---|---|
| 普通成员(`role ∈ {member, viewer}` 且非 owner) | admin / 同部门 dept-pm / 项目 owner / 项目内 PM |
| 项目经理(`role == 'project-manager'`) | admin / **仅** 同部门 dept-pm |
| 项目负责人(`isProjectOwner == true`,非 PM/dept-pm 身份) | admin / 同部门 dept-pm / **项目内 PM**(via `managed_project_ids`) |
| 部门项目负责人(`role == 'dept-project-manager'`) | admin / 其他同部门 dept-pm(若部门只有一个 dept-pm → 仅 admin) |
| **任何身份提交者(包括 admin)** | **任何人都不能审批自己提交的周报**(防自审) |

### 3.3 不变项(scope freeze)

- 状态机仍为 `draft → submitted → approved/rejected`,不加 `withdrawn / overdue_draft`;
- 不加 `dept_code / task_id` 主表冗余列;
- 不加未交周报提醒邮件 / 部门交付率看板;
- 现有"被驳回后允许再编辑"行为保留。

---

## 4. 后端实现

### 4.1 `PermissionService` 新增 / 重写的方法

> **类型口径**:`WeeklyReport.id` 是 `String`(`entity/WeeklyReport.java:11`),不是 `Long`。下文所有 `reportId` 一律 `String`;新增 `sys_weekly_report_approval_log.report_id` 列也用 `VARCHAR(64)` 保持类型一致。

```java
// service/PermissionService.java —— 重写现有 2 个 + 新增 3 个

public boolean canViewWeeklyReport(String userId, String reportId);    // 重写(替换 L343-346 内的 canViewProject 派生)
public boolean canApproveWeeklyReport(String userId, String reportId); // 重写(替换 L351-360 内的 canEditProject 派生)
public boolean canEditWeeklyReport(String userId, String reportId);    // 新增
public boolean canDeleteWeeklyReport(String userId, String reportId);  // 新增
public List<String> getAccessibleWeeklyReportUserIds(String userId);   // 新增(返回 List 而非 Set:① 便于直接喂给 MyBatis IN foreach;② 调用方需要稳定顺序时不再二次转换)
```

> **关于 `getAccessibleWeeklyReportUserIds` 与现有 `getAccessibleOvertimeUserIds`(L732-780)的关系**:本期采用 **clone-then-customize** —— 复制一份独立方法,不抽公共基座;后期周报/加班/请假的第三个审批场景出现时,再统一抽 `AccessibleSubmitterResolver` 公共基座。理由:① 两者业务边界目前还允许微调(例如未来可能给周报增加"创建者也可见"分支),提前抽象会过早绑死;② 抽象层不在本期 scope。两个方法相似度高的代价由【Section 7 不做清单】中"公共基座"条目兜住。

### 4.1.a `canViewWeeklyReport` 草稿私密性(2026-06-14 增补)

```java
public boolean canViewWeeklyReport(String userId, String reportId) {
    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;

    // 2026-06-14 规则:草稿状态对 creator 之外的人不可见(严格解读,admin 也受此约束)
    if ("draft".equals(report.getStatus())) {
        return userId != null && userId.equals(report.getUserId());
    }

    // 非草稿:admin 放行 + creator 放行 + 数据范围
    if (isAdmin(userId)) return true;
    if (userId != null && userId.equals(report.getUserId())) return true;

    User submitter = userMapper.selectById(report.getUserId());
    if (submitter == null) return false;
    String submitterDept = submitter.getDeptCode();
    String projectId = report.getProjectId();

    if (isProjectOwner(userId, projectId)) return true;
    if (isManagedProject(userId, projectId)) return true;
    if (isDeptManager(userId, submitterDept)) return true;
    return false;
}
```

> 列表端在 `WeeklyReportController.getReports` 还需**加一道 post-filter**(因为 `getAccessibleWeeklyReportUserIds` 返回的是 user 集合,无法在 SQL 层做状态过滤):
> ```java
> if (currentUserId != null) {
>     reports = reports.stream()
>         .filter(r -> !"draft".equals(r.getStatus()) || currentUserId.equals(r.getUserId()))
>         .toList();
> }
> ```

### 4.2 `canApproveWeeklyReport` 5 档判定骨架

```java
public boolean canApproveWeeklyReport(String approverId, String reportId) {
    User approver = userMapper.selectById(approverId);
    if (approver == null) return false;
    if ("admin".equals(approver.getRole())) return true;     // ① admin 任意放行

    WeeklyReport report = weeklyReportMapper.selectById(reportId);
    if (report == null) return false;

    // 防自审:任何身份都不能审批自己提交的周报
    if (approverId.equals(report.getUserId())) return false;

    User submitter = userMapper.selectById(report.getUserId());
    if (submitter == null) return false;

    String submitterRole = submitter.getRole();
    String submitterDept = submitter.getDeptCode();
    String projectId = report.getProjectId();

    // ② 提交者是 PM / dept-pm → 仅同部门 dept-pm 可批(防 PM 互批)
    //   注意:这是相对今天 canEditProject 派生口径的「收紧」 ——
    //   今天 PM 互批 实际上可能被 canEditProject 放过,新规则一刀切。
    if ("project-manager".equals(submitterRole)
            || "dept-project-manager".equals(submitterRole)) {
        return isDeptManagerOf(approverId, submitterDept);
    }

    // ②-bis(2026-06-14 调整):提交者是项目 owner(非 PM/dept-pm 身份)
    //   → 同部门 dept-pm 或 项目内 PM(via managed_project_ids)可批
    if (isProjectOwner(submitter.getId(), projectId)) {
        if (isDeptManagerOf(approverId, submitterDept)) return true;
        if (isManagedProject(approverId, projectId)) return true;
        return false;
    }

    // ③ 项目 owner
    if (isProjectOwner(approverId, projectId)) return true;

    // ④ PM 通过 managed_project_ids 管辖
    if (isManagedProject(approverId, projectId)) return true;

    // ⑤ dept-pm 兜底(按 submitter.dept_code)
    if (isDeptManagerOf(approverId, submitterDept)) return true;

    return false;
}
```

> 与 `OvertimeService.validateApprover:431-477` 接近,但在 owner 提交场景上**周报放宽于加班**:周报允许项目内 PM 批 owner 的周报,加班仍是仅 dept-pm。这是 2026-06-14 在实际跑通后由业务方提出的差异化要求。
>
> **相对现状的行为差**:
> - 现状(`canEditProject` 派生):dept-pm / 项目 owner / PM(via `isManagedProject`)/ 项目创建者 都能审;且 **可能允许 PM 互批 / owner 自审**(因 `canEditProject` 不区分提交者身份)。
> - 新规则:**收紧** PM 互批 + admin/PM/owner 自审场景,**放宽** dept-pm 由"被 Controller 层拒"变为"明确放行";**owner 提交的周报项目内 PM 也可批**(2026-06-14 调整)。

### 4.3 `getAccessibleWeeklyReportUserIds` 实现骨架

> mapper 方法名以**仓库实际签名**为准:`userMapper.selectIdsByDeptCodes(List<String>)`、`projectMapper.selectIdsByOwner(String)`、`projectMemberMapper.selectMemberIdsByProjectIds(List<String>)`(均见 `PermissionService.getAccessibleOvertimeUserIds:732-780`)。无须新增 mapper 方法。

```java
public List<String> getAccessibleWeeklyReportUserIds(String userId) {
    User u = userMapper.selectById(userId);
    if (u == null) return Collections.emptyList();
    if ("admin".equals(u.getRole())) return null;            // 不限

    Set<String> ids = new HashSet<>();
    ids.add(userId);                                          // 自己始终能看

    if ("dept-project-manager".equals(u.getRole())) {
        List<String> depts = parseManagedDeptCodes(u);
        if (!depts.isEmpty()) {
            ids.addAll(userMapper.selectIdsByDeptCodes(depts));    // 同加班口径
        }
    }
    if ("project-manager".equals(u.getRole())) {
        List<String> pids = parseManagedProjectIds(u);
        List<String> ownerPids = projectMapper.selectIdsByOwner(userId);
        Set<String> projectIds = new HashSet<>();
        projectIds.addAll(pids);
        projectIds.addAll(ownerPids);
        if (!projectIds.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(
                    new ArrayList<>(projectIds)));
        }
    } else {
        // 非 PM 的普通 owner —— role 不是 PM 但有 owner 关系(兼容);
        // 跳过 PM 分支(上面已经把 owner 项目并进 projectIds),避免 selectIdsByOwner 被调用两次
        List<String> ownedProjects = projectMapper.selectIdsByOwner(userId);
        if (!ownedProjects.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(ownedProjects));
        }
    }

    return new ArrayList<>(ids);
}
```

### 4.4 Mapper / SQL 新增

```xml
<!-- WeeklyReportMapper.xml -->
<select id="selectByUserIds" resultMap="WeeklyReportMap">
    SELECT * FROM sys_weekly_report
    WHERE user_id IN
    <foreach collection="userIds" item="uid" open="(" separator="," close=")">
        #{uid}
    </foreach>
    ORDER BY week_start DESC, created_at DESC
</select>
```

建议同步建索引(若未存在):

```sql
CREATE INDEX idx_user_week    ON sys_weekly_report(user_id, week_start);
CREATE INDEX idx_project_stat ON sys_weekly_report(project_id, status);
```

无须新增 mapper 方法 —— `selectIdsByDeptCodes` / `selectIdsByOwner` / `selectMemberIdsByProjectIds` 均已存在(`PermissionService.getAccessibleOvertimeUserIds` 同款调用)。仅 `WeeklyReportMapper.selectByUserIds` 是新的。

### 4.5 Controller 改造(`WeeklyReportController`)

> **`currentUserId` 取值方式**:仍沿用现有 `WeeklyReportController.getCurrentUserId()`(L32:`(String) request.getAttribute("userId")`,值由 `AuthInterceptor` 注入),不引入 `@RequestHeader("X-User-Id")`,与本控制器其余方法风格一致。

**审批入口**:

```java
String currentUserId = getCurrentUserId(request);
if (!permissionService.canApproveWeeklyReport(currentUserId, reportId)) {
    return Result.error(403, "无权审批此周报");
}
```

**列表入口**:

```java
List<String> visibleUserIds = permissionService.getAccessibleWeeklyReportUserIds(currentUserId);
List<WeeklyReport> reports;
if (visibleUserIds == null) {
    reports = weeklyReportService.getAllReports();           // admin
} else if (visibleUserIds.isEmpty()) {
    reports = Collections.emptyList();
} else {
    reports = weeklyReportService.getReportsByUserIds(visibleUserIds);
}
// status / keyword 过滤继续按需做
return Result.success(reports);
```

`getReportById`、`updateReport`、`deleteReport` 同样走对应 `canXxxWeeklyReport`,撕掉 Controller 内联硬编码字面量。

### 4.6 `WeeklyReportService` 改动

- `approveReport` / `rejectReport` 在主流程成功之后调用 `approvalLogService.log(...)`(详见 §5);
- 新增 `getReportsByUserIds(List<String>)` 走新 mapper;
- **`@Transactional`**:主流程仍在事务内;审批日志写入在 try/catch 内,**失败不回滚主流程**(参考 `OvertimeService:338-355`)。

---

## 5. 审批日志表(新增审计)

### 5.1 DDL

```sql
-- backend/add_weekly_report_approval_log_table.sql

CREATE TABLE IF NOT EXISTS sys_weekly_report_approval_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    report_id     VARCHAR(64)  NOT NULL                COMMENT '周报 ID -> sys_weekly_report.id (String,见 WeeklyReport.id)',
    approver_id   VARCHAR(8)   NOT NULL                COMMENT '审批人 user_id',
    approver_role VARCHAR(32)  NOT NULL                COMMENT '审批人当时角色快照',
    action        VARCHAR(16)  NOT NULL                COMMENT 'approve / reject',
    comment       TEXT         NULL                    COMMENT '审批意见;reject 时必填',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_id        (report_id),
    KEY idx_approver_created (approver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='周报审批操作日志(只读审计;不替代主表 approver_id/approve_time)';
```

不加 DB 外键(`CLAUDE.md` 规则:FK 在 Java 层执行)。

### 5.2 Entity / DTO

```java
// entity/WeeklyReportApprovalLog.java
@Data
public class WeeklyReportApprovalLog {
    private Long id;
    private String reportId;       // 对齐 WeeklyReport.id 类型
    private String approverId;
    private String approverRole;   // admin / dept-project-manager / project-manager / project-owner
    private String action;         // approve / reject
    private String comment;
    private LocalDateTime createdAt;
}

// dto/WeeklyReportApprovalLogDTO.java
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

### 5.3 Mapper

```xml
<!-- WeeklyReportApprovalLogMapper.xml -->

<insert id="insert"
        parameterType="com.wbs.project.entity.WeeklyReportApprovalLog"
        useGeneratedKeys="true" keyProperty="id">
    INSERT INTO sys_weekly_report_approval_log
        (report_id, approver_id, approver_role, action, comment, created_at)
    VALUES
        (#{reportId}, #{approverId}, #{approverRole}, #{action}, #{comment}, #{createdAt})
</insert>

<select id="selectByReportId" resultType="com.wbs.project.dto.WeeklyReportApprovalLogDTO">
    SELECT
        l.id, l.report_id AS reportId,
        l.approver_id     AS approverId,
        u.name            AS approverName,
        u.avatar_url      AS approverAvatar,
        l.approver_role   AS approverRole,
        l.action, l.comment,
        l.created_at      AS createdAt
    FROM sys_weekly_report_approval_log l
    LEFT JOIN sys_user u ON u.id = l.approver_id
    WHERE l.report_id = #{reportId}
    ORDER BY l.created_at ASC
</select>
```

### 5.4 `WeeklyReportApprovalLogService`

```java
@Service
public class WeeklyReportApprovalLogService {
    @Autowired private WeeklyReportApprovalLogMapper logMapper;
    @Autowired private PermissionService permissionService;
    @Autowired private UserMapper userMapper;
    @Autowired private WeeklyReportMapper weeklyReportMapper;
    @Autowired private ProjectMapper projectMapper;

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
            return role;                                  // admin / project-manager 直接返回
        }
        if (projectId != null) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && approverId.equals(project.getOwnerId())) {
                return "project-owner";
            }
        }
        return "dept-project-manager";                    // 兜底(canApproveWeeklyReport 已放行)
    }
}
```

后期可抽 `ApprovalRoleResolver` 公共类与加班共用(本期不做)。

### 5.5 写日志触点

`WeeklyReportService.approveReport` 主流程末尾追加(注意 `reportId` 是 `String`):

```java
approvalLogService.log(reportId, approverId, approve ? "approve" : "reject", comment);
```

### 5.6 新增 REST 接口

```
GET /api/weekly-reports/{id}/approval-logs
```

| 项 | 值 |
|---|---|
| 权限 | `permissionService.canViewWeeklyReport(currentUserId, reportId)` |
| 入参 | path `id` = report id(`String`) |
| 出参 | `Result<List<WeeklyReportApprovalLogDTO>>`,按 `createdAt ASC` |
| 失败 | 周报不存在 → 404;无权限 → 403 |

```java
@GetMapping("/{id}/approval-logs")
public Result<List<WeeklyReportApprovalLogDTO>> getApprovalLogs(
        @PathVariable String id,
        HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    if (!permissionService.canViewWeeklyReport(currentUserId, id)) {
        return Result.error(403, "无权查看此周报的审批记录");
    }
    return Result.success(approvalLogService.listByReport(id));
}
```

### 5.7 驳回 comment 强制必填

`approveReport` 已有的 reject 分支补充校验:

```java
if (!approve && (comment == null || comment.trim().isEmpty())) {
    throw new BusinessException(400, "驳回意见为必填项");
}
```

### 5.8 存量数据回填策略 —— **不回填**

理由:
- 主表 `approver_id / approveComment / approveTime` 仍可展示上次审批快照;
- 回填会丢失 `approver_role`(无法准确判定历史快照角色);
- 详情页 `WeeklyReportApprovalLog.vue` 在 `v-if="logs.length"` 时不渲染,旧周报无副作用。

---

## 6. 前端口径统一

### 6.1 改造前的 4 处不一致

| # | 文件 | 现状口径 | 问题 |
|---|------|----------|------|
| ① | `views/WeeklyReportDetail.vue:298-302` `canApprove` | `admin` 或 `project-manager` | dept-pm 看不到审批按钮 |
| ② | `stores/permission.ts:310-312` `canApproveWeeklyReport` | `canEditProject(projectId)` | 与 ① 不一致 |
| ③ | `views/WeeklyReports.vue:271-290` 列表过滤 | 角色字面量二次 filter | 多余且与后端不一致 |
| ④ | `components/weeklyReport/WeeklyReportCard.vue:81-92` `canEdit` | 显式 `if(project-manager) return false` | 反语义 |
| ⑤ | `components/weeklyReport/WeeklyReportComment.vue:58-60` `canDelete` | `admin` 或 `userId==self` | 与可见范围不一致 |

### 6.2 `stores/permission.ts` 收敛(单一真相源)

```ts
interface WeeklyReportPermissionInput {
  id: string             // 周报 ID(对齐 WeeklyReport.id String 类型)
  userId: string         // submitter
  projectId: string
  status: ReportStatus   // draft / submitted / approved / rejected
}

function canViewWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (currentRole.value === 'admin') return true
  if (report.userId === userStore.user.id) return true
  return isInAccessibleScope(report)
}

function canApproveWeeklyReport(report: WeeklyReportPermissionInput): boolean {
  if (!userStore.user) return false
  if (report.status !== 'submitted') return false
  if (report.userId === userStore.user.id) return false
  if (currentRole.value === 'admin') return true

  const submitter = userStore.userById(report.userId)        // 复用已有 helper(stores/user.ts:92)
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

> **前端判定仅控按钮显隐,后端是真正闸门**。submitter 不在前端缓存里时,保守地不显示审批按钮(用户可刷新或前端按需补拉)。
>
> **依赖**:`stores/user.ts` 已有 `userById(id: string): User | undefined`(L92-94),本设计直接复用,无须新增。

### 6.3 列表页 `WeeklyReports.vue` 移除前端二次过滤

```ts
// 改造后 —— 只剩 UI 过滤
const filteredReports = computed(() => {
  let list = store.reports
  if (filters.status) list = list.filter(r => r.status === filters.status)
  if (filters.keyword) list = list.filter(r => matchKeyword(r, filters.keyword))
  return list
})
```

在 `stores/weeklyReport.ts` 加全局 `watch`,user/role 切换时重新拉数据:

```ts
watch(
  () => `${userStore.user?.id ?? ''}|${permissionStore.currentRole ?? ''}`,
  () => { fetchReports() },
  { immediate: false }
)
```

### 6.4 `WeeklyReportCard.vue` / `WeeklyReportDetail.vue` 改写

```vue
<script setup lang="ts">
const perm = usePermissionStore()
const canEdit    = computed(() => perm.canEditWeeklyReport(props.report))
const canDelete  = computed(() => perm.canDeleteWeeklyReport(props.report))
const canApprove = computed(() => perm.canApproveWeeklyReport(props.report))
</script>
```

`WeeklyReportDetail.vue:298-302` 的内联 `canApprove` 整段删除,改为引用同一 computed。`WeeklyReportComment.vue.canDelete` 按"评论作者 / admin / 周报可见者"判定。

### 6.5 新组件 `WeeklyReportApprovalLog.vue`

形态与 `components/overtime/OvertimeApprovalLog.vue` 对称:

```vue
<template>
  <div v-if="logs.length" class="approval-log">
    <h3 class="text-base font-semibold">{{ $t('weeklyReports.approvalHistory') }}</h3>
    <div v-for="row in logs" :key="row.id" class="log-row">
      <Avatar :src="row.approverAvatar" :name="row.approverName" />
      <div>
        <span class="approver-name">{{ row.approverName }}</span>
        <Tag :variant="roleVariant(row.approverRole)">
          {{ $t('roles.' + row.approverRole.replaceAll('-', '_')) }}
        </Tag>
        <Tag :variant="actionVariant(row.action)">
          {{ $t('weeklyReports.action.' + row.action) }}
        </Tag>
        <time>{{ formatDateTime(row.createdAt) }}</time>
      </div>
      <p class="comment" v-if="row.comment">{{ row.comment }}</p>
    </div>
  </div>
</template>
```

挂在 `WeeklyReportDetail.vue` 的审批弹窗下方;`onMounted` 拉取一次:

```ts
const logs = ref<WeeklyReportApprovalLog[]>([])
onMounted(async () => {
  logs.value = await api.weeklyReport.getApprovalLogs(props.reportId)
})
```

### 6.6 `services/api.ts` 新增

```ts
async getApprovalLogs(reportId: string): Promise<WeeklyReportApprovalLog[]> {
  return request<WeeklyReportApprovalLog[]>(
    'GET',
    `/weekly-reports/${reportId}/approval-logs`,
  )
}
```

### 6.7 i18n 新增 key(仅 zh,ko/en 留待后续补)

```ts
// i18n/locales/zh.ts —— weeklyReports 命名空间下追加
approvalHistory: '审批记录',
action: {
  approve: '通过',
  reject: '驳回',
},
roles: {
  project_owner: '项目负责人',   // 用于审批日志快照展示
}
```

### 6.8 类型补充 `types/index.ts`

```ts
export interface WeeklyReportApprovalLog {
  id: number                    // 日志表自增 PK,仍是数字
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

### 6.9 `frontend/src/utils/constants.ts` —— 顺手补全 `USER_ROLE_OPTIONS`

`USER_ROLE_OPTIONS` 是 admin 角色管理界面用户角色下拉框的数据源,**当前漏了 `dept-project-manager`**,导致 admin 无法在 UI 上把人改成"部门项目负责人"角色 —— 这与本期"dept-pm 范围生效"的契约**直接冲突**(契约要求 dept-pm 能正常被任命)。一行修复,故纳入本期 scope。

```ts
export const USER_ROLE_OPTIONS = [
  { value: 'admin',                  labelKey: 'roles.admin' },
  { value: 'dept-project-manager',   labelKey: 'roles.dept_project_manager' },  // 新增
  { value: 'project-manager',        labelKey: 'roles.project_manager' },
  { value: 'member',                 labelKey: 'roles.member' },
  { value: 'viewer',                 labelKey: 'roles.viewer' },
]
```

---

## 7. 不做清单(scope freeze)

防止边写边膨胀,以下条目**本期一律不做**,可记入未来增强 backlog:

- 状态机增 `withdrawn / overdue_draft / resubmitted`
- 周报表加 `dept_code / task_id / approver_role` 冗余列
- 未交周报提醒邮件 / 周一自动提醒 scheduler
- 部门交付率 / PM 项目交付率统计看板
- 周报 ↔ 任务关联
- ko / en 文案补齐(仅补 zh)
- 公共 `ApprovalRoleResolver` 基座抽象(加班 + 周报共享)
- `WeeklyReportDebug.vue` 改动
- 历史审批数据回填 log 表

---

## 8. 验证 / 风险 / 实施顺序

### 8.1 验证脚本(手工)

由于本仓库**无前端测试运行器、后端测试套件为空**(`CLAUDE.md`),验证靠手工跑。建议 6 个测试账号(可用 SQL `add_*.sql` 临时插):

| 账号 | role | dept_code | managed_dept_codes | managed_project_ids | 拥有 owner 项目 |
|---|---|---|---|---|---|
| `T_ADMIN`  | admin                | -      | -            | -            | -    |
| `T_DEPT_A` | dept-project-manager | DEPT_A | `["DEPT_A"]` | -            | -    |
| `T_DEPT_B` | dept-project-manager | DEPT_B | `["DEPT_B"]` | -            | -    |
| `T_PM1`    | project-manager      | DEPT_A | -            | `["P001"]`   | P002 |
| `T_OWNER`  | member               | DEPT_A | -            | -            | P003 |
| `T_MEMBER` | member               | DEPT_A | -            | -            | -    |

### 8.2 验证场景清单

1. **可见范围**:`T_DEPT_A` 列表仅含 `dept_code = DEPT_A` 的 submitter;`T_PM1` 含 `P001` + `P002` 项目下成员的周报。
2. **审批 happy path**:`T_OWNER` 审 `T_MEMBER` 在 `P003` 的周报 → 通过,log 表出现 `approver_role = project-owner`。
3. **dept-pm 审 PM 提交者**:`T_PM1` 提交 `P001` 周报 → `T_DEPT_A` 可审,`T_PM1` / 其他 PM / `T_DEPT_B` 都不可。
4. **自审 / 互批拒绝**:`T_PM1` 不能审自己的周报。
5. **跨部门不可见**:`T_DEPT_B` 拉 `T_MEMBER`(DEPT_A)的详情 → 403。
6. **驳回 comment 必填**:`approve=false, comment=null` → 400 + `Result.error("驳回意见为必填项")`。
7. **审批日志权限**:`T_MEMBER` 拉别人周报的 `approval-logs` → 403。
8. **按钮显隐**:6 个账号下页面审批/编辑/删除按钮符合 §3 矩阵。
9. **不变项回归**:状态机不变;被驳回后能再编辑;评论功能不变。
10. **SQL 计划**:`getAccessibleWeeklyReportUserIds` 返回 200+ 个 user_id 时,主表 IN 过滤 EXPLAIN 走索引。

### 8.3 风险与回滚

| 风险 | 等级 | 触发 | 回滚 |
|---|---|---|---|
| 5 档判定对周报场景有边角不适用 | 中 | 业务方反馈 PM 提交无人能审 | `canApproveWeeklyReport` 档②加 fallback;1 行改动,无 DB 变更 |
| `getAccessibleWeeklyReportUserIds` IN 列表过长 → SQL 失败 | 低 | 单 dept-pm 管辖 > 5000 在职人员 | 切子查询 `user_id IN (SELECT id FROM sys_user WHERE dept_code IN (...))`;1 处 mapper 改动 |
| 审批日志写失败刷 ERROR | 低 | log 表 DDL 未执行 | service 内部 try/catch 已吞;部署 checklist 强制执行 SQL |
| 前端按钮显隐与后端不一致 | 低 | submitter 缓存未命中 | 刷新页面或在缓存中补拉;后端是闸门,不泄漏数据 |
| token_version 未刷,角色变更未即时生效 | 低 | 上线时改人员 `role` 或 `managed_xxx` | SQL 末尾 `UPDATE sys_user SET token_version = token_version + 1 WHERE role IN (...)` |
| 存量周报详情展示退化 | 极低 | 旧周报 log 表为空 | 主表 `approver_id/approveComment/approveTime` 兜底;新组件 `v-if="logs.length"` 不渲染 |

### 8.4 实施顺序(4 个提交粒度)

> **任何 git commit 操作均需用户明确指令"提交"才执行**(`CLAUDE.md` 强约束)。下列只是粒度建议,非自动脚本。

```
①  feat(db): 新增 sys_weekly_report_approval_log 表
    files: backend/add_weekly_report_approval_log_table.sql
    手工执行: mysql -uroot -proot db_webwbs < backend/add_weekly_report_approval_log_table.sql

②  feat(weekly-report): PermissionService 五档判定 + Controller 收口 + 审批日志
    files: PermissionService.java, WeeklyReportController.java,
           WeeklyReportService.java, WeeklyReportMapper.xml,
           entity/WeeklyReportApprovalLog.java,
           dto/WeeklyReportApprovalLogDTO.java,
           mapper/WeeklyReportApprovalLogMapper.java + .xml,
           service/WeeklyReportApprovalLogService.java

③  feat(weekly-report ui): 前端口径统一 + 审批日志面板
    files: stores/permission.ts, stores/weeklyReport.ts,
           views/WeeklyReports.vue, views/WeeklyReportDetail.vue,
           components/weeklyReport/WeeklyReportCard.vue,
           components/weeklyReport/WeeklyReportComment.vue,
           components/weeklyReport/WeeklyReportApprovalLog.vue (新),
           services/api.ts, types/index.ts,
           i18n/locales/zh.ts, utils/constants.ts

④  chore(weekly-report): docs/superpowers/specs 文档归档
    files: 本设计文档
```

每个粒度独立可上线,事故时局部回滚。

### 8.5 后端 i18n / Result.message 仍用中文 ✅

按 `CLAUDE.md`「Backend `Result.message` strings stay in Chinese」,新增报错文案均用中文:

- `"无权审批此周报"`
- `"无权查看此周报的审批记录"`
- `"驳回意见为必填项"`

---

## 9. 关键文件清单

### 9.1 新增文件

| 文件 | 类型 |
|---|---|
| `backend/add_weekly_report_approval_log.sql` | DDL |
| `backend/src/main/java/com/wbs/project/entity/WeeklyReportApprovalLog.java` | Entity |
| `backend/src/main/java/com/wbs/project/dto/WeeklyReportApprovalLogDTO.java` | DTO |
| `backend/src/main/java/com/wbs/project/mapper/WeeklyReportApprovalLogMapper.java` | Mapper interface |
| `backend/src/main/resources/mapper/WeeklyReportApprovalLogMapper.xml` | Mapper XML |
| `backend/src/main/java/com/wbs/project/service/WeeklyReportApprovalLogService.java` | Service |
| `frontend/src/components/weeklyReport/WeeklyReportApprovalLog.vue` | 新组件 |
| `docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md` | 本文档 |

### 9.2 修改文件

| 文件 | 修改概要 |
|---|---|
| `backend/.../service/PermissionService.java` | 新增 4 个 weekly-report 权限方法 + 1 个数据范围方法,重写 `canApproveWeeklyReport` |
| `backend/.../controller/WeeklyReportController.java` | 撕掉硬编码白名单;`getReports` / `approveReport` / `updateReport` / `deleteReport` 改走 PermissionService;新增 GET `/approval-logs` |
| `backend/.../service/WeeklyReportService.java` | `approveReport` 加审批日志触点;新增 `getReportsByUserIds`;reject 加 comment 必填校验 |
| `backend/src/main/resources/mapper/WeeklyReportMapper.xml` | 新增 `selectByUserIds` |
| `frontend/src/stores/permission.ts` | 重写 4 个 `canXxxWeeklyReport`,采纳新 5 档判定 |
| `frontend/src/stores/weeklyReport.ts` | 加 user/role 切换 `watch` |
| `frontend/src/views/WeeklyReports.vue` | 移除前端二次角色过滤 |
| `frontend/src/views/WeeklyReportDetail.vue` | 内联 `canApprove` 删除,改用 permission store;挂入 `WeeklyReportApprovalLog.vue` |
| `frontend/src/components/weeklyReport/WeeklyReportCard.vue` | `canEdit / canDelete / canApprove` 改走 store |
| `frontend/src/components/weeklyReport/WeeklyReportComment.vue` | `canDelete` 按可见范围判定 |
| `frontend/src/services/api.ts` | 新增 `weeklyReport.getApprovalLogs` |
| `frontend/src/types/index.ts` | 加 `WeeklyReportApprovalLog` 接口 |
| `frontend/src/i18n/locales/zh.ts` | 加 `approvalHistory / action / roles.project_owner` |
| `frontend/src/utils/constants.ts` | `USER_ROLE_OPTIONS` 补 `dept-project-manager` |

---

## 10. 决策日志(供未来追溯)

| 时间 | 决策 | 备选 | 理由 |
|---|---|---|---|
| 2026-06-14 | dept-pm 范围按 `submitter.dept_code` | project.dept_code / 并集 / 交集 | 与加班一致,面向"人"管理,跨部门借调不漏 |
| 2026-06-14 | PM 取 managed_project_ids ∪ owner | 仅 managed / 严格分层 / 双限定 | 与加班 `validateApprover` L459-467 一致 |
| 2026-06-14 | 项目成员仅看自己 | 同项目互见只读 / 项目级开关 | 隐私优先;协作需求走 owner/PM 视角汇总 |
| 2026-06-14 | 可见 + 审批一套放开 | 仅改可见 / 双限定 / 二级审批 | 当前 4 处口径不一致已是 bug,顺手收敛 |
| 2026-06-14 | 方案 B(对齐 + 审计) | A(口径对齐)/ C(冗余字段) | 审计是企业刚需;C 超 scope |
| 2026-06-14 | reject comment 强制必填 | 可选 | 与加班 OvertimeService L196 一致 |
| 2026-06-14 | owner 提交者项目内 PM 也可审批 | 仅 dept-pm | 业务方运营诉求;周报与加班对齐承诺被打破 |
| 2026-06-14 | 草稿状态对 creator 之外的人不可见(含 admin) | 草稿对 admin 可见 | 业务方运营诉求:草稿是个人工作区,严格私密 |

---

## 附录 A: 与加班模块对照表

| 维度 | 加班(已实现) | 周报(本设计) |
|---|---|---|
| 5 档审批判定 | `OvertimeService.validateApprover:422-479` | `PermissionService.canApproveWeeklyReport`(本设计 §4.2) |
| 数据范围方法 | `getAccessibleOvertimeUserIds:732-780` | `getAccessibleWeeklyReportUserIds`(本设计 §4.3) |
| 审批日志表 | `t_overtime_approval_log` | `sys_weekly_report_approval_log`(本设计 §5.1) |
| 角色快照解析 | `OvertimeService.resolveApproverRole:376-388` | `WeeklyReportApprovalLogService.resolveApproverRole`(本设计 §5.4) |
| Controller 入口 | `OvertimeController.approve` 走 `validateApprover` | `WeeklyReportController.approveReport` 走 `canApproveWeeklyReport`(本设计 §4.5) |
| reject comment 必填 | `OvertimeService.approveRecord:196` | `WeeklyReportService.approveReport`(本设计 §5.7) |

> 后续若有第三个审批场景(如请假、报销),可统一抽 `ApprovalRoleResolver` + `AbstractApprovalLogService<T>` 公共基座。
