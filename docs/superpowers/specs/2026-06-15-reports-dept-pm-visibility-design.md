# 报表统计 —— 部门项目负责人(dept-pm)可见性修复 设计文档

| 字段 | 值 |
|---|---|
| 日期 | 2026-06-15 |
| 作者 | sunruoqin10(用户)+ Claude 协作 |
| 状态 | 待用户复核 → 待 spec-reviewer 评审 |
| 适用版本 | `wbs-project-final` master 分支(基于 commit `75198cb`) |
| 关联模块 | 报表统计(Reports.vue)、权限(PermissionStore.canViewProject)、前端数据范围对齐 |
| Scope 关键字 | dept-pm 报表可见性 / 单一真源 / 前端过滤收敛到 permissionStore |

---

## 1. 背景

### 1.1 现状问题

- **5 个角色**(`UserRole.java:17-21`):`admin / dept-project-manager / project-manager / member / viewer`
- **报表统计页 `frontend/src/views/Reports.vue`** 的 `userProjects`(L188-197)和 `userTasks`(L200-206)用了一段手写的"按角色过滤":
  ```typescript
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return projectStore.projects;
  }
  // member / viewer / dept-project-manager 全部落到 else 分支
  return projectStore.projects.filter(p => p.ownerId === currentUserId || ...);
  ```
  显然**漏放了 `dept-project-manager`**;此外这个 `else` 分支还漏判了 `isProjectCreator`(创建者可见)与 `isManagedProject`(PM 管辖项目) —— 一并被 `canViewProject` 修正。
- **后端 `ProjectService.getAllProjectsForUser` + `PermissionService.getAccessibleProjectIds` 的 dept-pm 分支已经能正确返回所辖部门的项目**,断点完全在前端。
- **导出函数 `exportComprehensive` / `exportGantt`**(L475-539)传入的是 `taskStore.tasks` / `userStore.users` 全量,与图表口径不一致 —— 即使前端图表修对,导出 Excel 仍然会泄露全公司范围。

### 1.2 真实用户场景

- YAN TINGDANG(`C0000012`,`role=dept-project-manager`,`dept_code=ITSM`,`managed_dept_codes=["ITSM"]`)
- ITSM 部门下 2 个项目:`SOP`(owner C0000122,project-manager)、`HESS`(owner C0000002, created_by C0000103,均为 project-manager)
- YAN 自己未 owner / 未 member 这 2 个项目
- 当前 Reports 页:4 个汇总数字全 0,4 张图表全空,导出 Excel 含全公司数据
- 期望:Reports 页 4 数字 + 4 图表正确呈现 ITSM 部门 2 个项目数据,导出 Excel 也只含这 2 个项目

### 1.3 业务诉求(锁定的决策)

| # | 决策 | 选项 |
|---|------|------|
| 1 | dept-pm 范围口径 | `managed_dept_codes` 对应的部门项目集合(与 `PermissionService.getAccessibleProjectIds` dept-pm 分支一致) |
| 2 | 修复方式 | Reports.vue 改用 `permissionStore.canViewProject(p.id)` 统一过滤(单一真源) |
| 3 | 修复范围 | 仅 `Reports.vue`(DelayStats / WeeklyReportForm / WeeklyReportDetail 同样模式的 3 处不顺手改,见 §7) |
| 4 | 导出 Excel | 改 `tasks` / `users` 数据源,与图表口径一致 |

### 1.4 不在本期范围

详见 §7「不做清单」。

---

## 2. 设计原则

- **单一真源**: Reports.vue 不再写"角色 → 全部数据"的硬编码分支,统一委托给 `permissionStore.canViewProject(p.id)` —— 该方法内部已实现 admin / creator / owner / member / dept-pm 4 个分支(注:`managed_project` 是 **编辑** 矩阵的分项,**不在** view 矩阵里),与后端 `PermissionService.canViewProject` 语义一致。
- **与现有模块一致**: 周报(`PermissionStore.canViewWeeklyReport`)、加班(`PermissionStore.canApproveOvertime`)、文档(`canEditDocument`)已采用"前端走 `permissionStore.xxx()`,后端 `PermissionService` 兜底"模式。本次修复是补齐最后一块。
- **不引入新数据源 / 不动后端 / 不改 SQL**。

---

## 3. 改动清单

### 3.1 `frontend/src/views/Reports.vue` —— `userProjects`(L188-197)

**改前**:
```typescript
const userProjects = computed(() => {
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return projectStore.projects;
  }
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return projectStore.projects.filter(
    p => p.ownerId === currentUserId || (p.memberIds && p.memberIds.includes(currentUserId))
  );
});
```

**改后**:
```typescript
const userProjects = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return projectStore.projects.filter(p => permissionStore.canViewProject(p.id));
});
```

### 3.2 `frontend/src/views/Reports.vue` —— `userTasks`(L200-206)

**改前**:
```typescript
const userTasks = computed(() => {
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return taskStore.tasks;
  }
  const userProjectIds = new Set(userProjects.value.map(p => p.id));
  return taskStore.tasks.filter(t => userProjectIds.has(t.projectId));
});
```

**改后**:
```typescript
const userTasks = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return taskStore.tasks.filter(t => permissionStore.canViewProject(t.projectId));
});
```

### 3.3 `frontend/src/views/Reports.vue` —— `exportComprehensive`(L475-506)

**改前**:
```typescript
exportToExcelNamespace.comprehensive(
  {
    projects,
    tasks: taskStore.tasks,        // ← 全量
    users: userStore.users,        // ← 全量
    stats: statistics.value
  },
  ...
);
```

**改后**:
- `tasks` 改用 `userTasks.value`(可见项目下的任务)
- `users` 改用 **`owners ∪ assignees` 子集** —— 即 `userTasks` 中的 `assigneeId` ∪ `userProjects` 中的 `ownerId` 对应的 `userStore.users`。理由:`exportComprehensive` 内部会:
  1. 用 `data.users.find(u => u.id === project.ownerId)` 查 owner 名称(L141-156);若只传 assignee 子集,owner 不在其中,owner 单元格会落到 `'未分配'` fallback,正确性受损
  2. `data.users.length` 写入 "总成员数"(L527);若只传 assignee,数字仍可读但语义偏窄

  注:此处 `users` 的实际含义是 "导出表需要展示的用户",应覆盖所有 owner + 所有任务的 assignee;不是 "团队绩效" 那张图的过滤后用户(团队绩效的过滤仍然由 `userTasks` 派生,见 §3.5)。

### 3.4 `frontend/src/views/Reports.vue` —— `exportGantt`(L509-539)

**改前**: 同样传入 `taskStore.tasks` / `userStore.users` 全量。

**改后**: 同样改 tasks 为 `userTasks.value`、users 为 `owners ∪ assignees` 子集(与 §3.3 同口径)。

### 3.5 团队绩效图(L391-455)的用户过滤

**未改动**: 团队绩效图原本就以 `leafTasks.filter(t => t.assigneeId === user.id)` 为过滤条件,等价于 "assignee 子集",与 `userTasks` 派生口径一致,无需额外调整。

---

## 4. 数据流

### 4.1 修前(YAN 视角)

```
GET /api/projects  →  ProjectService.getAllProjectsForUser("C0000012")
                    →  PermissionService.getAccessibleProjectIds("C0000012")  (dept-pm 分支)
                    →  projectMapper.selectByDeptCodes(["ITSM"])
                    →  返回 [SOP, HESS]  (✅ 正确)

Reports.vue userProjects:
  - currentRole = 'dept-project-manager', 不在 admin/pm 放行分支
  - else: filter(p.ownerId == 'C0000012' || memberIds.contains('C0000012'))
  - SOP.ownerId = 'C0000122', HESS.ownerId = 'C0000002' → 都不是 YAN
  - 返回 []  (❌ 错误)

GET /api/tasks  →  taskStore.tasks  (全量)
Reports.vue userTasks: 由 userProjects 派生,也是 []
统计: 全 0;导出: taskStore.tasks 全量 + userStore.users 全量
```

### 4.2 修后(YAN 视角)

```
GET /api/projects  →  返回 [SOP, HESS]

Reports.vue userProjects:
  - filter(p => permissionStore.canViewProject(p.id))
  - permissionStore.canViewProject('SOP.id'):
      - isDeptProjectManager() === true
      - SOP.deptCode = 'ITSM', isDeptManager('ITSM') === true
      - 返回 true ✅
  - permissionStore.canViewProject('HESS.id'):
      - 同上 ✅
  - 返回 [SOP, HESS]  (✅ 正确)

Reports.vue userTasks: filter(t => canViewProject(t.projectId)) → 只含 SOP/HESS 项目的任务
统计: 真实数字;导出: userTasks 子集 + 相关 assignee
```

### 4.3 其他角色回归(admin / PM / member / viewer)

| 角色 | 修前 userProjects | 修后 userProjects | 影响 |
|---|---|---|---|
| admin | 放行全量 | `canViewProject` admin 分支放行全量 | 一致 ✅ |
| project-manager | 放行全量 | `canViewProject` 检查 creator / owner / member(注意:**view 矩阵不含 `managed_project` 分支**,只有 `canEditProject` 才含 `isManagedProject`) | 收紧 ⚠️ |

> ⚠️ **回归风险**: 旧实现对 PM 是"全量放行"(包括 PM 自己不是 owner / 不是 member / 不是 creator 的项目)。`canViewProject` 收紧为 creator / owner / member。
>
> 注:`isManagedProject`(`managed_project_ids`)是 **编辑** 矩阵的分支(`canEditProject` / `canManageTaskContent`),**不在** view 矩阵里。**一个 PM 若仅通过 `managedProjectIds` 与项目关联、不在 owner / member / creator 列表中,修前能在 Reports 看到,修后将看不到。**
>
> 但后端 `PermissionService.canViewProject` 早已按类似口径判定,PM 在其他视图(项目列表、详情、任务)能看到的范围本就是收紧后的。Reports.vue 旧实现对 PM "全放"是 **Reports 页独有的偏离**。
>
> 决策:接受此次回归 —— Reports 页与项目列表 / 任务详情等其他视图口径统一才是更重要的目标。如 PM 反馈看不到某个项目,优先确认 `managedProjectIds` / `ownerId` / `createdBy` / `memberIds` 之一是否匹配。
>
> 后续如需 "PM 仅通过 managed_project_ids 也能在 Reports 看到",可在 `canViewProject` 内增加 `isManagedProject` 分支 —— 这是 view 矩阵的扩张决策,**不在本期范围**。

| 角色 | 修前 userTasks | 修后 userTasks | 影响 |
|---|---|---|---|
| member / viewer | filter by `userProjectIds` | `canViewProject` member 分支按 memberIds 判定 | 一致 ✅(语义对齐) |

---

## 5. 边界与失败模式

### 5.1 `userStore.currentUserId` 为空(未登录态)

`userProjects` / `userTasks` 在 `currentUserId` 为空时直接返回 `[]`(防御性早返回);即便不写这道短路,`canViewProject` 内部 `isProjectOwner` / `isProjectMember` 在 `currentUserId` 为空时也会自然返回 false(成员判断会落到 `''.includes(userId)` 形式),整体结果等价。早返回主要是避免冗余 store 调用。

### 5.2 `permissionStore` 未就绪(竞态)

`permissionStore.currentRole` 是 `computed`,依赖 `userStore.currentUser`,登录态下两者在 `onMounted` 时已可用,无竞态。

### 5.3 `managed_dept_codes` 为 NULL 或 `[]`

`isDeptManager(deptCode)` 在该场景下必返回 false,dept-pm 走不到任何项目 → `userProjects = []`。这是"未配置管辖部门"的合理空,不应越权显示 —— 与原实现的 else 分支结果一致(也是 `[]`)。

### 5.4 跨部门项目

`sys_project.dept_code` 是单值,一项目只属于一个部门,不存在 dept-pm 跨部门"串数据"问题。

### 5.5 导出文件名 / 表结构

不动 `comprehensive` / `gantt` 工具函数内部,只调整传入数据源;Excel 表头、列、样式保持不变。

---

## 6. 验收方式

### 6.1 手动验收(由用户启服务执行)

1. **YAN TINGDANG 登录 → 报表统计页**
   - 4 个汇总数字非 0(应为 ITSM 下 2 个项目的真实数字)
   - 项目状态分布 / 任务优先级分布 / 项目进度 三张图含 SOP、HESS 数据
   - 团队绩效图:仅显示在 ITSM 项目下作为 assignee 的成员;若 SOP/HESS 项目下当前无任何任务被分配,此图可能为空(由实际数据决定,不算 bug)
2. **导出综合 Excel**:文件内 `projects` 工作表只含 SOP + HESS 两项目,`总成员数` = ITSM 项目涉及的成员数
3. **导出甘特图**:每个工作表一个项目,只含 SOP / HESS

### 6.2 回归验收(其他角色登录)

| 账号 | 期望结果 |
|---|---|
| admin | 报表页与现状一致(全公司数据) |
| project-manager | 报表页与 `canViewProject` 口径对齐:只看 owner / member / managed_project / creator 的项目。**可能比修前少一些项目**(见 §4.3 回归风险) |
| member | 与现状一致(参与的项目) |
| viewer | 与现状一致(参与的项目,只读) |

### 6.3 构建验证

- `npx vue-tsc` 通过(类型检查)
- `npm run build` 通过(vue-tsc + vite build)

### 6.4 不做的事

- 不写单测(前端无测试运行器,后端测试套件空)
- 不启服务(CLAUDE.md 规则)
- 不提交代码(用户未要求)

---

## 7. 不做清单

| # | 项 | 原因 | 后续 |
|---|----|------|------|
| 1 | `frontend/src/views/DelayStats.vue` L486 —— 项目过滤 `admin \|\| project-manager` 放行,漏 dept-pm。**与 Reports 同 bug 形态**(computed 过滤)。 | 本期只修 Reports | 另起 task,沿用 §3.1 同样的 `canViewProject` 替换 |
| 2 | `frontend/src/views/WeeklyReportForm.vue` L243 —— 项目列表过滤,同 bug 形态。 | 本期只修 Reports | 另起 task |
| 3 | `frontend/src/views/WeeklyReportDetail.vue` L196 —— 文档删除按钮的 `v-if` 显隐(`doc.uploadedBy === currentUserId \|\| admin \|\| project-manager`),**与 Reports 是不同的 bug 形态**(按钮显隐,不是列表过滤)。dept-pm 当前能删本部门成员的文档。 | 本期无关 | 另起 task,改为 `canDeleteDocument(doc.projectId, doc.uploadedBy)` |
| 4 | 周报模块现有 dept-pm 审批日志 / 视图调整 | 本期无关 | 已完成(commit 75198cb) |
| 5 | 后端 `PermissionService` / `ProjectService` / Mapper SQL | 本期前已正确,不动 | — |
| 6 | i18n 文案 / 图标 / 布局 | 无关 | — |
| 7 | Reports.vue 之外的报表导出口径 | 无关 | — |
| 8 | `canViewProject` 内是否新增 `isManagedProject` 分支 | PM 可能在 `managed_project_ids` 但不在 owner/member/creator,view 矩阵当前不含此分支 | 后续如 PM 反馈 Reports 看不到 managed 项目,再决策 view 矩阵扩张 |

---

## 8. 变更影响范围

| 文件 | 改动行数估计 | 风险 |
|---|---|---|
| `frontend/src/views/Reports.vue` | 4 处:userProjects / userTasks / exportComprehensive / exportGantt | 低 —— 均为内部数据过滤,不改 API |
| 后端 / 数据库 / 配置 | 0 | — |
| 其他前端文件 | 0 | — |
| 测试文件 | 0 | 项目内无前端测试运行器 |

---

## 9. 关联文档

- `docs/superpowers/specs/2026-06-14-weekly-report-roles-design.md` —— 周报模块已完成的 5 档对齐,可作为本次的模式参考
- `docs/superpowers/specs/2026-06-13-dept-pm-overtime-view-design.md` —— dept-pm 加班模块的同类修复模板
- `frontend/src/stores/permission.ts` L186-194 `canViewProject` —— 本次复用的核心方法
- `backend/src/main/java/com/wbs/project/service/PermissionService.java` L131-160 后端 `canViewProject` —— 语义对齐目标