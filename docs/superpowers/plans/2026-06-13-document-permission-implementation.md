# 文档管理权限实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `sys_document` 的列表/详情/下载/预览/上传/更新/删除接口加角色化权限控制，遵循 spec `2026-06-13-document-permission-design.md` 的判定语义。

**Architecture:** 不新建 ACL 表。集中在 `PermissionService` 新增 5 个方法（2 个数据范围预计算 + 3 个单文档判定），`DocumentMapper` 新增 1 个 `selectByAccessibleScope` SQL（带 `<if>` 空列表守卫 + 显式 `AND (1=0 OR ... OR ...)` 分组），`DocumentService` 14 个方法全部加 `userId` 参数并按需调用判定，`DocumentController` 给 10 个方法补 `@RequestHeader("X-User-Id")`（5 个写操作已带，无需重写）。前端 `services/api.ts` 已在 `request()` 统一注入 `X-User-Id`（line 60），无需改动。

**Tech Stack:** Spring Boot 3.2.0 + MyBatis + MySQL + Java 17。MyBatis XML 中 `<foreach>` + `<if>` 动态 SQL。

**Spec:** `docs/superpowers/specs/2026-06-13-document-permission-design.md`

---

## 文件结构（先看全局）

| 文件 | 类型 | 责任 |
|------|------|------|
| `UserMapper.java` + `UserMapper.xml` | 改 | 加 `selectIdsByDeptCodes(List<String>)` |
| `ProjectMemberMapper.xml` | 改 | 加 `selectMemberIdsByProjectIds(List<String>)` |
| `ProjectMapper.java` | 改 | 已有 `selectIdsByOwner`,**无需改动**（仅核对存在） |
| `PermissionService.java` | 改 | 加 5 个方法；删除错误的 `canEditDocument` |
| `DocumentMapper.java` + `DocumentMapper.xml` | 改 | 加 `selectByAccessibleScope` |
| `DocumentService.java` | 改 | 14 个方法加 `userId` 参数 + 判定 |
| `DocumentController.java` | 改 | 8 个 GET 接口补 `@RequestHeader("X-User-Id")` |

---

## 验证前置条件

- 后端 MySQL 在 `localhost:3306` 可用（`db_webwbs` 数据库存在）。
- 编译验证：`cd backend && mvn -pl . test -DfailIfNoTests=false` （项目无 Java 测试,加 `-DfailIfNoTests=false` 避免空测试目录报错）。
- 类型检查：`cd frontend && npx vue-tsc` （前端无变更,可选）。

---

### Task 1: UserMapper 新增 `selectIdsByDeptCodes`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/UserMapper.java`
- Modify: `backend/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: 在 UserMapper.java 接口添加方法签名**

打开 `backend/src/main/java/com/wbs/project/mapper/UserMapper.java`，在 `selectByIds` 后添加：

```java
/**
 * 按部门编码列表查询在职用户 ID(角色管理 v2:部门 PM 数据范围预计算用)
 * 排除离职用户(status='T'),避免部门 PM 看到死账号上传的文档
 */
List<String> selectIdsByDeptCodes(@Param("deptCodes") List<String> deptCodes);
```

- [ ] **Step 2: 在 UserMapper.xml 添加 SQL**

打开 `backend/src/main/resources/mapper/UserMapper.xml`，在 `selectByIds` `<select>` 块之后、`insert` 之前添加：

```xml
<!-- 文档权限（2026-06-13）：按部门编码列表查在职用户 ID -->
<select id="selectIdsByDeptCodes" resultType="java.lang.String">
    SELECT id FROM sys_user
    WHERE dept_code IN
    <foreach collection="deptCodes" item="dc" open="(" separator="," close=")">
        #{dc}
    </foreach>
      AND status != 'T'
</select>
```

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/wbs/project/mapper/UserMapper.java \
        backend/src/main/resources/mapper/UserMapper.xml
git commit -m "feat(doc-permission): UserMapper 新增 selectIdsByDeptCodes"
```

---

### Task 2: ProjectMemberMapper 新增 `selectMemberIdsByProjectIds`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/ProjectMemberMapper.java`
- Modify: `backend/src/main/resources/mapper/ProjectMemberMapper.xml`

- [ ] **Step 1: 在 ProjectMemberMapper.java 添加方法**

打开 `backend/src/main/java/com/wbs/project/mapper/ProjectMemberMapper.java`，在 `isProjectMember` 后添加：

```java
/**
 * 批量按项目 ID 查成员 user_id(DISTINCT 去重,用于 PM/项目负责人数据范围预计算)
 */
List<String> selectMemberIdsByProjectIds(@Param("projectIds") List<String> projectIds);
```

- [ ] **Step 2: 在 ProjectMemberMapper.xml 添加 SQL**

打开 `backend/src/main/resources/mapper/ProjectMemberMapper.xml`，在 `isProjectMember` 后添加：

```xml
<!-- 文档权限（2026-06-13）：批量按项目 ID 查成员 user_id -->
<select id="selectMemberIdsByProjectIds" resultType="java.lang.String">
    SELECT DISTINCT user_id
    FROM sys_project_member
    WHERE project_id IN
    <foreach collection="projectIds" item="pid" open="(" separator="," close=")">
        #{pid}
    </foreach>
</select>
```

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/wbs/project/mapper/ProjectMemberMapper.java \
        backend/src/main/resources/mapper/ProjectMemberMapper.xml
git commit -m "feat(doc-permission): ProjectMemberMapper 新增 selectMemberIdsByProjectIds"
```

---

### Task 3: PermissionService 新增 2 个数据范围预计算方法

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/PermissionService.java`

- [ ] **Step 1: 确认 ProjectMapper.selectIdsByOwner 已存在**

Run: `grep -n "selectIdsByOwner" backend/src/main/java/com/wbs/project/mapper/ProjectMapper.java`
Expected: 至少 1 行命中。如果未命中 → 停止并报告。

- [ ] **Step 2: 在 PermissionService.java 添加 `getAccessibleUploaderIds`**

打开 `backend/src/main/java/com/wbs/project/service/PermissionService.java`，找到现有 `parseManagedProjectIds` 工具方法（line ~644 附近），在它后面添加：

```java
// ============ 文档权限数据范围（2026-06-13） ============

/**
 * 返回当前用户能看/管的「文档上传者」userId 集合。
 * admin 返回 null（语义：不限）；MEMBER/VIEWER 兜底含自己 + 参与项目的成员。
 */
public Set<String> getAccessibleUploaderIds(String userId) {
    if (userId == null) {
        return Collections.emptySet();
    }
    if (isAdmin(userId)) {
        return null;
    }
    Set<String> ids = new HashSet<>();
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
    if (isProjectManager(userId)) {
        User u = userMapper.selectById(userId);
        if (u != null) {
            List<String> managed = parseProjectIds(u.getManagedProjectIds());
            if (!managed.isEmpty()) {
                ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(managed));
            }
        }
        ids.add(userId); // 自己上传的总能看到
        return ids;
    }
    // 项目负责人 / MEMBER / VIEWER 兜底
    List<String> ownerProjects = projectMapper.selectIdsByOwner(userId);
    if (!ownerProjects.isEmpty()) {
        ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(ownerProjects));
    }
    List<String> participated = projectMemberMapper.selectProjectIdsByUserId(userId);
    if (!participated.isEmpty()) {
        ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(participated));
    }
    ids.add(userId);
    return ids;
}

/**
 * 返回当前用户能看/管的「文档所属项目」projectId 集合。
 * admin 返回 null（不限）；DEPT_PM 不按项目维度管辖，返回空集合。
 */
public Set<String> getAccessibleProjectIdsForDoc(String userId) {
    if (userId == null) {
        return Collections.emptySet();
    }
    if (isAdmin(userId)) {
        return null;
    }
    if (isProjectManager(userId)) {
        User u = userMapper.selectById(userId);
        if (u == null) return Collections.emptySet();
        return new HashSet<>(parseProjectIds(u.getManagedProjectIds()));
    }
    if (isDeptProjectManager(userId)) {
        return Collections.emptySet(); // 部门 PM 走 uploader 维度,projectIds 留空 → SQL 的 <if> 守卫跳过
    }
    Set<String> ids = new HashSet<>(projectMapper.selectIdsByOwner(userId));
    ids.addAll(projectMemberMapper.selectProjectIdsByUserId(userId));
    return ids;
}
```

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`（确认无导入错误）

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/PermissionService.java
git commit -m "feat(doc-permission): PermissionService 数据范围预计算方法"
```

---

### Task 4: PermissionService 新增 3 个判定方法 + 删除错误 `canEditDocument`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/PermissionService.java`

- [ ] **Step 1: 确认无外部调用方引用 `canEditDocument`**

Run: `grep -rn "canEditDocument" backend/src/main/java/com/wbs/project/`
Expected: 只在 `PermissionService.java` 自身出现 1-2 次（方法定义 + 自调用）。

如果 `DocumentService` 里有 `permissionService.canEditDocument(...)` 调用，需同时改 `DocumentService.uploadDocument` 调 `canUploadDocument`。在本任务里一并处理。

- [ ] **Step 2: 在 PermissionService.java 添加 `canViewDocument` / `canDeleteDocument` / `canUploadDocument`**

打开 `PermissionService.java`，在 `getAccessibleProjectIdsForDoc` 之后添加：

```java
/**
 * 是否能查看指定文档（list/detail/download/preview 共用）
 */
public boolean canViewDocument(String userId, Document doc) {
    if (userId == null || doc == null) {
        return false;
    }
    if (isAdmin(userId)) return true;
    // 自上传统一早返回（覆盖所有角色）
    if (userId.equals(doc.getUploadedBy())) return true;

    // 部门 PM:上传者 dept 在 managed_dept_codes 内（含 general）
    User uploader = userMapper.selectById(doc.getUploadedBy());
    if (isDeptProjectManager(userId) && uploader != null
            && isDeptManager(userId, uploader.getDeptCode())) {
        return true;
    }
    // PM:doc.projectId ∈ managedProjectIds 且 uploader 是项目成员
    if (isProjectManager(userId) && doc.getProjectId() != null
            && isManagedProject(userId, doc.getProjectId())
            && isProjectMember(doc.getUploadedBy(), doc.getProjectId())) {
        return true;
    }
    // 项目负责人:project.ownerId == self 且 uploader 是项目成员
    if (doc.getProjectId() != null) {
        Project p = projectMapper.selectById(doc.getProjectId());
        if (p != null && userId.equals(p.getOwnerId())
                && isProjectMember(doc.getUploadedBy(), doc.getProjectId())) {
            return true;
        }
        // MEMBER / VIEWER 兜底:参与的项目
        if (isProjectMember(userId, doc.getProjectId())) {
            return true;
        }
    }
    return false;
}

/**
 * 删除权限（本轮规则下同查看权限）
 */
public boolean canDeleteDocument(String userId, Document doc) {
    return canViewDocument(userId, doc);
}

/**
 * 上传权限校验
 */
public boolean canUploadDocument(String userId, String projectId) {
    if (userId == null) return false;
    if (isAdmin(userId)) return true;
    if (projectId == null) return true; // general 任何人可上传
    if (isDeptProjectManager(userId)) {
        Project p = projectMapper.selectById(projectId);
        return p != null && p.getDeptCode() != null
                && isDeptManager(userId, p.getDeptCode());
    }
    if (isProjectManager(userId)) {
        return isManagedProject(userId, projectId);
    }
    // 项目负责人 / MEMBER 兜底
    if (isProjectMember(userId, projectId)) {
        // MEMBER 通过此分支放行;VIEWER 由 Controller 角色白名单另外拒
        Project p = projectMapper.selectById(projectId);
        if (p != null && userId.equals(p.getOwnerId())) return true;
        // MEMBER 但非 owner:仅自己参与项目可上传
        return !isViewer(userId);
    }
    return false;
}

/** 抛错版本：用于 upload/delete/update 入口 */
public void requireViewDocument(String userId, Document doc) {
    if (!canViewDocument(userId, doc)) {
        throw new BusinessException(403, "无文档查看权限");
    }
}

public void requireDeleteDocument(String userId, Document doc) {
    if (!canDeleteDocument(userId, doc)) {
        throw new BusinessException(403, "无文档删除权限");
    }
}

public void requireUploadDocument(String userId, String projectId) {
    if (!canUploadDocument(userId, projectId)) {
        throw new BusinessException(403, "无文档上传权限");
    }
}
```

需要的 imports 已在文件顶部存在（`BusinessException`, `Project`, `Document`, `User`）。

- [ ] **Step 3: 删除错误的 `canEditDocument` 方法**

打开 `PermissionService.java`，找到：

```java
/**
 * 是否能编辑文档(上传/删除)
 */
public boolean canEditDocument(String userId, String projectId) {
    return canEditProject(userId, projectId);
}
```

整段删除（含上方注释）。

如果 Step 1 发现 `DocumentService` 调到了 `canEditDocument`，记下位置，下一阶段（DocumentService 改造）一并改。

- [ ] **Step 4: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/PermissionService.java
git commit -m "feat(doc-permission): 新增 canView/canDelete/canUploadDocument + 删除错误 canEditDocument"
```

---

### Task 5: DocumentMapper 新增 `selectByAccessibleScope`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/DocumentMapper.java`
- Modify: `backend/src/main/resources/mapper/DocumentMapper.xml`

- [ ] **Step 1: 在 DocumentMapper.java 添加方法签名**

打开 `backend/src/main/java/com/wbs/project/mapper/DocumentMapper.java`，在 `countByProjectId` 后添加：

```java
import java.util.Collection;

/**
 * 按"可见上传者"和"可见项目"两个维度取文档并集。
 * 任一参数为 null（admin）或空集合（DEPT_PM 的 projectIds）时跳过对应分支。
 */
List<Document> selectByAccessibleScope(
    @Param("uploaderIds") Collection<String> uploaderIds,
    @Param("projectIds")  Collection<String> projectIds,
    @Param("category")    String category,
    @Param("reportId")    String reportId,
    @Param("taskId")      String taskId,
    @Param("projectId")   String projectId);
```

- [ ] **Step 2: 在 DocumentMapper.xml 添加 SQL**

打开 `backend/src/main/resources/mapper/DocumentMapper.xml`，在 `countByProjectId` 后添加：

```xml
<!-- 文档权限（2026-06-13）：按可见上传者 ∪ 可见项目过滤 -->
<select id="selectByAccessibleScope" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM sys_document
    <where>
        status = 'active'
        <if test="category != null and category != ''">
            AND category = #{category}
        </if>
        <if test="reportId != null and reportId != ''">
            AND report_id = #{reportId}
        </if>
        <if test="taskId != null and taskId != ''">
            AND task_id = #{taskId}
        </if>
        <if test="projectId != null and projectId != ''">
            AND project_id = #{projectId}
        </if>
        <!-- 范围 OR 必须显式分组,否则与上面的 AND 优先级错乱 -->
        <if test="(uploaderIds != null and uploaderIds.size() > 0) or (projectIds != null and projectIds.size() > 0)">
            AND (1=0
            <if test="uploaderIds != null and uploaderIds.size() > 0">
                OR uploaded_by IN
                <foreach collection="uploaderIds" item="uid" open="(" separator="," close=")">
                    #{uid}
                </foreach>
            </if>
            <if test="projectIds != null and projectIds.size() > 0">
                OR project_id IN
                <foreach collection="projectIds" item="pid" open="(" separator="," close=")">
                    #{pid}
                </foreach>
            </if>
            )
        </if>
    </where>
    ORDER BY created_at DESC
</select>
```

⚠️ **MyBatis 守卫 + 优先级**：
1. 两个 `<if>` 都必须判 `size() > 0`，否则 DEPT_PM 的空 `projectIds` 会拼出 `OR project_id IN ()`。
2. **范围条件必须显式分组**：用 `AND (1=0 OR uploaded_by IN (...) OR project_id IN (...))` 闭合。裸写 `OR` 会让 MyBatis 生成 `... AND category=? OR uploaded_by IN (...)`——`AND` 优先级高于 `OR`，导致 category 等精确过滤失效。

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/wbs/project/mapper/DocumentMapper.java \
        backend/src/main/resources/mapper/DocumentMapper.xml
git commit -m "feat(doc-permission): DocumentMapper 新增 selectByAccessibleScope"
```

---

### Task 6: DocumentService — 列表/范围查询方法加 userId 过滤

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/DocumentService.java`

- [ ] **Step 1: 注入 PermissionService 与 ProjectMapper**

打开 `backend/src/main/java/com/wbs/project/service/DocumentService.java`，修改类头：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentAccessLogMapper documentAccessLogMapper;
    private final ForeignKeyValidationService foreignKeyValidationService;
    private final PermissionService permissionService;     // 新增(Task 6)
    private final ProjectMapper projectMapper;            // 新增(Task 6)

    @Value("${document.upload.path:./uploads/documents}")
    private String uploadPath;
    ...
}
```

（ProjectMapper 必须在 `getDocumentsByUserId` 之前注入，因为它依赖 `projectMapper.selectIdsByOwner`。）

（同时 import `BusinessException`：`import com.wbs.project.exception.BusinessException;` 如果文件未导入。）

- [ ] **Step 2: 改造 `getAllDocuments` → `getAllDocuments(userId)`**

替换：

```java
public List<Document> getAllDocuments() {
    return documentMapper.selectAll();
}
```

为：

```java
public List<Document> getAllDocuments(String userId) {
    if (permissionService.isAdmin(userId)) {
        return documentMapper.selectAll();
    }
    Set<String> uploaderIds = permissionService.getAccessibleUploaderIds(userId);
    Set<String> projectIds  = permissionService.getAccessibleProjectIdsForDoc(userId);
    return documentMapper.selectByAccessibleScope(uploaderIds, projectIds, null, null, null, null);
}
```

- [ ] **Step 3: 改造 `getDocumentsByProjectId` → 加 userId**

```java
public List<Document> getDocumentsByProjectId(String userId, String projectId) {
    if (permissionService.isAdmin(userId)) {
        return documentMapper.selectByProjectId(projectId);
    }
    Set<String> uploaderIds = permissionService.getAccessibleUploaderIds(userId);
    Set<String> projectIds  = permissionService.getAccessibleProjectIdsForDoc(userId);
    projectIds.add(projectId); // 显式覆盖查询目标
    return documentMapper.selectByAccessibleScope(uploaderIds, projectIds, null, null, null, projectId);
}
```

- [ ] **Step 4: 改造其他 6 个范围查询方法（含 `getDocumentsByUserId`）**

按相同模式改造（每个方法新增第一个参数 `String userId`）：

| 旧方法 | 新方法体 |
|--------|----------|
| `getDocumentsByTaskId(taskId)` | taskId 传给 `selectByAccessibleScope` 第 6 参；不显式 add projectId |
| `getDocumentsByCategory(category)` | category 传给第 4 参 |
| `getDocumentsByUserId(userId)` | 仅 admin/dept-pm/pm/owner 可调（按 spec 6.1），先校验再查；含私有 `isOwnerOfAnyProject` 助手 |
| `getDocumentsByReportId(reportId)` | reportId 传给第 5 参 |
| `getDocumentsByReportIdAndCategory(reportId, category)` | reportId+category |
| `getDocumentsByProjectIdAndCategory(projectId, category)` | projectId+category |

**`getDocumentsByUserId` 实现（含 `isOwnerOfAnyProject` 助手）**：

```java
public List<Document> getDocumentsByUserId(String actorId, String targetUserId) {
    if (!(permissionService.isAdmin(actorId)
            || permissionService.isDeptProjectManager(actorId)
            || permissionService.isProjectManager(actorId)
            || isOwnerOfAnyProject(actorId))) {
        throw new BusinessException(403, "无该用户文档查看权限");
    }
    Set<String> uploaderIds = permissionService.getAccessibleUploaderIds(actorId);
    Set<String> projectIds  = permissionService.getAccessibleProjectIdsForDoc(actorId);
    return documentMapper.selectByAccessibleScope(uploaderIds, projectIds, null, null, null, null)
            .stream()
            .filter(d -> targetUserId.equals(d.getUploadedBy()))
            .collect(Collectors.toList());
}

private boolean isOwnerOfAnyProject(String userId) {
    return !projectMapper.selectIdsByOwner(userId).isEmpty();
}
```

> **Step 3 冗余过滤澄清**：`getDocumentsByProjectId` 同时 `projectIds.add(projectId)` 并把 projectId 传给第 7 参——这是**故意的防御性冗余**。SQL 第 7 参 `projectId` 是精确过滤，set 里 add 是兜底（如果未来 SQL 改动去掉精确过滤，set 仍生效）。保留两者不冲突，按精确过滤为最终结果。

> **Controller 端重命名提示**（Task 9）：`getDocumentsByUserId` 的 path variable 与 actor 参数同名 `userId`，在 controller 里把局部变量重命名为 `actorId` / `targetUserId` 避免混淆。

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/DocumentService.java
git commit -m "feat(doc-permission): DocumentService 列表方法按用户范围过滤"
```

---

### Task 7: DocumentService — 单文档方法加权限判定（detail/download/preview）

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/DocumentService.java`

- [ ] **Step 1: 改造 `getDocumentById` → 加 userId**

```java
public Document getDocumentById(String userId, String id) {
    Document doc = documentMapper.selectById(id);
    if (doc == null) return null;
    if (!"active".equals(doc.getStatus())) return null;
    permissionService.requireViewDocument(userId, doc);
    return doc;
}
```

- [ ] **Step 2: 改造 `downloadDocument` → 加 userId**

```java
public byte[] downloadDocument(String userId, String id) {
    Document doc = documentMapper.selectById(id);
    if (doc == null) throw new IllegalArgumentException("文档不存在");
    if (!"active".equals(doc.getStatus())) throw new IllegalArgumentException("文档已删除");
    permissionService.requireViewDocument(userId, doc);
    documentMapper.incrementDownloadCount(id);
    logAccess(id, userId, "download", null);
    try {
        Path path = Paths.get(doc.getFilePath());
        return Files.readAllBytes(path);
    } catch (IOException e) {
        log.error("读取文件失败: path={}", doc.getFilePath(), e);
        throw new RuntimeException("读取文件失败");
    }
}
```

- [ ] **Step 3: 改造 `previewDocument`（如果单独存在）或合并到 download**

当前 `DocumentService` 没有独立的 `previewDocument`（逻辑在 controller），不需要动 service。Controller 改 `downloadDocument` 调用即可（见 Task 9）。

- [ ] **Step 4: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/DocumentService.java
git commit -m "feat(doc-permission): DocumentService 详情/下载加权限判定"
```

---

### Task 8: DocumentService — 写操作加权限判定（upload/update/delete）

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/DocumentService.java`

- [ ] **Step 1: 改造 `uploadDocument` → 加 userId 校验**

修改方法签名首参位置：

```java
@Transactional
public Document uploadDocument(String userId, MultipartFile file, String name, String category,
                            String projectId, String taskId, String parentId, String reportId,
                            String description) {
    log.info("开始上传文档: fileName={}, category='{}', projectId={}, taskId={}, reportId={}, uploadedBy={}",
            file.getOriginalFilename(), category, projectId, taskId, reportId, userId);

    validateFile(file);

    // 新增:权限校验
    permissionService.requireUploadDocument(userId, projectId);

    foreignKeyValidationService.validateDocumentUpload(projectId, taskId, userId, parentId, reportId);
    ...
    document.setUploadedBy(userId != null && !userId.isEmpty() ? userId : "system-default");
    ...
}
```

注意：原签名是 `(MultipartFile, ..., String uploadedBy)`，**新签名把 `userId` 提到第一位**。Controller 改调用时相应调整（见 Task 9）。

- [ ] **Step 2: 改造 `updateDocument` → 加 userId 校验**

```java
@Transactional
public Document updateDocument(String userId, String id, String name, String category,
                            String projectId, String taskId, String parentId,
                            String description) {
    Document doc = documentMapper.selectById(id);
    if (doc == null) throw new IllegalArgumentException("文档不存在");
    if (!"active".equals(doc.getStatus())) throw new IllegalArgumentException("文档已删除");
    permissionService.requireDeleteDocument(userId, doc);  // 更新权 = 删除权

    foreignKeyValidationService.validateDocumentUpdate(projectId, taskId, parentId);
    ...
}
```

- [ ] **Step 3: 改造 `deleteDocument` → 加 userId 校验**

```java
@Transactional
public void deleteDocument(String userId, String id) {
    Document doc = documentMapper.selectById(id);
    if (doc == null) throw new IllegalArgumentException("文档不存在");
    if (!"active".equals(doc.getStatus())) throw new IllegalArgumentException("文档已删除");
    permissionService.requireDeleteDocument(userId, doc);
    documentMapper.deleteById(id);
    ...
}
```

- [ ] **Step 4: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/DocumentService.java
git commit -m "feat(doc-permission): DocumentService 上传/更新/删除加权限判定"
```

---

### Task 9: DocumentController 补 `X-User-Id` 头 + 调新签名

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/controller/DocumentController.java`

- [ ] **Step 1: 给所有缺 `X-User-Id` 的方法补 `@RequestHeader`**

Controller 共 15 个方法，其中 5 个（upload/download/preview/update/delete）已正确标注 `@RequestHeader(value = "X-User-Id", required = false) String userId`。剩余 10 个方法补上：

| 方法 | 当前 userId 参数？ |
|------|--------|
| `getAllDocuments()` | ❌ 没有 → 加上 |
| `getDocumentById(id)` | ❌ |
| `getDocumentsByProjectId(projectId)` | ❌ |
| `getDocumentsByTaskId(taskId)` | ❌ |
| `getDocumentsByCategory(category)` | ❌ |
| `getDocumentsByReportId(reportId)` | ❌ |
| `getDocumentsByReportIdAndCategory(...)` | ❌ |
| `getDocumentsByUserId(userId)` | ❌ |
| `getReportDocumentStats(reportId)` | ❌ |
| `getDocumentCountByProjectId(projectId)` | ❌ |

每个缺 userId 的方法补上：

```java
@RequestHeader(value = "X-User-Id", required = false) String userId
```

⚠️ **`getDocumentsByUserId` 的 path variable 与 actor 参数同名 `userId`**——为了避免 controller 局部变量混淆，**重命名 path variable 参数名为 `targetUserId`**：

```java
@GetMapping("/user/{userId}")
public Result<List<Document>> getDocumentsByUserId(
        @PathVariable("userId") String targetUserId,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
    List<Document> documents = documentService.getDocumentsByUserId(userId, targetUserId);
    return Result.success(documents);
}
```

- [ ] **Step 2: 调用 service 时把 userId 传进去**

| 方法 | 调用 |
|------|------|
| `getAllDocuments` | `documentService.getAllDocuments(userId)` |
| `getDocumentById` | `documentService.getDocumentById(userId, id)` |
| `getDocumentsByProjectId` | `documentService.getDocumentsByProjectId(userId, projectId)` |
| `getDocumentsByTaskId` | `documentService.getDocumentsByTaskId(userId, taskId)` |
| `getDocumentsByCategory` | `documentService.getDocumentsByCategory(userId, category)` |
| `getDocumentsByReportId` | `documentService.getDocumentsByReportId(userId, reportId)` |
| `getDocumentsByReportIdAndCategory` | `documentService.getDocumentsByReportIdAndCategory(userId, reportId, category)` |
| `getDocumentsByUserId` | `documentService.getDocumentsByUserId(userId, targetUserId)` |
| `getReportDocumentStats` | 改造：先按 `getDocumentsByReportId(userId, reportId)` 取列表再聚合；service 层加 `getReportDocumentStats(userId, reportId)` 包装 |
| `downloadDocument` | `documentService.downloadDocument(userId, id)` |
| `previewDocument` | `documentService.downloadDocument(userId, id)` (复用) |
| `updateDocument` | `documentService.updateDocument(userId, id, document.getName(), document.getCategory(), document.getProjectId(), document.getTaskId(), document.getParentId(), document.getDescription())` |
| `deleteDocument` | `documentService.deleteDocument(userId, id)` |
| `getDocumentCountByProjectId` | `documentService.getDocumentCountByProjectId(userId, projectId)` |

- [ ] **Step 3: `uploadDocument` 调用调整**

旧的：

```java
Document document = documentService.uploadDocument(file, name, category, projectId, taskId, parentId, reportId, description, userId);
```

改为：

```java
Document document = documentService.uploadDocument(userId, file, name, category, projectId, taskId, parentId, reportId, description);
```

- [ ] **Step 4: `getReportDocumentStats` 单独处理**

由于 `getReportDocumentStats` 返回的是聚合统计（不是列表），按"先按用户范围过滤该 reportId 的文档，再聚合"思路：

`DocumentService.getReportDocumentStats` 新签名：

```java
public Map<String, Object> getReportDocumentStats(String userId, String reportId) {
    // 复用 list 方法(已含权限过滤)
    List<Document> documents = getDocumentsByReportId(userId, reportId);
    Map<String, Integer> categoryCount = new LinkedHashMap<>();
    // ... 原聚合逻辑不变 ...
}
```

Controller 端调用：

```java
@GetMapping("/report/{reportId}/stats")
public Result<Map<String, Object>> getReportDocumentStats(
        @PathVariable String reportId,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
    Map<String, Object> stats = documentService.getReportDocumentStats(userId, reportId);
    return Result.success(stats);
}
```

- [ ] **Step 4: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -20`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/wbs/project/controller/DocumentController.java
git commit -m "feat(doc-permission): DocumentController 透传 X-User-Id"
```

---

### Task 10: 端到端验证（手工 + curl）

**Files:** 仅运行命令，不改代码。

- [ ] **Step 1: 编译最终验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -10`
Expected: `BUILD SUCCESS`

- [ ] **Step 2: 准备测试数据（数据库）**

通过 MySQL MCP 或直接 SQL：

```sql
-- 假设已有 admin (C0000001)、department pm (C0000002)、PM (C0000003)、owner (C0000004)、member (C0000005)、viewer (C0000006)、跨部门上传者 (C0000007) 7 个用户
-- 假设已有 project p001 (owner=C0000004, dept_code=D001), p002 (owner=C0000004, dept_code=D002)
-- C0000007 的 dept_code=D002(跨部门)

-- 文档覆盖:
--   doc1=admin 上传;p001
--   doc2=部门 PM 上传;p001(用于部门 PM 列表)
--   doc3=PM 上传;p001(同项目内)
--   doc4=owner 上传;p001
--   doc5=member 上传;p001
--   doc6=member 上传;p002(跨项目)
--   doc7=跨部门用户(C0000007, dept=D002)上传到 p001(跨部门协作)
--   doc_general=admin 上传;project_id=NULL(测 general 文档)
INSERT INTO sys_document (id, project_id, category, file_name, file_path, file_size, file_type, file_extension, version, uploaded_by, status, created_at, updated_at)
VALUES
  ('doc1',        'p001', 'requirements', 'a.pdf',         '/tmp/a.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000001', 'active', NOW(), NOW()),
  ('doc2',        'p001', 'requirements', 'b.pdf',         '/tmp/b.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000002', 'active', NOW(), NOW()),
  ('doc3',        'p001', 'design',       'c.pdf',         '/tmp/c.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000003', 'active', NOW(), NOW()),
  ('doc4',        'p001', 'design',       'd.pdf',         '/tmp/d.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000004', 'active', NOW(), NOW()),
  ('doc5',        'p001', 'design',       'e.pdf',         '/tmp/e.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000005', 'active', NOW(), NOW()),
  ('doc6',        'p002', 'design',       'f.pdf',         '/tmp/f.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000005', 'active', NOW(), NOW()),
  ('doc7',        'p001', 'design',       'g.pdf',         '/tmp/g.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000007', 'active', NOW(), NOW()),
  ('doc_general', NULL,   'requirements', 'h.pdf',         '/tmp/h.pdf', 100, 'application/pdf', 'pdf', 1, 'C0000001', 'active', NOW(), NOW());
```

确认测试用户角色：

```sql
SELECT id, name, role, dept_code, managed_dept_codes, managed_project_ids FROM sys_user
WHERE id IN ('C0000001','C0000002','C0000003','C0000004','C0000005','C0000006','C0000007');
```

- [ ] **Step 3: 启动后端 + 登录拿 JWT**

⚠️ 按 CLAUDE.md "不要自动启动服务"——让用户启动并提供 JWT。或者：

```bash
# 用户执行:
cd backend && mvn spring-boot:run
# 在另一个终端:
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"C0000001","password":"1"}'
# 拿到 JWT 和 userId/role
```

- [ ] **Step 4: 跑 11 项核心验证（spec section 8 全覆盖）**

```bash
# 1) admin 看到全 8 个文档
curl -H "Authorization: Bearer $JWT_ADMIN" http://localhost:8084/api/documents | jq '.data | length'
# 期望: 8

# 2) 部门 PM (managed_dept_codes=[D001]) 看到上传者 dept_code=D001 的 doc (含 admin 自己 D001、dept-pm D001、PM D001、owner D001、member D001、跨部门用户 D002 不在)
#    → doc1,doc2,doc3,doc4,doc5(全 D001)+ doc_general(uploader admin=D001) = 6
curl -H "Authorization: Bearer $JWT_DEPT_PM" http://localhost:8084/api/documents | jq '.data | length'
# 期望: 6

# 3) PM (managed_project_ids=[p001]) 看到 p001 的项目成员上传的 doc
#    → doc1..doc7 (p001 内)+ doc_general? 不,doc_general project_id=NULL,不在 projectIds 也不在 uploaderIds(D001 内有,但 PM 不按部门查)
#    → 实际: doc1..doc7 = 7
curl -H "Authorization: Bearer $JWT_PM" http://localhost:8084/api/documents | jq '.data | length'
# 期望: 7

# 4) 项目负责人 (owner=p001) 看到 p001 项目成员上传的 doc
#    → doc1..doc7 = 7
curl -H "Authorization: Bearer $JWT_OWNER" http://localhost:8084/api/documents | jq '.data | length'
# 期望: 7

# 5) MEMBER 只看到自己上传的 + 参与项目(p001)的文档
#    → doc5(自己)+ doc1..doc7(p001 内的) = 8(但自己也算在内)
curl -H "Authorization: Bearer $JWT_MEMBER" http://localhost:8084/api/documents | jq '.data | length'
# 期望: 8

# 6) 跨部门协作: 跨部门用户 C0000007(dept=D002)上传了 doc7 到 p001(属 D001)
#    → D001 的 dept-pm 看不到 doc7(因上传者 dept=D002 不在 managed_dept_codes 内)
#    → D002 的 dept-pm 能看到 doc7
curl -H "Authorization: Bearer $JWT_DEPT_PM_D001" http://localhost:8084/api/documents | jq '.data[].id' | grep doc7
# 期望: 无输出
curl -H "Authorization: Bearer $JWT_DEPT_PM_D002" http://localhost:8084/api/documents | jq '.data[].id' | grep doc7
# 期望: "doc7"

# 7) general 文档(项目 ID=null):上传者部门 PM 能看,非部门 PM 不能
#    doc_general 由 C0000001(admin, dept=D001)上传
#    → D001 的 dept-pm 应看到(doc_general 的 uploader admin dept=D001 ∈ managed_dept_codes)
curl -H "Authorization: Bearer $JWT_DEPT_PM_D001" http://localhost:8084/api/documents | jq '.data[].id' | grep doc_general
# 期望: "doc_general"
#    → D002 的 dept-pm 看不到
curl -H "Authorization: Bearer $JWT_DEPT_PM_D002" http://localhost:8084/api/documents | jq '.data[].id' | grep doc_general
# 期望: 无输出

# 8) 越权下载: MEMBER 试下载 doc6(由 member 自己上传但属于 p002,自己非 p002 成员)
#    不,改为 MEMBER 试下载一个不属于自己且不在参与项目中的文档
#    这里换成: 跨部门用户上传的 doc7(p001 内,MEMBER 是 p001 成员,实际上可见)
#    真正测越权: 用非项目成员 MEMBER 试下载 doc_general(非项目文档,只有 admin/上传者部门 PM 可见)
curl -H "Authorization: Bearer $JWT_MEMBER" http://localhost:8084/api/documents/doc_general/download -o /tmp/x -w "%{http_code}\n"
# 期望: 403

# 9) 越权上传: MEMBER 试上传到 p002 (未参与)
curl -X POST -H "Authorization: Bearer $JWT_MEMBER" \
  -F "file=@/tmp/test.pdf" -F "category=design" -F "projectId=p002" \
  http://localhost:8084/api/documents/upload
# 期望: code 403

# 10) VIEWER 上传被拒
curl -X POST -H "Authorization: Bearer $JWT_VIEWER" \
  -F "file=@/tmp/test.pdf" -F "category=design" -F "projectId=p001" \
  http://localhost:8084/api/documents/upload
# 期望: code 403

# 11) /documents/user/{userId} 端点角色白名单: MEMBER 调用应 403
curl -H "Authorization: Bearer $JWT_MEMBER" http://localhost:8084/api/documents/user/C0000001 | jq '.code'
# 期望: 403
curl -H "Authorization: Bearer $JWT_ADMIN" http://localhost:8084/api/documents/user/C0000001 | jq '.data | length'
# 期望: admin 看 C0000001 上传的文档数(可能不止 1,取决于 admin 上传了几个)
```

每条不符合期望 → 回到对应任务修代码。

- [ ] **Step 5: 清理测试数据**

```sql
DELETE FROM sys_document WHERE id IN ('doc1','doc2','doc3','doc4','doc5','doc6','doc7','doc_general');
DELETE FROM sys_document_access_log WHERE document_id IN ('doc1','doc2','doc3','doc4','doc5','doc6','doc7','doc_general');
```

- [ ] **Step 6: 最终 commit（如有运行时修复）**

```bash
git status
# 如果有未提交修改:
git add -A
git commit -m "fix(doc-permission): 验证阶段修复"
```

---

## 任务完成检查清单

- [ ] Task 0: 数据清理（spec 5.3 一次性 SQL,管理员切换前手动执行）
- [ ] Task 1: UserMapper 新方法
- [ ] Task 2: ProjectMemberMapper 新方法
- [ ] Task 3: PermissionService 数据范围方法
- [ ] Task 4: PermissionService 判定方法 + 删除 `canEditDocument`
- [ ] Task 5: DocumentMapper `selectByAccessibleScope`
- [ ] Task 6: DocumentService 列表方法
- [ ] Task 7: DocumentService 单文档方法（detail/download）
- [ ] Task 8: DocumentService 写操作（upload/update/delete）
- [ ] Task 9: DocumentController 透传 userId
- [ ] Task 10: 端到端验证通过

---

### Task 0: 部署前数据清理（spec 5.3）

**Files:** 仅运行 SQL，不改代码。

⚠️ **必须在 Task 1 之前完成**（与代码切换解耦，由 admin 单独窗口执行）。

- [ ] **Step 1: 备份孤儿记录**

Run:

```sql
SELECT * INTO OUTFILE '/tmp/sys_document_orphans_$(date +%F).csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM sys_document
WHERE uploaded_by NOT IN (SELECT id FROM sys_user WHERE status != 'T')
   OR uploaded_by IS NULL
   OR uploaded_by = 'system-default';
```

> 若 MySQL 用户无 `FILE` 权限，改用 `mysqldump --where` 或前端 UI 手工 export CSV。

- [ ] **Step 2: 软删孤儿**

```sql
UPDATE sys_document
SET status = 'deleted', updated_at = NOW()
WHERE uploaded_by NOT IN (SELECT id FROM sys_user WHERE status != 'T')
   OR uploaded_by IS NULL
   OR uploaded_by = 'system-default';
```

> 软删后除非手工 UPDATE 回 `status='active'`，否则无法恢复——这就是 Step 1 备份的必要性。

- [ ] **Step 3: 记录执行日志**

在 ops 文档中记下：执行人、执行时间、孤儿记录数、被软删的 ID 范围，便于追溯。