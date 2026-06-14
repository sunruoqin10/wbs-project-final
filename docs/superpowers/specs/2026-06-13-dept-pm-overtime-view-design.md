# 部门项目负责人查看所辖成员加班信息 — 设计

> 日期：2026-06-13
> 状态：草案（待 spec 审查 + 用户确认）
> 范围：后端 `OvertimeController` / `OvertimeService` / `PermissionService` 及对应 mapper;前端不动

## 1. 背景与目标

WBS 项目管理系统的加班管理模块当前**未覆盖部门项目负责人(`DEPT_PROJECT_MANAGER`)**：

- `OvertimeService.hasPermission` 只放行 `admin` / `project-manager`,部门项目负责人实际上与普通 `MEMBER` 一样 —— 只能看自己提交或自己 `owner_id` 项目的加班记录。
- `OvertimeController.getStats` (L234, L244) 同样把 dept-pm 排除在外,即便绕过也只会拿到 403。
- `OvertimeService.validateApprover` 同问题 —— 部门项目负责人无法审批所辖部门成员的加班。

本次改造的目标是按以下业务规则扩展加班权限:

1. **部门项目负责人** 可**只读查看**所辖部门(`managed_dept_codes` 内)成员的加班记录(列表 / 详情 / 统计)
2. **部门项目负责人** 可**审批**所辖部门成员提交的加班申请(项目归属部门在 `managed_dept_codes` 内)
3. **不**给部门项目负责人改 / 删 / 跨成员创建 / 跨成员导出等权限(保留 admin / owner / 创建者本人 的写权)

约束:

- 不新建独立 ACL 表,复用 `sys_user.dept_code`、`sys_user.managed_dept_codes`、`sys_user.managed_company_cd`、`sys_project.dept_code` 字段
- 不引入新权限注解,沿用 `PermissionService` 集中判定 + `OvertimeService` 调用的现有模式
- 不动前端 `OvertimeManagement.vue` 与 `OvertimeModal.vue`(用户已确认)

## 2. 术语与语义澄清

### 2.1 "所辖成员"口径

> 部门项目负责人所辖的"项目成员" = **`record.userId` 对应 `User.deptCode ∈ currentUser.managedDeptCodes` 且 该用户 `status != 'T'`(在职) 的所有用户**

与 2026-06-13 已落地的 `PermissionService.getAccessibleUploaderIds` 完全一致:

```java
if (isDeptProjectManager(userId)) {
    User u = userMapper.selectById(userId);
    if (u != null) {
        List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
        if (!codes.isEmpty()) {
            ids.addAll(userMapper.selectIdsByDeptCodes(codes));
        }
    }
    return ids;
}
```

**不要求该用户参与过项目**(语义上包含所有 role=member/project-manager/dept-project-manager/viewer 的同部门用户)。

### 2.2 审批权判定口径:按"项目归属部门"而非"提交者部门"

`OvertimeService.validateApprover` 审批权判定采用 **项目归属部门**(`project.deptCode`):

> 加班所在项目 `project.deptCode ∈ dept-pm.managed_dept_codes` ⇒ 部门项目负责人可审批

**不**采用"提交者当前部门" —— 因为提交者可能换部门(`User.deptCode` 由 HR 同步刷新),按项目归属判定更稳定,也与 2026-06-12 角色 v2 的 `PermissionService.canEditProject` 口径一致。

### 2.3 "查看"边界

含:列表(`GET /api/overtime`)、详情(`GET /api/overtime/{id}`)、统计聚合(`GET /api/overtime/stats` 及 `/stats/*` 全部子接口)、按成员/按项目/按日期/按类型分组。

不含:创建 / 修改 / 删除。

### 2.4 自检

- dept-pm 自己的加班记录 —— `userId == recordUserId` 早返回,`hasPermission` 命中
- dept-pm 管辖范围内 owner 是非 dept-pm 本人的项目 —— 仍由 hasPermission 命中(以"提交者部门"判定) —— 即:dept-pm 不需要是项目 owner,只要加班记录提交者 dept 在 managed_dept_codes 内即可
- dept-pm 跨公司 / 跨 `managed_company_cd` —— `isDeptManager` 内部 `managedCompanyCd == user.companyCd` 不匹配会拒绝(本用户接口调用方已带 companyCd 约束;如未来放宽需另开 PR)
- `status='T'` 离职用户的历史加班记录 —— 数据保留,但因 dept-pm 范围过滤(`status != 'T'`)不会命中,符合预期

## 3. 权限矩阵

| 角色 | 查看(列表/详情/统计) | 审批 | 改/删 | 创建 |
|------|------------------|-----|-------|------|
| `ADMIN` | 全部 | 全部 | 全部 | 全部 |
| `DEPT_PROJECT_MANAGER` | 提交者 `deptCode ∈ managed_dept_codes` 的记录 | 项目 `deptCode ∈ managed_dept_codes` 的记录 | ❌ | 仅自己 |
| `PROJECT_MANAGER` | 全部 | 全部 | 仅自己创建 | 仅自己 |
| 项目负责人(owner) | 自己 owner 的项目 | 同上 | 同上 | 仅自己 |
| `MEMBER` | 仅自己提交 | ❌ | 仅自己(走 owner 或自提交路径) | 仅自己 |
| `VIEWER` | 仅自己提交(只读) | ❌ | ❌ | ❌ |

## 4. 关键判定逻辑(伪代码)

### 4.1 `OvertimeService.hasPermission(userId, projectId, recordUserId)` — 改造

```text
# 现状(L58-87)
if userId == null or projectId == null:                return false
user = userMapper.selectById(userId);                  if null return false
if role ∈ {admin, project-manager}:                    return true
if recordUserId != null and userId == recordUserId:    return true
project = projectMapper.selectById(projectId);         if null return false
return userId == project.ownerId;

# 改造(在 self == recordUserId 之后,owner 判断之前,加入 dept-pm 分支)
if recordUserId != null:
    recordUser = userMapper.selectById(recordUserId)
    if recordUser != null and recordUser.deptCode != null
       and permissionService.isDeptManager(userId, recordUser.deptCode):
        return true
```

**注意**:
- `recordUserId == null` 时(老调用方,如 `filterStatsByPermission` 走 `selectByOwnerId` 路径)不放过 dept-pm 分支,避免只看 projectId 误放行
- 复用 `permissionService.isDeptManager(userId, deptCode)`,不重新解析 `managedDeptCodes`
- dept-pm 不再短路返回 `true`(因为仍然要 `recordUserId != null`),以避免一个 dept-pm 看任意 projectId 记录(即便不是所辖成员提交)
- **性能 trade-off(已接受)**: 该方法在 `GET /api/overtime` 列表场景下会被调用 N 次,每次多查一次 `recordUserId → User`(拿 deptCode)。在 dept-pm 调用的场景下,等价于列表分页大小次的额外单点查询。**当前不优化(N+1 接受)**,理由: 1) 已有 6+ 个调用点,签名变更面大; 2) 单点 PK 查询在 8GB MySQL 上 < 5ms; 3) dept-pm 用户基数小,实际数据量不高。**如未来 dept-pm 角色高频使用,可在 PermissionService 加 `getDeptByUserIdBatch(List<String>)` 批量接口,在 Controller 列表入口一次性拉全**。本次不实现,仅记录于此。

### 4.2 `OvertimeService.validateApprover(approverId, projectId)` — 改造

```text
# 现状(L314-337)
approver = userMapper.selectById(approverId);          if null throw
if role ∈ {admin, project-manager}:                    return
project = projectMapper.selectById(projectId);         if null throw
if approverId == project.ownerId:                      return
throw "您没有权限审批此加班申请"

# 改造(在 owner 判断之后,throw 之前,加入 dept-pm 分支)
if permissionService.isDeptProjectManager(approverId)
   and project.deptCode != null
   and permissionService.isDeptManager(approverId, project.deptCode):
    return
```

### 4.3 `PermissionService.getAccessibleOvertimeUserIds(String userId)` — 新增

与 `getAccessibleUploaderIds` 平行,语义上"可见加班记录的 userId 集合":

```text
if userId == null:                                     return emptySet
if isAdmin(userId) OR isProjectManager(userId):        return null   # null = 不限
if isDeptProjectManager(userId):
    u = userMapper.selectById(userId)
    if u != null:
        codes = parseManagedDeptCodes(u)
        if !codes.isEmpty():
            return userMapper.selectIdsByDeptCodes(codes)  # 在职用户 ID 集合
    return emptySet
# MEMBER / VIEWER / owner: 仅自己
return Set.of(userId)
```

**为什么 admin / PM 返回 null**: 与 `getAccessibleUploaderIds` 保持一致(`null` 在 Service 层语义为"不限",用 `IN` 改写时跳过条件)。

### 4.4 `OvertimeService.getStats` — SQL 过滤下沉

```text
# 现状(L344)
records = overtimeMapper.selectByCondition(userId, projectId, null, startDate, endDate, null)
# 然后 Java 流式聚合

# 改造:在调用 selectByCondition 之前,根据角色注入 userId 过滤
if !isAdmin(currentUser) AND !isProjectManager(currentUser):
    accessibleUserIds = permissionService.getAccessibleOvertimeUserIds(currentUser)
    # null 表示不限(MEMBER/owner 等单角色 = 仅自己)
    if accessibleUserIds == null:
        effectiveUserId = null  # 不动
    else if accessibleUserIds.isEmpty():
        return emptyStats       # 没管辖范围,直接空
    else:
        # 与调用方传入的 userId 做 AND 关系
        if userId != null:
            if !accessibleUserIds.contains(userId): return emptyStats
            effectiveUserId = userId  # 限定到具体人
        else:
            # 注入"userId IN (accessibleUserIds)"条件
            effectiveUserIdsForSql = accessibleUserIds
```

**实现方式**: 给 `OvertimeMapper.selectByCondition` 加一个 `List<String> userIds` 参数(可空),XML `<if>` 守卫。SQL:

```sql
<if test="userIds != null and userIds.size() > 0">
    AND o.user_id IN
    <foreach collection="userIds" item="uid" open="(" separator="," close=")">
        #{uid}
    </foreach>
</if>
```

### 4.5 `OvertimeController.getStats` — 放行 dept-pm + 移除 `filterStatsByPermission` 调用

```java
// 现状(L234, L244)
if (!"admin".equals(role) && !"project-manager".equals(role)) {
    return Result.error("您没有权限查看该用户的统计");  // L236
}
// ...
if (!"admin".equals(role) && !"project-manager".equals(role)) {
    stats = filterStatsByPermission(stats, currentUserId);  // L245
}

// 改造(必须与 §4.6 同步):
//   1. L236 allowlist 加上 dept-pm —— 仅放行,不调 filterStatsByPermission
//   2. L244-247 整个 if 块删除(因为 filterStatsByPermission 方法被 §4.6 删除)
if (!"admin".equals(role) && !"project-manager".equals(role) && !"dept-project-manager".equals(role)) {
    return Result.error("您没有权限查看该用户的统计");  // L236 改造后
}
// L244-247 整块删除
```

### 4.6 `OvertimeController.filterStatsByPermission` — 整体删除(强依赖 §4.4)

> 该方法(L43-73)对 dept-pm 不再适用 —— 它假设"按 owner 过滤 byProject",但 dept-pm 应当看到所辖成员在**所有项目**上的加班,按 owner 过滤会丢失信息。

**强依赖**: 本步骤**必须**在 §4.4(`OvertimeService.getStats` SQL 过滤下沉)完成之后执行。否则 dept-pm 在过渡期会看到全公司聚合数据(原 filterStatsByPermission 是兜底裁剪)。

**删除**:
1. `OvertimeController.filterStatsByPermission` 方法本身(L43-73)
2. `OvertimeController.getStats` 中 L244-247 调用 filterStatsByPermission 的 if 块
3. (顺手) `OvertimeController` 中 `private final ProjectMapper projectMapper;` 字段如果 §4.5 之后没有其他引用,可一并删除;否则保留

改由 `OvertimeService.getStats` 入口下沉到 SQL 层做 user_id 过滤,统计的 byProject / byType / byDate 都按"所辖成员"全集聚合(由 SQL `IN` 一次性约束),不再二次裁剪。

### 4.7 `OvertimeMapper` 其他聚合 SQL(`sumHoursGroupBy*`)— 同步注入过滤

`/stats/users` / `/stats/projects` / `/stats/dates` / `/stats/types` 4 个聚合 SQL 当前都**不带 userId 过滤**,dept-pm 调用会看到全公司数据。**统一改造**:

- 给所有 `sumHoursGroupBy*` SQL 加 `userIds` IN 守卫(同 4.4)
- **参数语义**: 新增 `List<String> userIds` 参数表示"允许的提交者集合",与原有单值 `userId`(`projectId`)是 **AND 关系**(两个条件都生效),不替代。当 `userIds` 为 null 或空时跳过 IN 守卫(`null` = 不限,见 `PermissionService.getAccessibleOvertimeUserIds` admin/PM 返回 null 的语义)
- `OvertimeMapper` 5 个受影响方法签名变更:
  - `selectByCondition(String userId, String projectId, ..., List<String> userIds)` —— `userIds` 加在末尾
  - `sumHoursGroupByUser(String projectId, LocalDate startDate, LocalDate endDate, List<String> userIds)` —— **替换** 原 `projectId` 是单值的语义;**但项目 / 日期 / 类型 维度的查询不接受 userId 过滤**(无意义),只接受 userIds
  - `sumHoursGroupByProject(String userId, LocalDate, LocalDate, List<String> userIds)` —— userId 原存在,加 userIds
  - `sumHoursGroupByDate(String userId, String projectId, LocalDate, LocalDate, List<String> userIds)` —— 加 userIds
  - `sumHoursGroupByType(String userId, String projectId, LocalDate, LocalDate, List<String> userIds)` —— 加 userIds
- `OvertimeService` 4 个对应方法(`getUserStats` / `getProjectStats` / `getDateStats` / `getTypeStats`)末尾加 `List<String> userIds` 参数
- `OvertimeController` 4 个对应入口(L256-304)在调用前从 `permissionService.getAccessibleOvertimeUserIds(currentUserId)` 拿 userIds 透传:admin/PM 传 null,dept-pm 传 dept 内用户 ID 集合,其他角色传 `List.of(currentUserId)`

## 5. 接口契约(API Contract)

不变,所有改动对前端透明:

| Endpoint | 改动 |
|---------|------|
| `GET /api/overtime` | dept-pm 现在会看到所辖成员的记录(由 hasPermission 放行) |
| `GET /api/overtime/{id}` | 同上,单条详情可看 |
| `GET /api/overtime/stats` | dept-pm 不再 403,会看到所辖成员的聚合 |
| `GET /api/overtime/stats/users` | dept-pm 现在会看到所辖成员聚合(按 user 分组) |
| `GET /api/overtime/stats/projects` | 同上(按 project 分组) |
| `GET /api/overtime/stats/dates` | 同上(按日期分组) |
| `GET /api/overtime/stats/types` | 同上(按类型分组) |
| `PUT /api/overtime/{id}/approve` | dept-pm 对所辖部门项目的加班可审批 |
| `GET /api/overtime/my/{userId}` | 不变(永远只看自己,前端"我的加班"用) |
| `POST /api/overtime` | 不变(仍只为自己提交) |
| `PUT /api/overtime/{id}` | dept-pm 仍不可改(走原 hasPermission 路径,dept-pm 不是 owner) |
| `DELETE /api/overtime/{id}` | dept-pm 仍不可删(同上) |

## 6. 错误处理

- `Result.message` 全部中文(沿用现有 "您没有权限查看此加班记录" / "您没有权限审批此加班申请")
- 200 / 非 200 行为保持不变
- dept-pm 访问**非所辖成员**的加班:`Result.error("您没有权限查看此加班记录")`
- dept-pm 审批**非所辖部门项目**的加班:`throw new RuntimeException("您没有权限审批此加班申请")`(沿用现有 throw,被 `GlobalExceptionHandler` 捕获)

## 7. 顺手修复

| 修复 | 描述 |
|------|------|
| `getStats` 信息泄露 | 原行为:`MEMBER`/`VIEWER`/`owner`(非 admin/PM)调 `/stats` 会看到**全公司**所有加班小时数总和,只是 `byProject` 字段被裁。改造后这些角色走 SQL `IN` 过滤,只看到自己/自己 owner 的项目,行为更严格且正确 |
| `/stats/users` / `/stats/projects` / `/stats/dates` / `/stats/types` 信息泄露 | 4 个聚合 SQL 当前都不过滤 userId,任何登录用户都能拿到全公司聚合数据。改造后 dept-pm / member / viewer 全部走 SQL `IN` 过滤 |

> **release notes 需注明**: 此变更修复了两个潜在信息泄露 BUG(member/viewer 调 `/stats` 原本会看到全公司加班小时数总和,以及 4 个 `/stats/*` 聚合 SQL 原本不限制 userId)。

## 8. 测试

后端测试套件当前为空(按 `AGENTS.md` 约定,新增测试遵循 JUnit 5 + Spring Boot starter test 风格):

| 测试 | 位置 | 用例 |
|------|------|------|
| `OvertimeService.hasPermission` 单测 | `service/OvertimeServiceTest.java` | dept-pm + 所辖成员 = true;dept-pm + 非所辖成员 = false;dept-pm + 自己 = true(早返回);dept-pm + recordUserId=null = false(需走 owner 路径) |
| `OvertimeService.validateApprover` 单测 | 同上 | dept-pm 审批所辖部门项目 = pass;审批非所辖项目 = throw |
| `OvertimeService.getStats` 单测 | 同上 | dept-pm 调用 → SQL 带 `user_id IN (...)` 过滤;非所辖成员记录不出现;dept-pm 同时调 `/stats/users` 也按所辖成员过滤 |
| `PermissionService.getAccessibleOvertimeUserIds` 单测 | `service/PermissionServiceTest.java` | admin = null;PM = null;dept-pm = dept 内用户 ID 集合(在职);member/viewer = 仅自己 |
| `OvertimeController.getStats` 单测 | `controller/OvertimeControllerTest.java` | dept-pm 不再 403;非所辖成员记录不出现 |

**集成测试**(可选):用 `application-test.yml` + 真实 H2/MySQL,灌 dept-pm + 3 个用户(2 个所辖 + 1 个非所辖),断言 API 返回集合正确。

## 9. 风险 & 缓解

| 风险 | 缓解 |
|------|------|
| `getStats` 行为收紧(member 原本看到全公司总和) | release notes 注明"修复潜在信息泄露";前端 member 调用方仅看自己预期行为正确 |
| 4 个 `/stats/*` 聚合 SQL 行为收紧(原不限 userId) | 同上,前端无任何调用方依赖全公司聚合(member/viewer 没有"全公司"业务场景) |
| dept-pm 切换所辖部门(角色变更)后 token 失效 | `UserMapper.updateRoleAndScope` 已经 `token_version + 1`,沿用 2026-06-12 机制,前端下次登录拿新 token |
| `managed_dept_codes` JSON 字段格式错误 | `parseDeptCodes` 已在 `PermissionService` 内部 try/catch 返回空列表(2026-06-11 实现) |
| OvertimeMapper 加 `userIds` IN 守卫时,空集合是否会被优化掉 | XML `<if test="userIds != null and userIds.size() > 0">` 守卫保证空集合走"无 userId 过滤",由调用方在 Service 层判空集合决定"返回空 stats"(已在 4.4 实现) |
| admin / PM 调用 `/stats/*` 行为是否变化 | 改造后 admin / PM 传 `userIds = null`(走 §4.3 `getAccessibleOvertimeUserIds` admin/PM 分支返回 null),SQL `<if>` 守卫跳过,行为与改造前**完全一致**。其他角色(包含 dept-pm)行为才变化 |

## 10. 实施步骤(高层)

**强顺序约束**: 步骤 5 是步骤 6 的**硬前提**(否则过渡期 dept-pm 会看到全公司数据)。其余步骤彼此独立,可并行。

1. `PermissionService` 新增 `getAccessibleOvertimeUserIds` 方法
2. `OvertimeMapper` 给 `selectByCondition` + 4 个 `sumHoursGroupBy*` SQL 加 `userIds` IN 守卫,Mapper 接口加 `List<String> userIds` 参数
3. `OvertimeService.hasPermission` 加 dept-pm 分支(N+1 trade-off 见 §4.1)
4. `OvertimeService.validateApprover` 加 dept-pm 分支
5. `OvertimeService.getStats` 入口下沉 SQL 过滤;`getUserStats/getProjectStats/getDateStats/getTypeStats` 接受 `userIds` 参数并透传
6. **`OvertimeController.getStats` 放行 dept-pm + 删除 `filterStatsByPermission` 方法(必须在步骤 5 完成后)**
7. 跑 `mvn clean install -DskipTests` 确认编译;按 §8 写测试
8. 更新 `AGENTS.md` 角色权限矩阵(如有"角色 × 模块"汇总章节)
9. 提交:feat(overtime) dept-pm 视图 + 审批 + 修复 2 个信息泄露

## 11. 不在本次范围

- 前端 `OvertimeManagement.vue` 加 dept 维度 tab(用户已确认暂不改)
- dept-pm 改 / 删所辖成员加班(权限保持仅 owner / admin / 创建者本人)
- dept-pm 跨公司(`managed_company_cd`)维度的特殊处理
- 离职用户(`status='T'`)加班记录的特殊处理(按现状,数据保留)
- 周报(`WeeklyReport`)的类似扩展 —— 那是另一个独立 spec
