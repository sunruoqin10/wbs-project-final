# Reports 部门项目负责人可见性修复 —— 实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `frontend/src/views/Reports.vue` 报表统计页对 `dept-project-manager` 角色的过滤逻辑,使其与 `permissionStore.canViewProject` 的 5 档判定对齐,确保 YAN TINGDANG 等部门项目负责人能看到所辖部门下所有项目的真实数据(含导出)。

**Architecture:** 前端单点修复。`Reports.vue` 内 4 处数据过滤(2 个 computed + 2 个导出函数)统一改为调用 `permissionStore.canViewProject(p.id)`,与 `PermissionStore` / `PermissionService` 已有 5 档判定语义对齐。不动后端、不动数据库、不动其他视图。

**Tech Stack:** Vue 3 + TypeScript + Pinia(`usePermissionStore` / `useProjectStore` / `useTaskStore` / `useUserStore`)+ Vue I18n + ECharts(已存在,不动)+ ExcelJS(已存在,不动)。

---

## 文件结构

| 文件 | 类型 | 职责 |
|---|---|---|
| `frontend/src/views/Reports.vue` | 修改 | 报表统计页;改 4 处数据过滤 |

无新建文件、无后端改动、无 SQL 改动、无 i18n 改动。

---

## 上下文

**角色与数据范围**(对齐 `frontend/src/stores/permission.ts:186-194` `canViewProject`):
- `admin` → 全部项目
- `isProjectCreator(p.id)` → 创建者
- `isProjectOwner(p.id)` → owner
- `isProjectMember(p.id)` → memberIds 包含当前用户
- `isDeptProjectManager() && isDeptManager(p.deptCode)` → 部门项目负责人且项目所在部门在管辖范围内

**用户场景**:YAN TINGDANG(`C0000012`, `role=dept-project-manager`, `managed_dept_codes=["ITSM"]`),ITSM 部门下 2 个项目 SOP / HESS,既非 owner 也非 member。修前 `userProjects = []`,修后 `userProjects = [SOP, HESS]`。

**导出子集规则**(`owners ∪ assignees`):
- 原因 1:`exportComprehensive` 内部 `data.users.find(u => u.id === project.ownerId)`(export.ts L141-156)用于查找项目负责人名称;若 owner 不在 `data.users` 子集中,owner 单元格会落到 `'未分配'` fallback,正确性受损
- 原因 2:`data.users.length` 写入 "总成员数"(export.ts L527)

---

## 任务 1:修改 `userProjects` computed

**Files:**
- Modify: `frontend/src/views/Reports.vue:188-197`

- [ ] **Step 1: 替换 `userProjects` computed 实现**

将 `Reports.vue:188-197` 的实现替换为:

```typescript
// 根据数据范围过滤项目:统一走 permissionStore.canViewProject
// (admin / creator / owner / member / dept-pm 五档,与后端 PermissionService.canViewProject 对齐)
const userProjects = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return projectStore.projects.filter(p => permissionStore.canViewProject(p.id));
});
```

- [ ] **Step 2: 视觉对照确认删除/新增内容**

打开文件确认 L188-197:
- 删除:手写的 `if (admin || pm) return ...; else filter by owner/member`
- 新增:防御性 `currentUserId` 早返回 + `filter(p => permissionStore.canViewProject(p.id))`

- [ ] **Step 3: 类型检查**

运行:`cd frontend && npx vue-tsc --noEmit`
预期:无 TS 错误(若失败,检查 `permissionStore` / `userStore` / `projectStore` 是否仍在 script setup 顶部 import —— 原文 L172-179 已 import,无需新增)。

---

## 任务 2:修改 `userTasks` computed

**Files:**
- Modify: `frontend/src/views/Reports.vue:200-206`

- [ ] **Step 1: 替换 `userTasks` computed 实现**

将 `Reports.vue:200-206` 的实现替换为:

```typescript
// 根据数据范围过滤任务:任务所属项目能看,则任务能看
const userTasks = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return taskStore.tasks.filter(t => permissionStore.canViewProject(t.projectId));
});
```

- [ ] **Step 2: 类型检查**

运行:`cd frontend && npx vue-tsc --noEmit`
预期:无 TS 错误。

---

## 任务 3:修改 `exportComprehensive` 数据源

**Files:**
- Modify: `frontend/src/views/Reports.vue`(任务 3 涉及两处:script-setup 顶层 + `exportComprehensive` 函数体内)

- [ ] **Step 1: 在 script-setup 顶层新增 `usersInScope` computed**

⚠️ **必须放在 `<script setup>` 顶层**(在 `userTasks` computed 之后,`statistics` computed 之前,即原 L206 之后),**不能**放在 `exportComprehensive` 函数体内 —— `computed()` 必须有 Vue 组件实例上下文,在 async 函数体内调用会报错;且任务 4 的 `exportGantt` 也要复用同一个 `usersInScope`,必须保持单例。

新增代码块:

```typescript
// 导出用用户子集:"出现在可见范围内"的所有用户
//   = 可见项目的 owner ∪ 可见任务的 assignee
// 理由:exportComprehensive 内部按 data.users.find(ownerId) 查项目负责人名称
//      (export.ts L141-156),若 owner 不在此子集,owner 单元格会落到 '未分配' fallback
const usersInScope = computed(() => {
  const ownerIds = new Set(userProjects.value.map(p => p.ownerId));
  const assigneeIds = new Set(userTasks.value.map(t => t.assigneeId).filter(Boolean));
  const scopedIds = new Set([...ownerIds, ...assigneeIds]);
  return userStore.users.filter(u => scopedIds.has(u.id));
});
```

注意:这里直接用 `userProjects.value` / `userTasks.value`(任务 1/2 已定义的 computed),不复用 `exportComprehensive` 内部的 `projects` 局部变量 —— 那个 alias 作用域太窄。

- [ ] **Step 2: 在 `exportComprehensive` 函数顶部添加 `tasks` 局部变量**

打开 `exportComprehensive`(L475 起始)。当前 L476 已有 `const projects = userProjects.value;`。在该行下方新增:

```typescript
const tasks = userTasks.value;
```

(后续 Step 3 会引用 `tasks` 局部。)

- [ ] **Step 3: 替换 `exportToExcelNamespace.comprehensive` 调用**

将 `exportToExcelNamespace.comprehensive({ ... })` 中的 `tasks: taskStore.tasks,` 改为 `tasks: tasks,`,`users: userStore.users,` 改为 `users: usersInScope.value,`。

- [ ] **Step 4: 类型检查**

运行:`cd frontend && npx vue-tsc --noEmit`
预期:无 TS 错误。

---

## 任务 4:修改 `exportGantt` 数据源

**Files:**
- Modify: `frontend/src/views/Reports.vue:509-539`(函数体)以及任务 3 中已经在 setup 顶层加好的 `usersInScope`(`exportGantt` 复用同一个 computed,无需再定义)

- [ ] **Step 1: 在 `exportGantt` 函数顶部添加 `tasks` 局部变量**

打开 `exportGantt`(L509 起始)。当前 L510 已有 `const projects = userProjects.value;`。在该行下方新增:

```typescript
const tasks = userTasks.value;
```

- [ ] **Step 2: 替换 `exportGantt` 调用**

将 `exportToExcelNamespace.gantt({ ... })` 中的 `tasks: taskStore.tasks,` 改为 `tasks: tasks,`,`users: userStore.users,` 改为 `users: usersInScope.value,`。

- [ ] **Step 3: 类型检查**

运行:`cd frontend && npx vue-tsc --noEmit`
预期:无 TS 错误。

---

## 任务 5:构建验证

**Files:**
- 无

- [ ] **Step 1: 运行完整构建**

运行:`cd frontend && npm run build`
预期:`vue-tsc` 通过 + `vite build` 成功,无 TS 错误,无 lint 警告(本项目未配置 lint)。

---

## 任务 6:报告与 handoff

**Files:**
- 无

- [ ] **Step 1: 提交报告**

向用户报告:
1. 改了 `frontend/src/views/Reports.vue` 哪几行(L188-197, L200-206, L475-506, L509-539)
2. 验收方式:YAN TINGDANG 登录看报表页 4 数字 + 4 图表 + 2 个导出(由用户按 CLAUDE.md 启动服务后手动验证)
3. 回归点:`project-manager` 角色在 Reports 页可见项目数可能比修前少(口径与其他视图对齐;详见 spec §4.3)
4. 顺带报告未修位置:`DelayStats.vue:486`、`WeeklyReportForm.vue:243`(同 bug 形态);`WeeklyReportDetail.vue:196`(按钮显隐,不同 bug 形态);`canViewProject` 是否新增 `isManagedProject` 分支(留待 PM 反馈后决策)
5. 未提交 git(按 CLAUDE.md 规则,等用户明确"提交"指令)

- [ ] **Step 2: 等待用户指示**

- 不主动 `git add` / `git commit` / `git push`
- 不启动后端 / 前端服务(按 CLAUDE.md 规则)
- 等用户:
  - 启动服务并手动验证报表页 → 用户反馈通过或需调整
  - 给出提交指令("提交" / "commit")后才 commit

---

## 验收清单(供用户手动验证时参考)

### 主验收:YAN TINGDANG(C0000012, dept-pm, managed_dept_codes=["ITSM"])

| # | 检查点 | 期望 |
|---|---|---|
| 1 | 报表页 `总项目数` 卡片 | ≥ 1(应为 2:SOP + HESS) |
| 2 | 报表页 `总任务数` / `已完成` / `进行中` 3 张卡片 | 非全 0(对应 ITSM 下 2 个项目的真实数字) |
| 3 | 项目状态分布图 | 含 SOP、HESS 两个项目的状态分类 |
| 4 | 任务优先级分布图 | 含 ITSM 项目下任务的优先级分布 |
| 5 | 项目进度图 | 2 个柱状条(SOP、HESS) |
| 6 | 团队绩效图 | 仅显示 ITSM 项目作为 assignee 的成员(若数据为空属正常,不算 bug) |
| 7 | 点击"导出综合报表"按钮 → Excel 内 `项目总览` 工作表 | 仅含 SOP、HESS;owner 单元格正确显示 PM 名称(非 `'未分配'`) |
| 8 | 点击"导出甘特图"按钮 → Excel 内 | 2 个工作表,各对应一个项目 |

### 回归验收

| 角色 | 期望 |
|---|---|
| admin | 报表页与现状一致(全公司数据) |
| project-manager | 报表页与项目列表页口径对齐:creator / owner / member 项目可见;**仅通过 `managedProjectIds` 关联的项目将不再可见**(详见 spec §4.3 回归风险) |
| member | 报表页与现状一致(参与的项目) |
| viewer | 报表页与现状一致(参与的项目,只读) |

### 构建验收

- `cd frontend && npx vue-tsc --noEmit` 无错
- `cd frontend && npm run build` 成功

---

## 关联文档

- Spec: `docs/superpowers/specs/2026-06-15-reports-dept-pm-visibility-design.md`
- `frontend/src/stores/permission.ts:186-194` `canViewProject`(本计划的核心调用)
- `frontend/src/utils/export.ts:141-156` owner 查表;`:527` "总成员数"
- `frontend/src/views/Reports.vue:188-197, 200-206, 475-506, 509-539` 改动目标行

---

## 风险与不做清单

| # | 项 | 状态 |
|---|---|---|
| 1 | `DelayStats.vue:486` 同 bug 形态 | 不在本期(用户已确认) |
| 2 | `WeeklyReportForm.vue:243` 同 bug 形态 | 不在本期 |
| 3 | `WeeklyReportDetail.vue:196` 不同 bug 形态(按钮显隐) | 不在本期 |
| 4 | `canViewProject` 新增 `isManagedProject` 分支 | 不在本期(决策留待 PM 反馈) |
| 5 | 后端 / 数据库 / SQL / i18n | 不动 |
| 6 | 新建 / 删除文件 | 无 |
| 7 | 新建单测 | 不做(项目内无前端测试运行器) |
| 8 | 启动服务 / 提交 git | 不做(按 CLAUDE.md 规则,等用户指示) |