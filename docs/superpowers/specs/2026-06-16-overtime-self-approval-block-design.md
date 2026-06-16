# 加班审批防自审(项目负责人 / 项目经理) — 设计

> 日期:2026-06-16
> 状态:草案(待用户确认 + 实施)
> 范围:后端 `OvertimeService.validateApprover` 单方法;前端不动

## 1. 背景与目标

`OvertimeService.validateApprover` 当前的"非 PM 提交者"分支有一处放行:

```java
// 项目负责人可以审批其负责项目的加班申请
if (approverId.equals(project.getOwnerId())) {
    return;
}
```

当加班记录的提交者本人恰好是该项目 `project.ownerId` 时,这一行会通过 `approverId == submitterId` 的隐式相等判断,导致**项目负责人可以审批自己提交的加班** —— 业务上属于"自审",与周报模块的 `canApproveWeeklyReport` 已落地的"防自审"先例不一致。

类似地,提交者角色为 `project-manager` 时,虽然走的是"同部门 dept-pm 才可批"分支,理论上不会被自己审批,但若该 PM 同时持有同部门的 `dept-project-manager` 身份(兼岗),存在自审的灰色路径。本次一并补上兜底。

## 2. 业务规则

| 提交者 | 防自审 | 可审批人 | 备注 |
|--------|------|---------|------|
| 项目负责人(`submitterId == project.ownerId`) | ✗ 禁止自审 | admin ∪ 项目 PM(项目 ∈ `managed_project_ids`) ∪ 部门项目负责人(提交者 dept ∈ `managed_dept_codes`) | admin 兜底不变 |
| 项目经理角色(`submitter.role == 'project-manager'`) | ✗ 禁止自审 | admin ∪ 同部门 dept-pm | 与现状分支一致,仅补防自审 |
| 普通成员 / 观察者 | (未变更) | 同现状(不构成自审场景) | 保留 |

## 3. 关键判定逻辑

### 3.1 `OvertimeService.validateApprover(approverId, projectId, submitterId)` — 改造

```text
# 现状(L422-479)
1. 查 approver,失败 throw
2. role == admin → return
3. submitterIsProjectManager 分支
4. 非 PM 提交者分支:
   a. approverId == project.ownerId → return          ← 改动
   b. project-manager + managed_project_ids → return
   c. dept-project-manager + submitter dept 匹配 → return
5. throw

# 改造
1. 查 approver,失败 throw
2. role == admin → return
3. ★ 新增 ★ approverId == submitterId → throw "不能审批自己提交的加班申请"
4. submitterIsProjectManager 分支(原样,已自包含防自审语义)
5. 非 PM 提交者分支(原样,因步骤 3 兜底,owner 自审路径已被拦截)
6. throw
```

**关键点**: 防自审闸口放在 admin 早返回**之后**、业务分支判断**之前**,作为统一闸口。这样无论是 project owner 自审路径、PM 自审路径、还是未来新增的"成员"自审场景,都一次性兜住,无需在每个分支里重复判断。

### 3.2 不改的边界

- 不动 `hasPermission`(读权限,本就允许 owner 看自己提交)
- 不动 `OvertimeController` / 审批日志 / 邮件通知
- 不动前端 `ApprovalModal.vue`(后端 `Result.error` 直接弹窗)
- 不回溯历史已自审的记录(数据保留,不刷)

## 4. 错误处理

- 抛出 `RuntimeException("不能审批自己的加班申请")`
- 沿用现有 `GlobalExceptionHandler.handleRuntimeException` 兜底(返回 `Result.error(400, message)`)
- 前端 `OvertimeManagement.vue` 的 `handleApproveSubmit` / `handleRejectSubmit` 两个 catch 改为 `alert((error as Error)?.message || '审批失败,请重试')`,**透传后端 message**,不再硬编码吞错
- 中文 `Result.message`,符合 API 契约

## 5. 行为对比

| 场景 | 改前 | 改后 |
|------|------|------|
| 项目 owner 批自己的加班 | ✓ 通过(bug) | ✗ 拒绝:不能审批自己的加班申请 |
| 项目 owner 批同项目其它人 | ✓ 通过 | ✓ 不变 |
| PM(角色)批自己的(无 dept-pm 身份) | ✗ 已被 PM 分支拒绝 | ✗ 提早一步,统一错误信息 |
| PM(角色)批自己的(同部门 dept-pm 身份) | ✓ 偶然通过 | ✗ 拒绝 |
| 同部门 dept-pm 批 PM 提交 | ✓ 通过 | ✓ 不变 |
| 项目 PM 批普通成员 | ✓ 通过 | ✓ 不变 |
| admin 批任何 | ✓ 通过 | ✓ 不变(admin 在防自审前已早返回) |

## 6. 测试

后端测试套件当前为空(按 `AGENTS.md` 约定,本次不写单测,沿用 `mvn -pl . test -DfailIfNoTests=false` 兜底)。

- 编译验证:`mvn -pl . compile -DfailIfNoTests=false` 必须通过
- 业务验证:由用户在本地启动后端 + 前端,按 §5 场景手动跑一遍

## 7. 实施步骤

1. `OvertimeService.validateApprover` 在 admin 早返回后插入自审闸口
2. 跑 `mvn -pl . compile -DfailIfNoTests=false` 验证
3. 提示用户在本地跑 §5 场景

## 8. 不在本次范围

- `PermissionService.canApproveOvertime` 的统一封装(可作为后续重构)
- 前端 `ApprovalModal` 隐藏"批准/拒绝"按钮(目前 403 反馈足够,等前端有诉求再加)
- 撤回 / 锁定已自审的历史记录
- 周报(`WeeklyReport`)模块:本身已有"防自审"先例,本次不动
