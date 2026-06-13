# 文档管理权限设计

> 日期：2026-06-13
> 状态：草案（待 spec 审查 + 用户确认）
> 范围：后端 `DocumentController` / `DocumentService` / `PermissionService` 及对应 mapper

## 1. 背景与目标

WBS 项目管理系统的文档管理模块当前**没有任何权限控制**：任何登录用户可调用 `GET /documents` 看到所有文档、可上传/删除任意文件。这与项目其他模块（项目、任务、周报）的权限分级不一致。

本次改造的目标是按以下 4 条业务规则统一文档管理权限：

1. **管理员** 可以查看/上传/删除 所有的文件
2. **部门项目经理** 可以查看本部门人员上传的所有文档，可以删除
3. **项目经理** 可以查看自己管理项目的人员上传的所有文档，可以删除
4. **项目负责人** 可以查看自己的项目人员上传的所有文档，可以删除

约束：

- 不新建独立的 ACL 授权表，复用现有的 `sys_user.dept_code`、`sys_user.managed_dept_codes`、`sys_user.managed_project_ids`、`sys_project.owner_id`、`sys_project_member` 多对多表、`sys_document.uploaded_by / project_id` 字段。
- 不引入新的权限注解，沿用 `PermissionService` 集中判定 + Service 调用的现有模式。
- 不破坏现有 `DocumentAccessLog` 审计语义（操作人 + 动作 + 文档）。

## 2. 术语与语义澄清

### 2.1 锚点口径：以"上传者本人"为锚（业务确认）

| 表述 | 含义 |
|------|------|
| "部门人员" | `sys_user` 中 `dept_code` 命中部门 PM 的 `managed_dept_codes` 的所有用户 |
| "自己管理项目的人员" | `project.id ∈ PM.managed_project_ids` 的项目，其 `sys_project_member` 中的所有用户 |
| "自己的项目人员" | `project.owner_id = self` 的项目，其 `sys_project_member` 中的所有用户 |

即：管的是"上传者本人"，而不是"文档所属项目"。跨部门协作场景下，文档归属按上传者 dept 计算。

### 2.2 "查看"的边界

含：列表、详情、下载、预览 4 类接口。权限判断一致，用户预期不会错位。

### 2.3 非项目 / 非周报的 general 文档

`project_id = null AND report_id = null` 的文档（如 `general/requirements/...`），按 A 口径纳入"上传者所在部门 PM"的管辖。即 general 文档不是公共资源，不属于"任何登录用户可看"。

### 2.4 MEMBER / VIEWER 沿用现有规则

需求里只定义 4 个"管理类"角色。MEMBER / VIEWER 不在本次新增规则里，保留：
- MEMBER：上传到自己参与的项目；查看自己参与项目中的所有文档；可删自己上传的。
- VIEWER：只读自己参与项目中的文档。

## 3. 权限矩阵

| 角色 | 可见（列表/详情/下载/预览） | 可上传到 | 可删除 |
|------|-------------|--------|--------|
| `ADMIN` | 全部 | 任意 `projectId`（含 null=general） | 全部 |
| `DEPT_PROJECT_MANAGER` | `uploader.deptCode ∈ managed_dept_codes`（含 general） | `project.deptCode ∈ managed_dept_codes`（含 null=general） | 同可见 |
| `PROJECT_MANAGER` | `projectId ∈ managed_project_ids` **且** `uploader ∈ project_member` | `projectId ∈ managed_project_ids` | 同可见 |
| 项目负责人（owner） | `project.ownerId = self` **且** `uploader ∈ project_member` | `project.ownerId = self` | 同可见 |
| `MEMBER` | `uploader = self` **或** `projectId ∈ 我参与` | `projectId ∈ 我参与` | `uploader = self` |
| `VIEWER` | 同 MEMBER（只读） | ❌ | ❌ |

> **自上传统一走 4.1 早返回**：`canViewDocument` 第一行 `userId == document.uploadedBy → return true` 已经覆盖所有角色的"自己上传的文档"分支，因此上面矩阵不必再为每个角色重复声明 `uploader = self`。MEMBER / VIEWER 的 `uploader = self` 是早返回直接命中；PM / owner 的自上传同理。

> **MEMBER 上传到自己未参与的项目**也拒绝；不开放"借项目上传"。VIEWER 完全禁止上传。

## 4. 关键判定逻辑（伪代码）

### 4.1 `PermissionService.canViewDocument(userId, document)`

```text
if isAdmin(userId):                                return true
if userId == document.uploadedBy:                  return true   # 自己上传的

uploader = userMapper.selectById(document.uploadedBy)

# 部门 PM：上传者本人 dept 在 managed_dept_codes 内
if isDeptProjectManager(userId) AND uploader != null
   AND isDeptManager(userId, uploader.deptCode):    return true

# PM：项目 ∈ managedProjectIds 且 上传者是该项目成员
if isProjectManager(userId) AND document.projectId != null
   AND isManagedProject(userId, document.projectId)
   AND isProjectMember(document.uploadedBy, document.projectId):
                                                   return true

# 项目负责人：project.ownerId == self 且 上传者是该项目成员
project = projectMapper.selectById(document.projectId)
if project != null AND userId == project.ownerId
   AND isProjectMember(document.uploadedBy, document.projectId):
                                                   return true

# MEMBER / VIEWER 兜底：参与的项目中的文档
if document.projectId != null
   AND isProjectMember(userId, document.projectId): return true

return false
```

### 4.2 `PermissionService.canDeleteDocument(userId, document)`

2026-06-13 业务确认：**删除权严格弱于查看权**，按"上传者在我范围内的角色"判定：

```text
if isAdmin(userId):                                              return true
if userId == document.uploadedBy:                                return true   # 自上传
if isDeptProjectManager(userId):
    uploader = userMapper.selectById(document.uploadedBy)
    if uploader != null AND uploader.deptCode != null
       AND isDeptManager(userId, uploader.deptCode):             return true
if isProjectManager(userId) AND document.projectId != null
   AND isManagedProject(userId, document.projectId)
   AND isProjectMember(document.uploadedBy, document.projectId): return true
project = projectMapper.selectById(document.projectId)
if project != null AND userId == project.ownerId
   AND isProjectMember(document.uploadedBy, document.projectId):  return true
return false   # MEMBER 仅自上传(已 early return);VIEWER 无权删
```

规则要点：
- **MEMBER** 仅能删自上传；不能删其他成员上传的（即便在同一项目）
- **VIEWER** 完全无删除权（自上传也不允许——若 VIEWER 历史上传过文档，需先提权为 MEMBER 才能清理）
- 部门 PM / 项目 PM / 项目 owner 按层级严格判定，越权即 403
- 早期"= canViewDocument"的实现存在安全漏洞（MEMBER 可删其他成员上传），2026-06-13 修复

### 4.3 `PermissionService.canUploadDocument(userId, projectId)`

```text
if isAdmin(userId):                                                  return true
if projectId == null:                                                return true   # general 任何人可上传
if isDeptProjectManager(userId):
    project = projectMapper.selectById(projectId)
    return project != null
           AND project.deptCode != null
           AND isDeptManager(userId, project.deptCode)
if isProjectManager(userId):
    return isManagedProject(userId, projectId)
if userId == project.ownerId (when projectId != null):              return true   # 项目负责人
# MEMBER / VIEWER 兜底：必须是自己参与的项目
return isProjectMember(userId, projectId)   # MEMBER: 是项目成员;VIEWER: 也是项目成员但下游会被上传拒绝
```

**关键说明**：

- `canEditProject` **不覆盖 MEMBER**（现有实现只覆盖 admin/createdBy/owner/dept-pm/PM），所以不能直接复用。**本轮新增的 MEMBER 兜底分支 `return isProjectMember(userId, projectId)` 是行为变更**，解决 MEMBER 当前完全无法上传到参与项目的问题。
- VIEWER 即便命中 `isProjectMember`，仍由上层业务按角色黑名单（VIEWER 角色拒上传）拒绝。`canUploadDocument` 只判断"项目归属"维度。
- **`PermissionService.canEditDocument` 老方法按设计错误地复用了 `canEditProject`**，导致 MEMBER 走该方法无法上传。**本轮改造一并替换**：删除 `canEditDocument`，`DocumentService.uploadDocument` 改调 `canUploadDocument`。

### 4.4 数据范围预计算（列表过滤）

为避免对每行文档都跑 4 次单文档判定，列表接口改为一次性预计算两个 ID 集合，下推到 SQL `IN (...)`：

```text
getAccessibleUploaderIds(userId):
    if isAdmin: return null   # null = 全部
    if isDeptProjectManager:
        return userMapper.selectIdsByDeptCodes(parseManagedDeptCodes(self))
    if isProjectManager:
        ids = projectMemberMapper.selectMemberIdsByProjectIds(parseManagedProjectIds(self))
        ids.add(self)
        return ids
    if owner of any project:
        # 合并所有 owner=我 的项目的成员
        ownerProjects = projectMapper.selectIdsByOwner(self)
        ids = projectMemberMapper.selectMemberIdsByProjectIds(ownerProjects)
        ids.add(self)
        return ids
    # MEMBER / VIEWER
    ids = projectMemberMapper.selectMemberIdsByProjectIds(
            projectMemberMapper.selectProjectIdsByUserId(self))
    ids.add(self)
    return ids

getAccessibleProjectIdsForDoc(userId):
    if isAdmin: return null
    if isProjectManager: return parseManagedProjectIds(self)
    if isDeptProjectManager: return emptyList()    # 部门 PM 不按项目维度管辖,projectIds 留空 → 4.4 SQL 的 <if> 守卫会跳过此分支
    if owner of any project:
        return projectMapper.selectIdsByOwner(self)
    # MEMBER / VIEWER
    return projectMemberMapper.selectProjectIdsByUserId(self)
```

然后列表 SQL：

```sql
SELECT * FROM sys_document
WHERE status = 'active'
  AND ( <if test="uploaderIds != null and uploaderIds.size() > 0">
            uploaded_by IN
            <foreach collection="uploaderIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
       </if>
       <if test="projectIds != null and projectIds.size() > 0">
            OR project_id IN
            <foreach collection="projectIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
       </if>
     )
ORDER BY created_at DESC
```

**MyBatis 空列表陷阱**：两个 `<if>` 守卫必须显式判断 `size() > 0`，否则 DEPT_PROJECT_MANAGER 的 `projectIds` 恒为空，会拼出 `OR project_id IN ()` —— MyBatis 不同版本对此处理不同（有的抛 `IllegalArgumentException`，有的静默匹配 0 行），不可依赖。admin 走 `selectAll()` 不走这个 SQL。

## 5. 数据库 / SQL 改动

### 5.1 新增 mapper 方法

```java
// UserMapper
List<String> selectIdsByDeptCodes(@Param("deptCodes") List<String> deptCodes);
//   SELECT id FROM sys_user
//   WHERE dept_code IN (...)
//     AND status = 'active'    -- 关键:必须排除离职/休职用户,避免部门 PM 看到死账号上传的文档

// ProjectMemberMapper
List<String> selectMemberIdsByProjectIds(@Param("projectIds") List<String> projectIds);
//   SELECT DISTINCT user_id FROM sys_project_member WHERE project_id IN (...)
//   (无需 status 过滤:sys_project_member 表本身没有 user_id 软删除字段,离职用户保留关联)

// DocumentMapper
List<Document> selectByAccessibleScope(
    @Param("uploaderIds") Collection<String> uploaderIds,
    @Param("projectIds")  Collection<String> projectIds,
    @Param("category")    String category,
    @Param("reportId")    String reportId,
    @Param("taskId")      String taskId,
    @Param("projectId")   String projectId  // 可选,精确过滤某个项目的文档
);
```

### 5.2 不改 schema

无新增表 / 字段 / 索引。`uploaded_by / dept_code / managed_dept_codes / managed_project_ids / owner_id / project_id` 现成字段已足够。

### 5.3 数据清理（一次性 SQL）

历史数据可能存在 `uploaded_by = 'system-default'` 的孤儿记录，按 A 口径会落到所有人 uploaderIds 集合外（无人能管也无人能删）。建议：

```sql
-- 1) 切换前先备份（管理员手动执行,保留 CSV/导出）
--    SELECT * FROM sys_document
--    WHERE uploaded_by NOT IN (SELECT id FROM sys_user WHERE status = 'active')
--       OR uploaded_by IS NULL
--       OR uploaded_by = 'system-default';

-- 2) 软删孤儿（status='deleted' 是终态,需要重新启用必须手工 UPDATE）
UPDATE sys_document
SET status = 'deleted', updated_at = NOW()
WHERE uploaded_by NOT IN (SELECT id FROM sys_user WHERE status = 'active')
   OR uploaded_by IS NULL
   OR uploaded_by = 'system-default';
```

由 admin 在切换前手动跑一次，**不写入自动迁移脚本**——保留人工确认窗口。软删后除非手工翻回 `status='active'`，否则无法恢复，建议先导出 CSV 留底。

## 6. 接口改造（DocumentController）

### 6.1 列表 / 范围查询（统一加 userId 参数）

| 原接口 | 新签名 |
|--------|--------|
| `GET /documents` | `getAllDocuments(@RequestHeader X-User-Id)` → `getAllDocuments(userId)` |
| `GET /documents/{id}` | `getDocumentById(id, userId)`（详情：过 canViewDocument） |
| `GET /documents/project/{projectId}` | `getDocumentsByProjectId(projectId, userId)` |
| `GET /documents/task/{taskId}` | `getDocumentsByTaskId(taskId, userId)` |
| `GET /documents/category/{category}` | `getDocumentsByCategory(category, userId)` |
| `GET /documents/report/{reportId}` | `getDocumentsByReportId(reportId, userId)` |
| `GET /documents/report/{reportId}/category/{category}` | `getDocumentsByReportIdAndCategory(reportId, category, userId)` |
| `GET /documents/user/{userId}` | 4 类管理角色（admin / dept-pm / PM / 项目负责人）均可调，需先按可见范围过滤；MEMBER / VIEWER 调用返回 403（避免越权探测） |
| `GET /documents/project/{projectId}/count` | `getDocumentCountByProjectId(projectId, userId)`（count 也按可见范围过滤） |

### 6.2 下载 / 预览

| 接口 | 改造 |
|------|------|
| `GET /documents/{id}/download` | `downloadDocument(id, userId)`；过 `canViewDocument` |
| `GET /documents/{id}/preview` | `previewDocument(id, userId)`；过 `canViewDocument` |

### 6.3 写操作

| 接口 | 改造 |
|------|------|
| `POST /documents/upload` | `uploadDocument(...)`；过 `canUploadDocument(userId, projectId)` |
| `PUT /documents/{id}` | `updateDocument(id, ..., userId)`；过 `canDeleteDocument(userId, document)`（更新权同删除权） |
| `DELETE /documents/{id}` | `deleteDocument(id, userId)`；过 `canDeleteDocument` |

### 6.4 错误码

权限不足统一抛 `BusinessException(403, "无文档操作权限")`，由 `GlobalExceptionHandler` 映射成 `Result.error(...)`。前端按 `code === 403` 给出"权限不足"提示。

### 6.5 前端 API 调用方同步（必做）

8 个列表 / 范围查询接口的签名变更（每个都加 `X-User-Id` header 或 query 参数）是**破坏性变更**。实施 PR 必须同时更新所有前端调用方——通过 `services/api.ts` 的中央 API 客户端统一改动，不在每个 view 里散改。**验证步骤**：实施完成后 `grep -r "documents" frontend/src` 确认所有调用点都已传 `userId`。

前端已有的角色过滤逻辑（如存在）按 CLAUDE.md "前端无权限守卫" 原则移除——本次不动前端过滤逻辑、仅保证后端返回值正确即可。

## 7. 文件改动清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `PermissionService.java` | 改 | 新增 5 个方法 + 注入 `UserMapper`/`ProjectMapper`（已有） |
| `UserMapper.java` + `UserMapper.xml` | 改 | 新增 `selectIdsByDeptCodes` |
| `ProjectMemberMapper.java` + `ProjectMemberMapper.xml` | 改 | 新增 `selectMemberIdsByProjectIds` |
| `ProjectMapper.java` + `ProjectMapper.xml` | 改 | **实施前先查 `ProjectMapper.java` 是否已有 `selectIdsByOwner`，没有则补上**（owner 维度列表查询所需） |
| `DocumentMapper.java` + `DocumentMapper.xml` | 改 | 新增 `selectByAccessibleScope` |
| `DocumentService.java` | 改 | 14 个方法全部加 `userId` 参数 + 权限判定调用 |
| `PermissionService.java`（删 `canEditDocument`） | 改 | 删除错误的 `canEditDocument` 方法（实施时以方法名为准定位，不要按行号），文档上传改调 `canUploadDocument`；若无其它调用方则一并删除 |
| `DocumentController.java` | 改 | 所有接口从 header 取 `X-User-Id` 透传 |
| `docs/superpowers/specs/2026-06-13-document-permission-design.md` | 新增 | 本文档 |

前端无强制改动；如果前端目前在前端做角色过滤，需要移除（CLAUDE.md 明确"无前端守卫，后端是唯一权限源"）。

## 8. 验证计划

由于 `backend/src/test/` 当前为空（AGENTS.md 提到），不引入新的测试框架；改为手工/接口级验证清单：

1. **admin**：`GET /documents` 返回全量；可上传到任意 `projectId`；可删任意文档。
2. **部门 PM A**：
   - `GET /documents` 仅含 `uploader.dept_code ∈ managed_dept_codes` 的文档。
   - 上传到 `project.dept_code ∈ managed_dept_codes` 的项目成功；上传到其他部门的项目 403。
   - 删自己管辖上传者的文档成功；删非管辖 403。
3. **PM B**：
   - 仅看到 `managed_project_ids` 范围内项目的成员上传的文档。
   - 上传 / 删除仅限这些项目。
4. **项目负责人 C（owner）**：
   - 仅看到 `owner=我` 的项目成员上传的文档。
5. **MEMBER D**：
   - 看不到非参与项目的文档；看不到他人上传的文档；删自己上传的可；删他人 403。
   - 上传到非参与项目 403。
6. **VIEWER E**：与 MEMBER 类似但上传/删除全 403。
7. **general 文档**：上传者部门 PM 能看/删；非部门 PM 不能看。
8. **跨部门协作**：项目属于部门 A 但上传者属于部门 B 时，A 的 PM 看不到、B 的 PM 看到（A 口径预期）。

## 9. 风险与补充

- **跨部门协作归属**：A 口径下"项目归属部门"与"上传者部门"不一致时按上传者计。需对业务方明确告知，否则可能引发"为什么我看不到本部门项目的某些文档"的工单。
- **`uploader.deptCode` 为 null**：纯 admin / 历史外包账号可能无 dept_code，部门 PM 看不到这些上传者的文档——符合 A 口径预期。
- **`managed_dept_codes / managed_project_ids` JSON 字段解析失败**：`PermissionService.parseDeptCodes / parseProjectIds` 已 try-catch 兜底返回空列表，等价于"无管辖范围"（fail-closed）。
- **审计不变**：`DocumentAccessLog` 仍记录 userId + action + documentId，权限规则变更不影响审计日志写入；运维侧仍可追责。
- **前端角色过滤**（若存在）：按 CLAUDE.md"前端无权限守卫"原则，本次改造后端后，前端对应的前端过滤逻辑成为冗余——建议下次重构清理，本次不动。
- **MEMBER 上传到自己参与的项目**：`canUploadDocument` 需要在 `canEditProject` 之外补 `isMember AND isProjectMember` 分支；该分支目前缺失，需新增。

## 10. 不在本轮范围

- 周报内嵌文档的特殊授权（周报附件仍走 `report_id` 维度，由周报权限间接控制，不重新设计）。
- 文档版本管理 / 父文档链 (`parent_id`) 的权限继承——本轮不处理，后续若 PM 要求"删父文档必须级联审"再单独设计。
- 文档标签 / 全文搜索的权限过滤——搜索结果需叠加本轮 `canViewDocument` 过滤，超出本 spec。

## 11. 决策记录

- **2026-06-13** 锚点口径：业务确认以"上传者本人"为锚。
- **2026-06-13** general 文档：按上传者所在部门 PM 管辖，不当作公共资源。
- **2026-06-13** 查看边界：列表 + 详情 + 下载 + 预览 一致。
- **2026-06-13** 删除权限 = 查看权限（不开放额外白名单）。
- **2026-06-13** 不新建 ACL 表，复用现成字段。