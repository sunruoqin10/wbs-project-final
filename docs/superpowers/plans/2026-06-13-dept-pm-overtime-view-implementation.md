# 部门项目负责人加班查看/审批 实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让部门项目负责人(role=`dept-project-manager`)能**只读查看 + 审批**所辖部门成员的加班记录,顺带修复 `/stats` 与 `/stats/*` 4 个聚合接口的两处潜在信息泄露。

**Architecture:** 不新建 ACL 表。集中在 `PermissionService` 新增 `getAccessibleOvertimeUserIds`(对齐 `getAccessibleUploaderIds` 模式),`OvertimeMapper` 5 个 SQL 加 `userIds` IN 守卫,`OvertimeService.hasPermission` / `validateApprover` / `getStats` 加 dept-pm 分支,`OvertimeController.getStats` 放行 dept-pm 并删除 `filterStatsByPermission` 兜底方法。前端 0 改动。

**Tech Stack:** Spring Boot 3.2.0 + MyBatis + MySQL + Java 17 + JUnit 5 + Mockito(测试)。MyBatis XML `<foreach>` + `<if>` 动态 SQL。

**Spec:** `docs/superpowers/specs/2026-06-13-dept-pm-overtime-view-design.md`

---

## 文件结构(先看全局)

| 文件 | 类型 | 责任 |
|------|------|------|
| `PermissionService.java` | 改 | 新增 `getAccessibleOvertimeUserIds(String)` |
| `OvertimeMapper.java` + `OvertimeMapper.xml` | 改 | 5 个方法加 `List<String> userIds` 参数 + IN 守卫 |
| `OvertimeService.java` | 改 | `hasPermission` / `validateApprover` / `getStats` / 4 个聚合方法签名 + 内部 SQL 注入 userIds |
| `OvertimeController.java` | 改 | `getStats` 放行 dept-pm + 删除 `filterStatsByPermission`;4 个 `/stats/*` 入口透传 userIds |
| `OvertimeServiceHasPermissionTest.java` (新) | 创 | Mockito 单测,覆盖 `hasPermission` dept-pm 路径 |
| `OvertimeServiceValidateApproverTest.java` (新) | 创 | Mockito 单测,覆盖 `validateApprover` dept-pm 路径 |
| `OvertimeServiceGetStatsTest.java` (新) | 创 | Mockito 单测,覆盖 `getStats` SQL userIds 注入 |
| `PermissionServiceGetAccessibleOvertimeUserIdsTest.java` (新) | 创 | Mockito 单测,覆盖 `getAccessibleOvertimeUserIds` 6 种分支 |

---

## 验证前置条件

- 后端 MySQL 在 `localhost:3306` 可用(`db_webwbs` 数据库存在)。
- 编译验证:`cd backend && mvn -pl . test -DfailIfNoTests=false` (项目原无 Java 测试,加 `-DfailIfNoTests=false` 避免空测试目录报错;**实施本计划后会引入测试,届时可去掉该 flag**)。
- 前端无变更,无需 `vue-tsc`。
- JDK 17, Maven 3.8+。

---

## 强顺序约束

| 步骤 | 依赖 |
|------|------|
| Task 5(`OvertimeService.getStats` SQL 过滤下沉) | 必须在 Task 7(`OvertimeController` 删除 `filterStatsByPermission`) 之前完成 |
| Task 6(`OvertimeService` 4 个聚合方法加 userIds) | 必须在 Task 7 (Controller 透传 userIds) 之前完成 |
| Task 3 / Task 4 | 互相独立,可与 Task 5 / Task 6 并行 |

> 原因:Task 7 会删除 `OvertimeController.filterStatsByPermission` 兜底方法。如果 Task 5/6 未先完成 SQL 注入,dept-pm 会在过渡期看到全公司聚合数据。

---

### Task 1: PermissionService 新增 `getAccessibleOvertimeUserIds`

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/PermissionService.java`

- [ ] **Step 1: 在 PermissionService 中添加方法**

打开 `backend/src/main/java/com/wbs/project/service/PermissionService.java`,在 §`// ============ 文档权限单文档判定(2026-06-13) ============` 注释块之前、`canViewDocument` 之后的位置,添加:

```java
// ============ 加班权限数据范围(2026-06-13) ============

/**
 * 返回当前用户能查看的「加班记录提交者」userId 集合。
 * 与 getAccessibleUploaderIds 同形(对齐 dept-pm 维度),但语义是"可见的加班来源"。
 *
 * 规则(2026-06-13):
 *  - admin / project-manager 返回 null(语义:不限)
 *  - dept-project-manager 返回所辖部门内的在职用户 ID 集合
 *  - 其他角色(MEMBER / VIEWER)返回仅自己
 *  - 项目负责人(owner 但非 admin/PM/dept-pm)走兜底返回仅自己
 *    (owner 看自己 owner 项目的成员加班,目前不在此方法覆盖范围,由 hasPermission 单独处理)
 *
 * 业务方应这样用:对 null 跳过 IN 守卫,非空 IN (...) 守卫,空集合 → 直接返回空结果。
 */
public Set<String> getAccessibleOvertimeUserIds(String userId) {
    if (userId == null) {
        return Collections.emptySet();
    }
    if (isAdmin(userId) || isProjectManager(userId)) {
        return null; // null = 不限
    }
    if (isDeptProjectManager(userId)) {
        User u = userMapper.selectById(userId);
        if (u != null) {
            List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
            if (!codes.isEmpty()) {
                return new HashSet<>(userMapper.selectIdsByDeptCodes(codes));
            }
        }
        return Collections.emptySet();
    }
    // MEMBER / VIEWER / owner: 仅自己
    return Set.of(userId);
}
```

- [ ] **Step 2: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/PermissionService.java
git commit -m "feat(overtime): PermissionService 新增 getAccessibleOvertimeUserIds"
```

---

### Task 2: OvertimeMapper 加 `userIds` IN 守卫(5 个 SQL)

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/mapper/OvertimeMapper.java`
- Modify: `backend/src/main/resources/mapper/OvertimeMapper.xml`

- [ ] **Step 1: 改 OvertimeMapper.java 接口签名**

打开 `backend/src/main/java/com/wbs/project/mapper/OvertimeMapper.java`,对以下 5 个方法在末尾加 `List<String> userIds` 参数:

- `selectByCondition`
- `sumHoursGroupByUser`
- `sumHoursGroupByProject`
- `sumHoursGroupByDate`
- `sumHoursGroupByType`

修改后完整签名:

```java
List<OvertimeRecord> selectByCondition(@Param("userId") String userId,
                                       @Param("projectId") String projectId,
                                       @Param("status") String status,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("overtimeType") String overtimeType,
                                       @Param("userIds") List<String> userIds);

List<Map<String, Object>> sumHoursGroupByUser(@Param("projectId") String projectId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("userIds") List<String> userIds);

List<Map<String, Object>> sumHoursGroupByProject(@Param("userId") String userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("userIds") List<String> userIds);

List<Map<String, Object>> sumHoursGroupByDate(@Param("userId") String userId,
                                               @Param("projectId") String projectId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("userIds") List<String> userIds);

List<Map<String, Object>> sumHoursGroupByType(@Param("userId") String userId,
                                               @Param("projectId") String projectId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("userIds") List<String> userIds);
```

需要在 import 区域加 `import java.util.List;`(若已存在则跳过)。

- [ ] **Step 2: 改 OvertimeMapper.xml 给 5 个 SQL 加 IN 守卫**

打开 `backend/src/main/resources/mapper/OvertimeMapper.xml`,对 5 个 `<select>` 的 `<where>` 或末尾添加守卫(在 GROUP BY 之前):

**2.1 `selectByCondition` (L143-171)**:在 `<if test="overtimeType ...">` 之后、`</where>` 之前添加:

```xml
<if test="userIds != null and userIds.size() > 0">
    AND o.user_id IN
    <foreach collection="userIds" item="uid" open="(" separator="," close=")">
        #{uid}
    </foreach>
</if>
```

**2.2 `sumHoursGroupByUser` (L246-262)**:在 `endDate` `<if>` 之后、`GROUP BY` 之前添加相同 IN 守卫。

**2.3 `sumHoursGroupByProject` (L264-280)**:同上位置加 IN 守卫。

**2.4 `sumHoursGroupByDate` (L282-300)**:同上位置加 IN 守卫。

**2.5 `sumHoursGroupByType` (L302-320)**:同上位置加 IN 守卫。

> 守卫语法对 5 处**完全一致**:
> ```xml
> <if test="userIds != null and userIds.size() > 0">
>     AND o.user_id IN
>     <foreach collection="userIds" item="uid" open="(" separator="," close=")">
>         #{uid}
>     </foreach>
> </if>
> ```

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`(若失败,检查 MyBatis 找不到 `userIds` 参数;常见原因是 mapper XML `<if>` 守卫拼写错或 Java 接口缺 `@Param` 注解。)

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/wbs/project/mapper/OvertimeMapper.java \
        backend/src/main/resources/mapper/OvertimeMapper.xml
git commit -m "feat(overtime): OvertimeMapper 5 个 SQL 加 userIds IN 守卫"
```

---

### Task 3: OvertimeService.hasPermission 加 dept-pm 分支

**Files:**
- Create: `backend/src/test/java/com/wbs/project/service/OvertimeServiceHasPermissionTest.java`
- Modify: `backend/src/main/java/com/wbs/project/service/OvertimeService.java`

- [ ] **Step 1: 写失败的单测**

创建 `backend/src/test/java/com/wbs/project/service/OvertimeServiceHasPermissionTest.java`:

```java
package com.wbs.project.service;

import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.hasPermission dept-pm 路径单测(2026-06-13)
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceHasPermissionTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    private User deptPm(String id) {
        User u = new User();
        u.setId(id);
        u.setRole("dept-project-manager");
        return u;
    }

    private User member(String id, String deptCode) {
        User u = new User();
        u.setId(id);
        u.setRole("member");
        u.setDeptCode(deptCode);
        return u;
    }

    @Test
    void deptPm_canViewRecordOfManagedDeptMember() {
        // dept-pm C0000001 manages ["D001"]
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);
        // record owner C0000002 dept = D001
        when(userMapper.selectById("C0000002")).thenReturn(member("C0000002", "D001"));
        when(permissionService.isDeptManager("C0000001", "D001")).thenReturn(true);

        assertTrue(overtimeService.hasPermission("C0000001", "P001", "C0000002"));
    }

    @Test
    void deptPm_cannotViewRecordOfOtherDeptMember() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);
        when(userMapper.selectById("C0000003")).thenReturn(member("C0000003", "D999"));
        when(permissionService.isDeptManager("C0000001", "D999")).thenReturn(false);

        // 既不是 owner 也不是 dept 内,落到 project.ownerId 检查
        Project p = new Project();
        p.setId("P001");
        p.setOwnerId("C0000099");
        when(projectMapper.selectById("P001")).thenReturn(p);

        assertFalse(overtimeService.hasPermission("C0000001", "P001", "C0000003"));
    }

    @Test
    void deptPm_canAlwaysViewOwnRecord() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);

        assertTrue(overtimeService.hasPermission("C0000001", "P001", "C0000001"));
    }

    @Test
    void deptPm_recordUserIdNull_fallsThroughToOwnerCheck() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);

        Project p = new Project();
        p.setId("P001");
        p.setOwnerId("C0000001"); // actor 是 owner
        when(projectMapper.selectById("P001")).thenReturn(p);

        // recordUserId 为 null:不应触发 dept-pm 分支,走 owner 路径放行
        assertTrue(overtimeService.hasPermission("C0000001", "P001", null));
    }
}
```

- [ ] **Step 2: 跑测试,确认失败(因 hasPermission 还没 dept-pm 分支)**

Run: `cd backend && mvn test -Dtest=OvertimeServiceHasPermissionTest 2>&1 | tail -50`
Expected: `Tests run: 4, Failures: 2, Errors: 0`(第 1、2 个 case 会因 dept-pm 分支缺失而 fail)

- [ ] **Step 3: 在 OvertimeService.hasPermission 加 dept-pm 分支**

打开 `backend/src/main/java/com/wbs/project/service/OvertimeService.java`,找到 `hasPermission(String userId, String projectId, String recordUserId)` 方法(L58-87),在 `// 用户可以访问自己提交的加班记录` 这段之后、`// 检查用户是否是项目的负责人` 这段之前,加入:

```java
        // 部门项目负责人(2026-06-13): 提交者 dept 在 managed_dept_codes 内则放行
        // 注意: recordUserId 必须非空(避免只看 projectId 误放行),且复用 permissionService.isDeptManager
        // 性能 trade-off: 列表场景下每次调用会多查一次 user(拿 deptCode); 当前 N+1 接受,见 spec §4.1
        if (recordUserId != null) {
            User recordUser = userMapper.selectById(recordUserId);
            if (recordUser != null && recordUser.getDeptCode() != null
                    && permissionService.isDeptManager(userId, recordUser.getDeptCode())) {
                return true;
            }
        }
```

- [ ] **Step 4: 跑测试,确认全部通过**

Run: `cd backend && mvn test -Dtest=OvertimeServiceHasPermissionTest 2>&1 | tail -30`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: 全量编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/OvertimeService.java \
        backend/src/test/java/com/wbs/project/service/OvertimeServiceHasPermissionTest.java
git commit -m "feat(overtime): hasPermission 加 dept-pm 分支(dept 内成员可查)"
```

---

### Task 4: OvertimeService.validateApprover 加 dept-pm 分支

**Files:**
- Create: `backend/src/test/java/com/wbs/project/service/OvertimeServiceValidateApproverTest.java`
- Modify: `backend/src/main/java/com/wbs/project/service/OvertimeService.java`

- [ ] **Step 1: 写失败的单测**

创建 `backend/src/test/java/com/wbs/project/service/OvertimeServiceValidateApproverTest.java`:

```java
package com.wbs.project.service;

import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.validateApprover dept-pm 路径单测(2026-06-13)
 * validateApprover 是 private,通过反射调用
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceValidateApproverTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    private void invokeValidateApprover(String approverId, String projectId) throws Exception {
        Method m = OvertimeService.class.getDeclaredMethod("validateApprover", String.class, String.class);
        m.setAccessible(true);
        m.invoke(overtimeService, approverId, projectId);
    }

    @Test
    void deptPm_canApproveRecordInManagedDeptProject() throws Exception {
        User approver = new User();
        approver.setId("C0000001");
        approver.setRole("dept-project-manager");
        when(userMapper.selectById("C0000001")).thenReturn(approver);
        when(permissionService.isDeptProjectManager("C0000001")).thenReturn(true);

        Project p = new Project();
        p.setId("P001");
        p.setDeptCode("D001");
        when(projectMapper.selectById("P001")).thenReturn(p);
        when(permissionService.isDeptManager("C0000001", "D001")).thenReturn(true);

        // 不应抛
        invokeValidateApprover("C0000001", "P001");
    }

    @Test
    void deptPm_cannotApproveRecordInOtherDeptProject() throws Exception {
        User approver = new User();
        approver.setId("C0000001");
        approver.setRole("dept-project-manager");
        when(userMapper.selectById("C0000001")).thenReturn(approver);
        when(permissionService.isDeptProjectManager("C0000001")).thenReturn(true);

        Project p = new Project();
        p.setId("P001");
        p.setDeptCode("D999");
        when(projectMapper.selectById("P001")).thenReturn(p);
        when(permissionService.isDeptManager("C0000001", "D999")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invokeValidateApprover("C0000001", "P001"));
        assertTrue(ex.getMessage().contains("您没有权限审批"));
    }
}
```

- [ ] **Step 2: 跑测试,确认失败**

Run: `cd backend && mvn test -Dtest=OvertimeServiceValidateApproverTest 2>&1 | tail -50`
Expected: `Tests run: 2, Failures: 0, Errors: 2` (case 1 期望不抛但目前抛,case 2 期望抛也抛 — 实际原因是 validateApprover 当前不识别 dept-pm 角色,都走"项目 owner != 自己"的失败路径,case 1 也会抛 → 2 个 case 都 fail)

- [ ] **Step 3: 在 OvertimeService.validateApprover 加 dept-pm 分支**

打开 `backend/src/main/java/com/wbs/project/service/OvertimeService.java`,找到 `validateApprover(String approverId, String projectId)` 方法(L314-337),在 `// 项目负责人可以审批其负责项目的加班申请` 这段 if 之后、`throw` 之前,加入:

```java
        // 部门项目负责人(2026-06-13): 项目归属部门在 managed_dept_codes 内则可审批
        // 判定口径: 按 project.deptCode(而非 recordUser.deptCode),避免提交者换部门后授权漂移
        if (permissionService.isDeptProjectManager(approverId)
                && project.getDeptCode() != null
                && permissionService.isDeptManager(approverId, project.getDeptCode())) {
            return;
        }
```

- [ ] **Step 4: 跑测试,确认全部通过**

Run: `cd backend && mvn test -Dtest=OvertimeServiceValidateApproverTest 2>&1 | tail -30`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: 全量编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/OvertimeService.java \
        backend/src/test/java/com/wbs/project/service/OvertimeServiceValidateApproverTest.java
git commit -m "feat(overtime): validateApprover 加 dept-pm 分支(按项目归属部门判定)"
```

---

### Task 5: OvertimeService.getStats 下沉 SQL userIds 过滤 ⚠️ 硬约束

**Files:**
- Create: `backend/src/test/java/com/wbs/project/service/OvertimeServiceGetStatsTest.java`
- Modify: `backend/src/main/java/com/wbs/project/service/OvertimeService.java`

> ⚠️ **本任务必须先于 Task 7 完成**(否则 Task 7 删除 `filterStatsByPermission` 后,dept-pm 调 `/stats` 会看到全公司数据)。

- [ ] **Step 1: 写失败的单测**

创建 `backend/src/test/java/com/wbs/project/service/OvertimeServiceGetStatsTest.java`:

```java
package com.wbs.project.service;

import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.getStats dept-pm SQL 过滤下沉 单测(2026-06-13)
 * 验证: dept-pm 调用 getStats 时,selectByCondition 收到的 userIds 不为 null
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceGetStatsTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    @Test
    void deptPm_getStats_passesUserIdsToMapper() {
        User actor = new User();
        actor.setId("C0000001");
        actor.setRole("dept-project-manager");
        when(userMapper.selectById("C0000001")).thenReturn(actor);
        when(permissionService.getAccessibleOvertimeUserIds("C0000001"))
                .thenReturn(java.util.Set.of("C0000002", "C0000003"));
        when(overtimeMapper.selectByCondition(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        overtimeService.getStats(null, null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> userIdsCap = ArgumentCaptor.forClass(List.class);
        verify(overtimeMapper).selectByCondition(
                any(), any(), any(), any(), any(), any(), userIdsCap.capture());
        assertEquals(List.of("C0000002", "C0000003"), userIdsCap.getValue());
    }

    @Test
    void admin_getStats_passesNullUserIdsToMapper() {
        User actor = new User();
        actor.setId("C0000099");
        actor.setRole("admin");
        when(userMapper.selectById("C0000099")).thenReturn(actor);
        when(permissionService.getAccessibleOvertimeUserIds("C0000099")).thenReturn(null);
        when(overtimeMapper.selectByCondition(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        overtimeService.getStats(null, null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> userIdsCap = ArgumentCaptor.forClass(List.class);
        verify(overtimeMapper).selectByCondition(
                any(), any(), any(), any(), any(), any(), userIdsCap.capture());
        assertNull(userIdsCap.getValue());
    }
}
```

> **重要**: 上面两个测试的 `overtimeService.getStats(...)` 调用是**新签名**,需要包含 currentUserId 参数。Step 3 会改 OvertimeService 签名。在测试文件中,先按新签名调用(测试编译会因 OvertimeService 旧签名失败,这正是 TDD 期望的)。

- [ ] **Step 2: 跑测试,确认失败(签名不匹配)**

Run: `cd backend && mvn test -Dtest=OvertimeServiceGetStatsTest 2>&1 | tail -50`
Expected: 编译失败或 case fail(因为 OvertimeService.getStats 旧签名是 4 参,新签名是 5 参)

- [ ] **Step 3: 改 OvertimeService.getStats 签名 + 下沉 SQL 过滤**

打开 `backend/src/main/java/com/wbs/project/service/OvertimeService.java`,修改 `getStats` 方法(L344-417),**新签名 + 内部 SQL 过滤**:

新签名:

```java
public OvertimeDTO.OvertimeStats getStats(String userId, String projectId,
                                          LocalDate startDate, LocalDate endDate,
                                          String currentUserId) {
    // 解析当前用户的可访问 userId 集合
    List<String> accessibleUserIds = null; // null = 不限
    if (currentUserId != null) {
        java.util.Set<String> set = permissionService.getAccessibleOvertimeUserIds(currentUserId);
        if (set == null) {
            accessibleUserIds = null; // admin/PM: 不限
        } else if (set.isEmpty()) {
            // 当前用户无任何可访问加班源,直接返回空 stats
            OvertimeDTO.OvertimeStats empty = new OvertimeDTO.OvertimeStats();
            empty.setTotalRecords(0);
            empty.setTotalHours(BigDecimal.ZERO);
            empty.setTotalPeople(0);
            empty.setPendingApprovals(0);
            empty.setThisMonthHours(BigDecimal.ZERO);
            empty.setThisMonthPeople(0);
            empty.setByType(new OvertimeDTO.ByTypeStats());
            empty.setByProject(List.of());
            return empty;
        } else {
            accessibleUserIds = new java.util.ArrayList<>(set);
            // 调用方 userId 与 accessibleUserIds 做 AND 关系
            if (userId != null && !accessibleUserIds.contains(userId)) {
                // 调用方限定到具体人,但该人不属于当前用户可访问范围 → 空 stats
                OvertimeDTO.OvertimeStats empty = new OvertimeDTO.OvertimeStats();
                empty.setTotalRecords(0);
                empty.setTotalHours(BigDecimal.ZERO);
                empty.setTotalPeople(0);
                empty.setPendingApprovals(0);
                empty.setThisMonthHours(BigDecimal.ZERO);
                empty.setThisMonthPeople(0);
                empty.setByType(new OvertimeDTO.ByTypeStats());
                empty.setByProject(List.of());
                return empty;
            }
        }
    }

    List<OvertimeRecord> records = overtimeMapper.selectByCondition(
            userId, projectId, null, startDate, endDate, null, accessibleUserIds);

    // ... 后续聚合逻辑与现状完全一致(以下省略,与 L347-417 一致)
    OvertimeDTO.OvertimeStats stats = new OvertimeDTO.OvertimeStats();
    stats.setTotalRecords(records.size());
    stats.setPendingRecords((int) records.stream().filter(r -> "pending".equals(r.getStatus())).count());
    stats.setApprovedRecords((int) records.stream().filter(r -> "approved".equals(r.getStatus())).count());
    stats.setRejectedRecords((int) records.stream().filter(r -> "rejected".equals(r.getStatus())).count());
    stats.setTotalHours(records.stream().filter(r -> "approved".equals(r.getStatus()))
            .map(OvertimeRecord::getHours).reduce(BigDecimal.ZERO, BigDecimal::add));
    stats.setPendingHours(records.stream().filter(r -> "pending".equals(r.getStatus()))
            .map(OvertimeRecord::getHours).reduce(BigDecimal.ZERO, BigDecimal::add));
    stats.setApprovedHours(records.stream().filter(r -> "approved".equals(r.getStatus()))
            .map(OvertimeRecord::getHours).reduce(BigDecimal.ZERO, BigDecimal::add));
    stats.setTotalPeople((int) records.stream().filter(r -> "approved".equals(r.getStatus()))
            .map(OvertimeRecord::getUserId).distinct().count());
    stats.setPendingApprovals((int) records.stream().filter(r -> "pending".equals(r.getStatus())).count());
    LocalDate now = LocalDate.now();
    LocalDate monthStart = now.withDayOfMonth(1);
    List<OvertimeRecord> monthRecords = records.stream().filter(r -> {
        LocalDate d = r.getOvertimeDate();
        return d != null && !d.isBefore(monthStart) && !d.isAfter(now);
    }).collect(Collectors.toList());
    stats.setThisMonthHours(monthRecords.stream().filter(r -> "approved".equals(r.getStatus()))
            .map(OvertimeRecord::getHours).reduce(BigDecimal.ZERO, BigDecimal::add));
    stats.setThisMonthPeople((int) monthRecords.stream().filter(r -> "approved".equals(r.getStatus()))
            .map(OvertimeRecord::getUserId).distinct().count());
    OvertimeDTO.ByTypeStats byType = new OvertimeDTO.ByTypeStats();
    byType.setWeekday((int) records.stream().filter(r -> "approved".equals(r.getStatus()))
            .filter(r -> "weekday".equals(r.getOvertimeType())).count());
    byType.setWeekend((int) records.stream().filter(r -> "approved".equals(r.getStatus()))
            .filter(r -> "weekend".equals(r.getOvertimeType())).count());
    byType.setHoliday((int) records.stream().filter(r -> "approved".equals(r.getStatus()))
            .filter(r -> "holiday".equals(r.getOvertimeType())).count());
    stats.setByType(byType);
    List<OvertimeDTO.ProjectOvertimeStats> projectStats = getProjectStats(
            userId, startDate, endDate, accessibleUserIds);
    stats.setByProject(projectStats);
    return stats;
}
```

注意:`getProjectStats` 在本任务里**先保持原签名**(4 参),让它内部继续调不带 userIds 的 SQL(后续 Task 6 会改造)。`accessibleUserIds` 这里先**不传**给 getProjectStats,避免循环依赖 — Task 6 会统一改 4 个聚合方法。

> **简化**: 为让 Task 5 编译通过,可以临时让 `getProjectStats` 接受**可选的** userIds 参数(7 参 = 4 原参 + 3 新 userIds)。但更好做法是:把 `getProjectStats` 的新签名改在 Task 6。**推荐**: Task 5 在 getStats 里调 `getProjectStats(userId, startDate, endDate)`,先不传 userIds,Task 6 再统一改。

- [ ] **Step 4: 跑测试,确认全部通过**

Run: `cd backend && mvn test -Dtest=OvertimeServiceGetStatsTest 2>&1 | tail -30`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: 全量编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/OvertimeService.java \
        backend/src/test/java/com/wbs/project/service/OvertimeServiceGetStatsTest.java
git commit -m "feat(overtime): getStats 入口下沉 SQL userIds 过滤(dept-pm / member 收紧)"
```

---

### Task 6: OvertimeService 4 个聚合方法加 userIds 参数 + Controller 透传

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/OvertimeService.java`
- Modify: `backend/src/main/java/com/wbs/project/controller/OvertimeController.java`

- [ ] **Step 1: 改 OvertimeService 4 个聚合方法签名**

打开 `backend/src/main/java/com/wbs/project/service/OvertimeService.java`,修改以下 4 个方法签名(末尾加 `List<String> userIds`):

```java
public List<OvertimeDTO.UserOvertimeStats> getUserStats(String projectId,
        LocalDate startDate, LocalDate endDate, List<String> userIds) {
    List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByUser(
            projectId, startDate, endDate, userIds);
    // ... 内部转换逻辑不变
}

public List<OvertimeDTO.ProjectOvertimeStats> getProjectStats(String userId,
        LocalDate startDate, LocalDate endDate, List<String> userIds) {
    List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByProject(
            userId, startDate, endDate, userIds);
    // ... 内部转换逻辑不变
}

public List<OvertimeDTO.DateOvertimeStats> getDateStats(String userId, String projectId,
        LocalDate startDate, LocalDate endDate, List<String> userIds) {
    List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByDate(
            userId, projectId, startDate, endDate, userIds);
    // ... 内部转换逻辑不变
}

public List<OvertimeDTO.TypeOvertimeStats> getTypeStats(String userId, String projectId,
        LocalDate startDate, LocalDate endDate, List<String> userIds) {
    List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByType(
            userId, projectId, startDate, endDate, userIds);
    // ... 内部转换逻辑不变
}
```

- [ ] **Step 2: 更新 OvertimeService.getStats 内部对 getProjectStats 的调用**

在 `getStats` 方法末尾(当前调 `getProjectStats(userId, startDate, endDate)`),改为:

```java
List<OvertimeDTO.ProjectOvertimeStats> projectStats = getProjectStats(
        userId, startDate, endDate, accessibleUserIds);
```

- [ ] **Step 3: 改 OvertimeController 4 个 `/stats/*` 入口签名**

打开 `backend/src/main/java/com/wbs/project/controller/OvertimeController.java`,在 4 个 controller 方法里:

- `getUserStats` (L256-263)
- `getProjectStats` (L269-276)
- `getDateStats` (L282-290)
- `getTypeStats` (L296-304)

每个方法:
1. 接受 `HttpServletRequest request` 参数
2. 拿到 `currentUserId = getCurrentUserId(request)`
3. 计算 `List<String> userIds = resolveAccessibleOvertimeUserIds(currentUserId)` (调用新加的 helper,见 Step 4)
4. 把 `userIds` 传给对应的 Service 方法

**示例**(`getUserStats` 改后):

```java
@GetMapping("/stats/users")
public Result<List<OvertimeDTO.UserOvertimeStats>> getUserStats(
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletRequest request) {
    List<String> userIds = resolveAccessibleOvertimeUserIds(getCurrentUserId(request));
    List<OvertimeDTO.UserOvertimeStats> stats = overtimeService.getUserStats(
            projectId, startDate, endDate, userIds);
    return Result.success(stats);
}
```

其他 3 个同理。

- [ ] **Step 4: 加 helper 方法 `resolveAccessibleOvertimeUserIds`**

在 `OvertimeController` 已有 `getCurrentUserId` 附近,加:

```java
/**
 * 解析当前用户可访问的加班提交者 userId 集合(2026-06-13)
 * - admin / project-manager 返回 null(不限)
 * - dept-pm 返回所辖部门内用户 ID 集合
 * - 其他返回 List.of(currentUserId)
 */
private List<String> resolveAccessibleOvertimeUserIds(String currentUserId) {
    if (currentUserId == null) return List.of();
    java.util.Set<String> set = permissionService.getAccessibleOvertimeUserIds(currentUserId);
    if (set == null) return null;
    return new java.util.ArrayList<>(set);
}
```

需要:
- 在 `OvertimeController` 添加 `private final PermissionService permissionService;` 字段(配合 `@RequiredArgsConstructor`)
- 顶部加 `import com.wbs.project.service.PermissionService;`

- [ ] **Step 5: 全量编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`(若失败,检查 `getStats` / `getUserStats` / `getProjectStats` / `getDateStats` / `getTypeStats` 调用点的参数数量)

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/wbs/project/service/OvertimeService.java \
        backend/src/main/java/com/wbs/project/controller/OvertimeController.java
git commit -m "feat(overtime): 4 个聚合 /stats/* 入口透传 userIds 过滤"
```

---

### Task 7: OvertimeController.getStats 放行 dept-pm + 删除 filterStatsByPermission ⚠️ 必须在 Task 5/6 之后

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/controller/OvertimeController.java`

> ⚠️ **本任务必须等 Task 5 + Task 6 完成后再执行**。否则过渡期 dept-pm 调 `/stats` 会因为删了 `filterStatsByPermission` 但 SQL 还没注入 userIds,看到全公司数据。

- [ ] **Step 1: 修改 `getStats` 方法(L221-250)**

打开 `backend/src/main/java/com/wbs/project/controller/OvertimeController.java`,找到 `getStats(...)` 方法,**重写**为:

```java
@GetMapping("/stats")
public Result<OvertimeDTO.OvertimeStats> getStats(
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletRequest request) {
    String currentUserId = getCurrentUserId(request);

    // 部门项目负责人 / 项目经理 / 管理员 以外的用户,不能查别人统计
    if (userId != null && !userId.equals(currentUserId)) {
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null
                || (!"admin".equals(currentUser.getRole())
                    && !"project-manager".equals(currentUser.getRole())
                    && !"dept-project-manager".equals(currentUser.getRole()))) {
            return Result.error("您没有权限查看该用户的统计");
        }
    }

    OvertimeDTO.OvertimeStats stats = overtimeService.getStats(
            userId, projectId, startDate, endDate, currentUserId);
    return Result.success(stats);
}
```

> 关键改动:
> 1. L236 allowlist 加 `"dept-project-manager"`
> 2. **删除 L241-247 整个 if 块**(因 filterStatsByPermission 方法被删除)
> 3. 改调 `overtimeService.getStats(userId, projectId, startDate, endDate, currentUserId)` —— 5 参新签名(传 currentUserId 给 Service)

- [ ] **Step 2: 删除 `filterStatsByPermission` 方法**

在 `OvertimeController.java` 中,**整段删除** `filterStatsByPermission` 方法(L43-73)。同时检查 `private final ProjectMapper projectMapper;` 字段(若 Task 6 之后没有其他引用)可一并删除。

- [ ] **Step 3: 全量编译验证**

Run: `cd backend && mvn -pl . test -DfailIfNoTests=false 2>&1 | tail -30`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: 手动端到端验证(用 curl,需要 user 启动后端)**

> 启动后端(用户操作): `cd backend && mvn spring-boot:run`

跑以下 curl 序列,验证 dept-pm 视图正确:

1. 登录一个 dept-pm 账号,拿 JWT
2. `GET /api/overtime?status=approved` 应当返回所辖部门成员的记录(不是空)
3. `GET /api/overtime/stats` 应当返回 200(不是 403),且 byUser 仅含所辖成员
4. `GET /api/overtime/stats/users` 应当仅返回所辖成员(不是全公司)
5. 拿一个所辖成员的加班记录 ID,`PUT /api/overtime/{id}/approve` 应当返回 200
6. 拿一个**非所辖**成员的加班记录 ID,`PUT /api/overtime/{id}/approve` 应当返回 500 + "您没有权限审批此加班申请"

> 用户完成验证后,告诉开发者停掉后端(本计划不自动启停服务)。

- [ ] **Step 5: 全量测试(已无 failIfNoTests)**

Run: `cd backend && mvn test 2>&1 | tail -30`
Expected: `Tests run: X, Failures: 0, Errors: 0`(X = 8,即 4 + 2 + 2)

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/wbs/project/controller/OvertimeController.java
git commit -m "feat(overtime): getStats 放行 dept-pm + 删除 filterStatsByPermission 兜底"
```

---

### Task 8: PermissionService.getAccessibleOvertimeUserIds 单测

**Files:**
- Create: `backend/src/test/java/com/wbs/project/service/PermissionServiceGetAccessibleOvertimeUserIdsTest.java`

> 验证 §4.3 的 5 种角色分支。

- [ ] **Step 1: 写单测**

创建 `backend/src/test/java/com/wbs/project/service/PermissionServiceGetAccessibleOvertimeUserIdsTest.java`:

```java
package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.PermissionMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PermissionService.getAccessibleOvertimeUserIds 单测(2026-06-13)
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceGetAccessibleOvertimeUserIdsTest {

    @Mock PermissionMapper permissionMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock ProjectMapper projectMapper;
    @Mock UserMapper userMapper;
    @Mock TaskMapper taskMapper;

    @InjectMocks PermissionService permissionService;

    private User user(String id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    @Test
    void admin_returnsNull() {
        when(userMapper.selectById("A1")).thenReturn(user("A1", "admin"));
        assertNull(permissionService.getAccessibleOvertimeUserIds("A1"));
    }

    @Test
    void projectManager_returnsNull() {
        when(userMapper.selectById("PM1")).thenReturn(user("PM1", "project-manager"));
        assertNull(permissionService.getAccessibleOvertimeUserIds("PM1"));
    }

    @Test
    void deptPm_returnsDeptMembers() {
        User u = user("D1", "dept-project-manager");
        u.setManagedDeptCodes("[\"D001\",\"D002\"]");
        when(userMapper.selectById("D1")).thenReturn(u);
        when(userMapper.selectIdsByDeptCodes(List.of("D001", "D002")))
                .thenReturn(List.of("U1", "U2", "U3"));

        Set<String> result = permissionService.getAccessibleOvertimeUserIds("D1");
        assertNotNull(result);
        assertEquals(Set.of("U1", "U2", "U3"), result);
    }

    @Test
    void deptPm_noManagedDepts_returnsEmpty() {
        User u = user("D1", "dept-project-manager");
        u.setManagedDeptCodes(null);
        when(userMapper.selectById("D1")).thenReturn(u);

        assertEquals(Set.of(), permissionService.getAccessibleOvertimeUserIds("D1"));
    }

    @Test
    void member_returnsSelf() {
        when(userMapper.selectById("M1")).thenReturn(user("M1", "member"));
        assertEquals(Set.of("M1"), permissionService.getAccessibleOvertimeUserIds("M1"));
    }

    @Test
    void viewer_returnsSelf() {
        when(userMapper.selectById("V1")).thenReturn(user("V1", "viewer"));
        assertEquals(Set.of("V1"), permissionService.getAccessibleOvertimeUserIds("V1"));
    }
}
```

- [ ] **Step 2: 跑测试,确认全部通过**

Run: `cd backend && mvn test -Dtest=PermissionServiceGetAccessibleOvertimeUserIdsTest 2>&1 | tail -30`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 3: 全量编译 + 测试**

Run: `cd backend && mvn test 2>&1 | tail -30`
Expected: `Tests run: 14, Failures: 0, Errors: 0`(6 + 4 + 2 + 2)

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/wbs/project/service/PermissionServiceGetAccessibleOvertimeUserIdsTest.java
git commit -m "test(overtime): PermissionService.getAccessibleOvertimeUserIds 单测 6 例"
```

---

## 完成检查清单

- [ ] 所有 Task 1-8 已 commit
- [ ] `cd backend && mvn test` 全绿(14 用例,0 失败)
- [ ] 用户手动端到端验证(curl)通过(dept-pm 可见 / 不可见 / 可审批 / 不可审批)
- [ ] release notes 已写明 2 个信息泄露修复(§7 顺手修复表)
- [ ] 未自动启停任何服务(遵守 AGENTS.md Service Startup 规则)
- [ ] 未主动 commit / push(等待用户确认后由用户执行)

---

## 已知风险 & 缓解(执行期间留意)

| 风险 | 应对 |
|------|------|
| Task 5/6 之前不能 commit Task 7(否则过渡期会全公司泄露) | Task 7 标题已标注 ⚠️,执行者必须按顺序 |
| `getStats` 新签名在 Task 5 已加,Task 7 才会传 currentUserId;中间 commit 状态下编译会断(因 OvertimeController 旧调 4 参) | 不需要担心 —— commit 应该是完整 feature 一次性提交,不要分阶段 commit |
| MyBatis `<if>` 守卫拼写错误 | Step 3 编译后 grep 5 处 `<if test="userIds...">` 确认 |
| dept-pm 用户没有 `managed_dept_codes` 字段(JSON 缺失) | `parseDeptCodes` 内部 try/catch 返回空列表(已有),无崩溃风险 |
| `List.of()` (Java 9+) 在 Java 17 上可用,无需兼容 | 项目已 Java 17,直接用 |
