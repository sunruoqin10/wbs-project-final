# 加班审批审计日志 — 实施计划

## Context

WBS 加班管理模块在 2026-06-13 完成了 dept-pm 视图接入后,目前 **3 类角色均可审批** 加班申请:
- `admin`
- `project-manager`
- 项目负责人 (`project.owner_id`)
- 部门项目负责人 (`dept-project-manager`,按 `project.deptCode ∈ managed_dept_codes`)

问题:虽然后端 `t_overtime_record.approver_id` 字段已在每次审批时写入,但:
1. **前端 UI 从未显示** "谁批了 / 什么时候批 / 当时什么角色"——用户看不到审计信息
2. **DB 里"半截多级审批"字段** (`approval_stage` / `first_approver_id` / `second_approver_id`) 完全不被代码使用,实体/mapper/service 都没接,只有 1 条历史测试数据
3. **拒绝原因** (`reject_reason`) 也只在主表存储一次,如果将来改成可多次审批就丢失历史

用户要求:"对于加班申请的审批需要标记是哪个人审批的。请给出最佳解决方案"。

**选定方案:新建 `t_overtime_approval_log` 审批日志表**(方案 C),不动现有 `t_overtime_record` 主表的单级审批字段,死字段另开 PR 清理。

## 设计要点

- 每次审批/拒批,**append 一条日志记录**,包含审批人 / 角色 / 时间 / 拒绝原因
- `approver_role` 在审批动作发生时**快照**当时的角色,避免后续角色变更影响历史
- 主表 `approver_id` / `approved_at` / `reject_reason` 仍作为"最终结果"保留,日志表是"完整审计"
- 前端 `ApprovalModal` 加"审批历史"区块,表格列加"审批人 / 审批时间"
- 老记录(已 approved/rejected 但没日志)在前端兜底显示:从主表 `approver_id`/`approved_at` 合成一条虚拟日志条目

## 改动文件清单

### 后端(5 个文件)

| 文件 | 类型 | 说明 |
|---|---|---|
| `backend/add_overtime_approval_log_table.sql` | 新增 | DDL 建表 |
| `backend/src/main/java/com/wbs/project/entity/OvertimeApprovalLog.java` | 新增 | 实体(`@Data`) |
| `backend/src/main/java/com/wbs/project/mapper/OvertimeApprovalLogMapper.java` | 新增 | `@Mapper` 接口 |
| `backend/src/main/resources/mapper/OvertimeApprovalLogMapper.xml` | 新增 | resultMap + insert + selectByOvertimeId |
| `backend/src/main/java/com/wbs/project/dto/OvertimeDTO.java` | 修改 | 在 `ApproveRequest` 旁加 `ApprovalLogResponse` inner class |
| `backend/src/main/java/com/wbs/project/service/OvertimeService.java` | 修改 | `approveRecord` 写入日志 + 新增 `getApprovalLogs` |
| `backend/src/main/java/com/wbs/project/controller/OvertimeController.java` | 修改 | 新增 `GET /{id}/approval-logs` 端点 |

### 前端(4 个文件)

| 文件 | 修改 |
|---|---|
| `frontend/src/types/index.ts` | 新增 `OvertimeApprovalLog` 接口(放在 `OvertimeRecord` 后 L154) |
| `frontend/src/services/api.ts` | 新增 `getOvertimeApprovalLogs(id)` 方法(在 `rejectOvertimeRecord` 后 L542)+ import 新类型 |
| `frontend/src/stores/overtime.ts` | 新增 `loadApprovalLogs(overtimeId)` action |
| `frontend/src/components/overtime/ApprovalModal.vue` | 顶部加"审批历史"区块(只读时间线) |
| `frontend/src/views/OvertimeManagement.vue` | 团队 tab 表格新增"审批人 / 审批时间"两列 |

## 详细设计

### 1. 数据模型 — `t_overtime_approval_log`

```sql
-- ===============================================================
-- 2026-06-14: 新建 t_overtime_approval_log (加班审批日志)
-- 用于审计每次审批/拒批动作,保留审批人 / 角色 / 时间 / 原因
-- 来源 plan: docs/superpowers/plans/2026-06-14-overtime-approval-log.md
-- ===============================================================

CREATE TABLE IF NOT EXISTS t_overtime_approval_log (
    id              VARCHAR(50)  NOT NULL PRIMARY KEY              COMMENT '日志ID(oal+uuid8)',
    overtime_id     VARCHAR(50)  NOT NULL                          COMMENT '加班记录ID(t_overtime_record.id)',
    approver_id     VARCHAR(20)  NOT NULL                          COMMENT '审批人ID(sys_user.id)',
    approver_role   VARCHAR(50)  DEFAULT NULL                      COMMENT '审批人当时的角色(admin/project-manager/dept-project-manager/project-owner)',
    action          VARCHAR(20)  NOT NULL                          COMMENT '操作: approve / reject',
    reject_reason   VARCHAR(500) DEFAULT NULL                      COMMENT '拒绝原因(仅 reject 时)',
    approved_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    INDEX idx_overtime_id (overtime_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approved_at (approved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班审批历史日志(审计追溯)';
```

注:**不**带外键约束(AGENTS.md 约定:FKs / cascade 在 Java 维护,不在 DB)。

### 2. 后端实体 / Mapper

**`OvertimeApprovalLog.java`** 字段:id, overtimeId, approverId, approverRole, action, rejectReason, approvedAt。
Lombok `@Data`(对照 `DocumentAccessLog.java` 模式)。

**`OvertimeApprovalLogMapper.java`** 接口方法:
- `int insert(OvertimeApprovalLog log)`
- `List<OvertimeApprovalLog> selectByOvertimeId(@Param("overtimeId") String overtimeId)`

**`OvertimeApprovalLogMapper.xml`** 用 `<sql id="Base_Column_List">` 模式(对照 `DocumentAccessLogMapper.xml:16-18`)。

ID 生成:在 `OvertimeService.approveRecord` 内 `"oal" + UUID.randomUUID().toString().substring(0, 8)`(对照 `OvertimeService.java:172` 现有模式)。

### 3. 后端 Service — `OvertimeService.approveRecord` 改造

**现状**(L276-320):只 set 主表 `approver_id` / `approved_at` / `reject_reason`,不写日志。

**改动**:在写入主表 + 发邮件通知**之间**,插入日志条目:

```java
// 计算当前审批人角色(快照)
String approverRole = resolveApproverRole(request.getApproverId(), record.getProjectId());

OvertimeApprovalLog log = new OvertimeApprovalLog();
log.setId("oal" + UUID.randomUUID().toString().substring(0, 8));
log.setOvertimeId(id);
log.setApproverId(request.getApproverId());
log.setApproverRole(approverRole);
log.setAction(request.getApproved() ? "approve" : "reject");
if (!request.getApproved()) {
    log.setRejectReason(request.getRejectReason());
}
log.setApprovedAt(LocalDateTime.now());
approvalLogMapper.insert(log);
```

新增私有方法 `resolveApproverRole(approverId, projectId)`:
- 查 user.role: 'admin' 或 'project-manager' → 直接返回
- 否则查 project.ownerId:匹配 → 'project-owner'
- 否则 'dept-project-manager'(因为 `validateApprover` 已经通过 dept-pm 分支放行)

新增 `OvertimeService.getApprovalLogs(overtimeId)` 方法:
- 调 `approvalLogMapper.selectByOvertimeId` 拿原始日志
- 老记录兜底:若主表 `approver_id` 非空且日志表为空,合成一条虚拟日志条目(用 `userMapper.selectById` 拿姓名)
- 返回 `List<OvertimeApprovalLog>`

### 4. 后端 DTO — `OvertimeDTO.ApprovalLogResponse`

放在 `ApproveRequest`(L122)之后,新加 inner class:

```java
@Data
public static class ApprovalLogResponse {
    private String id;
    private String overtimeId;
    private String approverId;
    private String approverName;     // 后端 join sys_user.name
    private String approverRole;     // 角色标签(admin / project-manager / ...)
    private String action;           // approve / reject
    private String rejectReason;
    private LocalDateTime approvedAt;
}
```

### 5. 后端 Controller — `GET /api/overtime/{id}/approval-logs`

放在 `approveRecord`(L185)之后,`// ==================== 审批 API ====================` 块内:

```java
@GetMapping("/{id}/approval-logs")
public Result<List<OvertimeDTO.ApprovalLogResponse>> getApprovalLogs(
        @PathVariable String id, HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);
    
    // 权限校验:复用 hasPermission(同 getRecordById L92-109)
    OvertimeRecord record = overtimeService.getRecordById(id);
    if (record == null) return Result.error("加班记录不存在");
    if (currentUserId != null && !overtimeService.hasPermission(
            currentUserId, record.getProjectId(), record.getUserId())) {
        return Result.error("您没有权限查看此加班记录");
    }
    
    List<OvertimeApprovalLog> logs = overtimeService.getApprovalLogs(id);
    // 装配 approverName
    List<OvertimeDTO.ApprovalLogResponse> response = logs.stream().map(log -> {
        OvertimeDTO.ApprovalLogResponse r = new OvertimeDTO.ApprovalLogResponse();
        BeanUtils.copyProperties(log, r);
        r.setApproverName(userMapper.selectById(log.getApproverId()) != null
                ? userMapper.selectById(log.getApproverId()).getName() : "未知");
        return r;
    }).toList();
    return Result.success(response);
}
```

### 6. 前端类型 — `OvertimeApprovalLog`

`frontend/src/types/index.ts` L154 后插入:

```ts
/**
 * 加班审批历史日志
 */
export interface OvertimeApprovalLog {
  id: string;
  overtimeId: string;
  approverId: string;
  approverRole?: 'admin' | 'project-manager' | 'dept-project-manager' | 'project-owner';
  approverName?: string;
  action: 'approve' | 'reject';
  rejectReason?: string;
  approvedAt: string;
}
```

### 7. 前端 API — `apiService.getOvertimeApprovalLogs`

`frontend/src/services/api.ts`:
- L3 import 添加 `OvertimeApprovalLog`
- L542(`rejectOvertimeRecord` 之后)加:

```ts
async getOvertimeApprovalLogs(id: string | number): Promise<OvertimeApprovalLog[]> {
  return request<OvertimeApprovalLog[]>(`/overtime/${id}/approval-logs`);
}
```

### 8. 前端 Store — `loadApprovalLogs`

`frontend/src/stores/overtime.ts` 加 action(放在 `rejectOvertimeRecord` 后):

```ts
const loadApprovalLogs = async (overtimeId: string): Promise<OvertimeApprovalLog[]> => {
  try {
    return await apiService.getOvertimeApprovalLogs(overtimeId);
  } catch (error) {
    console.error('Failed to load approval logs:', overtimeId, error);
    return [];
  }
};
```

`return` 块暴露该 action。

### 9. 前端 UI — `ApprovalModal` 加"审批历史"区块

`frontend/src/components/overtime/ApprovalModal.vue`:

- 新增 import:`useOvertimeStore`, `OvertimeApprovalLog`, `dayjs`
- 新增 reactive state:`approvalLogs: ref<OvertimeApprovalLog[]>([])`, `loadingLogs: ref(false)`
- 在 `watch(() => props.open, async (isOpen) => {...})` 中,打开时调 `loadApprovalLogs(props.record.id)` 拉日志
- 在模板 L48(加班原因区块)与 L56(审批操作区块)之间,新增"审批历史"区块:

```html
<!-- 审批历史 -->
<div>
  <h4 class="mb-2 text-sm font-semibold text-secondary-900">审批历史</h4>
  <div v-if="loadingLogs" class="text-sm text-secondary-500">加载中...</div>
  <div v-else-if="approvalLogs.length === 0" class="text-sm text-secondary-500">暂无审批记录</div>
  <ul v-else class="space-y-2">
    <li v-for="log in approvalLogs" :key="log.id" class="rounded-lg bg-secondary-50 p-3 text-sm">
      <div class="flex items-center justify-between">
        <span class="font-medium text-secondary-900">{{ log.approverName || '未知' }}</span>
        <span class="text-xs text-secondary-500">{{ formatDateTime(log.approvedAt) }}</span>
      </div>
      <div class="mt-1 flex items-center gap-2 text-xs">
        <span class="inline-flex rounded-full px-2 py-0.5"
              :class="log.action === 'approve' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'">
          {{ log.action === 'approve' ? '通过' : '拒绝' }}
        </span>
        <span class="text-secondary-500">{{ getRoleLabel(log.approverRole) }}</span>
      </div>
      <div v-if="log.rejectReason" class="mt-1 text-xs text-secondary-600">
        原因: {{ log.rejectReason }}
      </div>
    </li>
  </ul>
</div>
```

辅助函数:

```ts
const formatDateTime = (s: string) => dayjs(s).format('YYYY-MM-DD HH:mm');
const getRoleLabel = (role?: string) => {
  const map: Record<string, string> = {
    admin: '管理员',
    'project-manager': '项目经理',
    'project-owner': '项目负责人',
    'dept-project-manager': '部门项目负责人'
  };
  return map[role || ''] || role || '';
};
```

### 10. 前端 UI — 团队 tab 表格加列

`frontend/src/views/OvertimeManagement.vue`:
- L478(团队 tab 表头)"项目负责人"列**之后**插入:

```html
<th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('overtime.filters.approver') }}</th>
<th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('overtime.filters.approvalTime') }}</th>
```

注:这俩 i18n key 已存在(zh.ts L860-861),不用新增。

- L519("项目负责人"列 td)之后插入:

```html
<td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
  {{ getApproverName(record.approverId) }}
</td>
<td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
  {{ record.approvedAt ? formatDateTime(record.approvedAt) : '-' }}
</td>
```

辅助函数(放在已有 `formatDate` 旁):

```ts
const getApproverName = (id?: string) => {
  if (!id) return '-';
  return userStore.userById(id)?.name || '未知';
};
const formatDateTime = (s: string) => dayjs(s).format('YYYY-MM-DD HH:mm');
```

需 import `dayjs`(已 import 在 L633)。

## 复用的现有设施

| 复用什么 | 位置 |
|---|---|
| `overtimeService.hasPermission` 权限校验 | `OvertimeService.java:58` |
| `overtimeService.getRecordById` 单条查询 | `OvertimeService.java:122` |
| `userMapper.selectById` 用户名查询 | 已有,多处使用 |
| i18n key `overtime.filters.approver` / `approvalTime` | `frontend/src/i18n/locales/zh.ts:860-861`(已有) |
| `overtimeStore` 的 action 模式 | `frontend/src/stores/overtime.ts`(已有) |
| `apiService.request<T>` 包装 | `frontend/src/services/api.ts:36` |
| `BeanUtils.copyProperties` | Spring 内置 |
| UUID+prefix ID 生成 | `OvertimeService.java:172` 现有模式 |

## 验证

### 静态检查

```bash
# 后端
cd backend && mvn -pl . clean install -DskipTests

# 前端
cd frontend && npx vue-tsc --noEmit
```

预期:无新错误(基线已有 `result / availableUsers / aValue/bValue` 等与本次无关)。

### 运行时验证(用户启动后端 + 前端)

1. **DB 验证** — 先跑迁移 `mysql ... < backend/add_overtime_approval_log_table.sql`,确认表存在
2. **PM 审批** — 用 C0000103 登录,审批一条 `pending` 记录 → `t_overtime_approval_log` 新增一行,`approver_role='project-manager'`
3. **dept-pm 审批** — 用 C0000012 登录,审批同条记录 → 新增日志,`approver_role='dept-project-manager'`
4. **老记录兼容** — 已存在的 approved 记录 `otd331ae67` 打开 ApprovalModal → "审批历史"区显示从主表合成的虚拟条目(兜底)
5. **拒绝场景** — 拒批一条记录 → 日志的 `action='reject'`,`reject_reason` 有值,Modal 显示
6. **团队 tab 表格** — 两条列"审批人 / 审批时间"显示正确

### 后端 SQL 验证

```sql
-- 迁移后表存在
SHOW TABLES LIKE 't_overtime_approval_log';

-- 审批后日志
SELECT id, overtime_id, approver_id, approver_role, action, reject_reason, approved_at
FROM t_overtime_approval_log ORDER BY approved_at DESC LIMIT 5;

-- 老记录兜底
SELECT t.id, t.status, t.approver_id, t.approved_at,
       (SELECT COUNT(*) FROM t_overtime_approval_log WHERE overtime_id = t.id) AS log_count
FROM t_overtime_record t WHERE t.status IN ('approved','rejected');
```

## 不动的部分

- `t_overtime_record.approver_id` / `approved_at` / `reject_reason` —— 主表"最终结果"字段保留
- 死字段 `approval_stage` / `first_approver_id` / `second_approver_id` —— 本次不动,留独立 cleanup PR
- `OvertimeService.validateApprover` —— 不变,谁有权批还是那个判断
- 前端 `canEdit` / `canDelete` / dept-pm 视图 —— 上两轮已修,本轮不变
- 后端 `approveRecord` 主表写入逻辑 —— 仍 set `approver_id`/`approved_at`/`reject_reason`(向后兼容)

## 风险

| 风险 | 缓解 |
|---|---|
| 老记录无日志,Modal "审批历史"显示空 | 兜底:`getApprovalLogs` 从主表合成虚拟条目 |
| `approver_role` 在 user.role 改变后历史失真 | 接受(快照语义);同时存 `approver_id` 可反查当前 role |
| 日志表膨胀 | 索引 `idx_overtime_id`,主表删除时暂不级联清理日志(审计要求);后续 PR 评估 |
| 日志写入失败导致审批失败 | try/catch 包裹 insert,不阻断审批主流程(只 log 错误) |
| 与死字段 `first_approver_id` 等并存 | 不动它们;后续 PR 评估是否清理 |

## 提交策略

按 CLAUDE.md 不自动 commit。本次完成后:
1. 跑 `mvn -pl . clean install -DskipTests` + `npx vue-tsc --noEmit` 自查
2. `git diff` 给用户看
3. 等用户明确说"提交"再 commit

建议 commit message:

```
feat(overtime): 新建 t_overtime_approval_log 审计每次审批操作

解决"多个角色都能审批,无法追溯具体审批人"的问题。
新建独立日志表记录每次审批/拒批动作,不影响主表。

后端:
- 新建表 t_overtime_approval_log (oal+uuid8 PK)
- 新增 OvertimeApprovalLog 实体/Mapper
- OvertimeService.approveRecord 在写入主表后插入日志条目,
  approver_role 快照当时的角色
  (admin/project-manager/dept-project-manager/project-owner)
- OvertimeService.getApprovalLogs 兜底老记录(从主表合成虚拟条目)
- OvertimeController 新增 GET /api/overtime/{id}/approval-logs

前端:
- types: OvertimeApprovalLog
- api: getOvertimeApprovalLogs(id)
- store: loadApprovalLogs(overtimeId)
- ApprovalModal: 加"审批历史"区块(只读时间线)
- OvertimeManagement 团队 tab 表格: 新增"审批人/审批时间"两列
  (复用已有 i18n key overtime.filters.approver/approvalTime)

不动主表 approver_id/approved_at/reject_reason(向后兼容),
不动死字段 approval_stage/first_approver_id/second_approver_id
(留独立 cleanup PR)。
```