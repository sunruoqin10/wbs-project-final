# JPSTN_CD 推断角色默认设置 实施 Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HR 同步时,按 `sys_user.jpstn_cd` 启发式推断角色默认设置:`BA → dept-project-manager`,`BF → project-manager`;不破坏现有手工 role 分配。

**Architecture:** 后端在 `UserService.syncHrData` 步 ④ 加 `inferRoleFromJpstnForHrSync()`,复用现有 `updateRoleAndScope` / `roleChangeLogMapper`;前端 Team.vue / OrgGroup.vue / RoleChangeDialog.vue / Settings.vue 加 Badge 与 hint。审计 `sys_role_change_log` 复用,顺手补 2 列。

**Tech Stack:** Spring Boot 3.2.0 + MyBatis + MySQL + Vue 3 + Pinia + Tailwind + i18n (zh/ko)

**Spec:** `docs/superpowers/specs/2026-06-16-jpstn-role-default-design.md` (3 轮 spec review 通过)

**Git Workflow 提醒(来自 `CLAUDE.md` / `AGENTS.md`):** 每个 Task 末尾的"Commit"步骤**需用户显式确认**才执行 `git add` / `git commit` / `git push`,不可自动执行。commit message 已在每步给出,实施时按 message 走。

---

## 文件结构总览

### 新建文件

| 路径 | 职责 |
|------|------|
| `backend/add_role_change_log_project_ids.sql` | 给 `sys_role_change_log` 加 2 列 |
| `backend/src/main/java/com/wbs/project/util/JpstnRoleMapping.java` | JPSTN_CD → role code 常量映射 |
| `backend/src/test/java/com/wbs/project/util/JpstnRoleMappingTest.java` | JpstnRoleMapping 单元测试 |
| `backend/src/test/java/com/wbs/project/service/UserServiceJpstnInferenceTest.java` | `inferRoleFromJpstnForHrSync` 单元测试(覆盖 §8 E1/E3/E4/E5/E7/E8/E9) |
| `backend/src/test/java/com/wbs/project/service/UserServiceFillMarkersTest.java` | `fillRoleInferredMarkers` 单元测试 |

### 修改文件

| 路径 | 改动范围 |
|------|---------|
| `backend/src/main/java/com/wbs/project/entity/User.java` | 加 2 个 `@Transient` 字段 |
| `backend/src/main/java/com/wbs/project/entity/RoleChangeLog.java` | 加 2 个字段 |
| `backend/src/main/java/com/wbs/project/mapper/UserMapper.java` | 加 2 个接口方法 |
| `backend/src/main/java/com/wbs/project/service/UserService.java` | 加 `inferRoleFromJpstnForHrSync` + `fillRoleInferredMarkers` + 接入 4 个返 User 入口 + 顺手补 `changeUserRole` |
| `backend/src/main/resources/mapper/UserMapper.xml` | 加 `selectMdmActiveEmpNums` + `selectLatestHrSyncInferences` 2 select |
| `backend/src/main/resources/mapper/RoleChangeLogMapper.xml` | resultMap + insert + selectByUserId 各加 2 列 |
| `frontend/src/types/index.ts` | User 加 2 可选字段 |
| `frontend/src/i18n/locales/zh.ts` | `team.roleSource.*` 加 4 个 key |
| `frontend/src/i18n/locales/ko.ts` | 同上 |
| `frontend/src/views/Team.vue` | 两处 Badge 改造 + RoleChangeDialog 加 prop |
| `frontend/src/components/team/OrgGroup.vue` | Badge 改造 |
| `frontend/src/components/team/RoleChangeDialog.vue` | 加 `currentRoleAutoInferred` prop + 顶部蓝色 hint |
| `frontend/src/views/Settings.vue` | 同步回调 message 含 inferred |
| `frontend/src/services/api.ts` | `syncHrUsers` 返回类型加 `inferred: number` |

---

## Task 1 — 数据库加列(SQL 必须先于代码)

**Files:**
- Create: `backend/add_role_change_log_project_ids.sql`

- [ ] **Step 1: 写 SQL 文件**

完整内容见 spec §4.1,关键行:

```sql
ALTER TABLE sys_role_change_log
  ADD COLUMN old_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更前管辖项目 ID(JSON 数组)' AFTER new_managed_company_cd,
  ADD COLUMN new_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更后管辖项目 ID(JSON 数组)' AFTER old_managed_project_ids,
  ALGORITHM=INPLACE, LOCK=NONE;
```

完整头注释(必须含 `⚠️` 警告 + MySQL 5.6+ 提示),见 spec §4.1 line 90-104。

- [ ] **Step 2: 跑 SQL(用户手动)**

⚠️ **必须由用户手动执行**(本工具按 `CLAUDE.md` Service Startup 规则不连 DB / 不起服务 / 不自动写数据)。

```bash
# 用户在 mysql 客户端跑:
mysql -uroot -proot db_webwbs < backend/add_role_change_log_project_ids.sql
# 验证:
mysql -uroot -proot db_webwbs -e "DESCRIBE sys_role_change_log;" | grep managed_project_ids
# 期望:看到 old_managed_project_ids / new_managed_project_ids 两行
```

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add backend/add_role_change_log_project_ids.sql
git commit -m "feat(audit): sys_role_change_log 增加 managed_project_ids 字段"
```

---

## Task 2 — RoleChangeLog 实体 + Mapper XML 改造

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/entity/RoleChangeLog.java:14-26`
- Modify: `backend/src/main/resources/mapper/RoleChangeLogMapper.xml:6-18` (resultMap), `21-31` (insert), `33-41` (selectByUserId)

- [ ] **Step 1: 加 RoleChangeLog 实体 2 字段**

在 `private String reason;` 之前加:

```java
private String oldManagedProjectIds;     // 变更前管辖项目 ID(JSON 数组字符串)
private String newManagedProjectIds;     // 变更后管辖项目 ID(JSON 数组字符串)
```

- [ ] **Step 2: 改 RoleChangeLogMapper.xml resultMap**

在 `<result column="new_managed_company_cd" property="newManagedCompanyCd"/>` 之后加 2 行:

```xml
<result column="old_managed_project_ids" property="oldManagedProjectIds"/>
<result column="new_managed_project_ids" property="newManagedProjectIds"/>
```

- [ ] **Step 3: 改 RoleChangeLogMapper.xml insert**

把现有 INSERT 段 columns + VALUES 都加 2 列(`old_managed_project_ids, new_managed_project_ids`),见 spec §5.7 第 39-55 行。

- [ ] **Step 4: 改 RoleChangeLogMapper.xml selectByUserId**

在 SELECT 列段加 `old_managed_project_ids, new_managed_project_ids`,见 spec §5.7 第 59-68 行。

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn clean install -DskipTests
```

预期:`BUILD SUCCESS`,无错。

- [ ] **Step 6: Commit(待用户确认)**

```bash
git add backend/src/main/java/com/wbs/project/entity/RoleChangeLog.java \
        backend/src/main/resources/mapper/RoleChangeLogMapper.xml
git commit -m "feat(audit): RoleChangeLog 加 managed_project_ids 审计字段"
```

---

## Task 3 — JpstnRoleMapping 工具类 + 单测(TDD)

**Files:**
- Create: `backend/src/main/java/com/wbs/project/util/JpstnRoleMapping.java`
- Create: `backend/src/test/java/com/wbs/project/util/JpstnRoleMappingTest.java`

- [ ] **Step 1: 写测试(JUnit 5)**

```java
package com.wbs.project.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JpstnRoleMappingTest {

    @Test
    void inferRoleCode_ba_returnsDeptProjectManager() {
        assertEquals("dept-project-manager", JpstnRoleMapping.inferRoleCode("BA"));
    }

    @Test
    void inferRoleCode_bf_returnsProjectManager() {
        assertEquals("project-manager", JpstnRoleMapping.inferRoleCode("BF"));
    }

    @Test
    void inferRoleCode_unknownCd_returnsNull() {
        assertNull(JpstnRoleMapping.inferRoleCode("X1"));
        assertNull(JpstnRoleMapping.inferRoleCode(""));
        assertNull(JpstnRoleMapping.inferRoleCode(null));
    }

    @Test
    void describe_ba_returnsReadableText() {
        String text = JpstnRoleMapping.describe("BA");
        assertNotNull(text);
        assertTrue(text.contains("BA"));
        assertTrue(text.contains("部门项目负责人"));
    }

    @Test
    void describe_unknownCd_returnsNull() {
        assertNull(JpstnRoleMapping.describe("X1"));
        assertNull(JpstnRoleMapping.describe(null));
    }
}
```

- [ ] **Step 2: 跑测试,确认 fail**

```bash
cd backend && mvn test -Dtest=JpstnRoleMappingTest
```

预期:FAIL(`JpstnRoleMapping` 类不存在)

- [ ] **Step 3: 实现 JpstnRoleMapping**

完整内容见 spec §5.1 line 108-145。要点:
- `public static final String JPSTN_DEPT_PM = "BA";`
- `public static final String JPSTN_PROJECT_PM = "BF";`
- `inferRoleCode(String jpstnCd)` 命中返回 `UserRole.DEPT_PROJECT_MANAGER.code` / `PROJECT_MANAGER.code`,否则 null
- `describe(String jpstnCd)` 返回 `"JPSTN_CD=X → 部门项目负责人"` 格式

- [ ] **Step 4: 跑测试,确认 pass**

```bash
cd backend && mvn test -Dtest=JpstnRoleMappingTest
```

预期:5 tests passed

- [ ] **Step 5: Commit(待用户确认)**

```bash
git add backend/src/main/java/com/wbs/project/util/JpstnRoleMapping.java \
        backend/src/test/java/com/wbs/project/util/JpstnRoleMappingTest.java
git commit -m "feat(hr-sync): JpstnRoleMapping JPSTN_CD→role 常量映射 + 单测"
```

---

## Task 4 — UserMapper 加 2 个 select

**Files:**
- Modify: `backend/src/main/resources/mapper/UserMapper.xml` (在 `</mapper>` 之前新增 2 个 select)
- Modify: `backend/src/main/java/com/wbs/project/mapper/UserMapper.java` (加 2 个接口方法)

- [ ] **Step 1: 加 selectMdmActiveEmpNums + selectLatestHrSyncInferences 到 UserMapper.xml**

完整 SQL 见 spec §5.2.1 line 150-159 + §5.2.2 line 162-184。

要点:
- `selectMdmActiveEmpNums` resultType=String,过滤 `EMP_NAM IS NOT NULL AND EMAIL_ADDR IS NOT NULL AND ACT_CLSS_CD IN ('C','H') AND EMP_NUM IS NOT NULL`
- `selectLatestHrSyncInferences` resultType=java.util.HashMap,用 `MAX(id) GROUP BY user_id` 子查询 + `SUBSTRING_INDEX` 解析 reason

- [ ] **Step 2: 加 UserMapper.java 接口**

```java
List<String> selectMdmActiveEmpNums();
List<java.util.Map<String, Object>> selectLatestHrSyncInferences(List<String> ids);  // SQL resultType="java.util.HashMap" 实际返 Object,需在 get 时 (String) 强转
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn clean install -DskipTests
```

预期:`BUILD SUCCESS`

- [ ] **Step 4: Commit(待用户确认)**

```bash
git add backend/src/main/resources/mapper/UserMapper.xml \
        backend/src/main/java/com/wbs/project/mapper/UserMapper.java
git commit -m "feat(hr-sync): UserMapper 加 selectMdmActiveEmpNums + selectLatestHrSyncInferences"
```

---

## Task 5 — User 实体加 2 个标记字段(无注解,MyBatis-only)

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/entity/User.java:24-39` (HR 扩展字段段之后)

**方案决策(2026-06-16 实施时调整)**:原 plan 用 `@Transient`(JPA 注解),但项目是 MyBatis-only(pom.xml 无 JPA 依赖),改用**纯字段** —— MyBatis XML mapper 不写这俩字段就不入库;Jackson 默认序列化所有 getter/setter 暴露的字段(前端 type 正好要这俩字段)。

- [ ] **Step 1: 加 2 个标记字段**

**不加 import**(原 plan 的 `import jakarta.persistence.Transient;` 取消)。在 `private Integer tokenVersion = 0;` 之后加:

```java
// === HR 推断标记(2026-06-16 新增,前端 Badge 用;不入库靠 MyBatis XML 不列这俩字段,JPA 注解不需要) ===
private Boolean roleAutoInferred;          // true = 此 role 由 HR_SYNC 按 JPSTN_CD 推断得来
private String roleInferredFromJpstn;      // 推断来源 JPSTN_CD('BA' / 'BF'),前端 hover 提示用
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn clean install -DskipTests
```

预期:`BUILD SUCCESS`

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add backend/src/main/java/com/wbs/project/entity/User.java
git commit -m "feat(hr-sync): User entity 加 roleAutoInferred / roleInferredFromJpstn 标记字段"
```

---

## Task 6 — UserService 核心改动(推断 + 填充 markers + 接入点 + 顺手补 changeUserRole)

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/service/UserService.java` (核心)

⚠️ **本 Task 是最复杂的一步**,代码量大。建议拆为多个 git commit(commit 1: 推断方法 + 接入点;commit 2: changeUserRole 顺手修;commit 3: 测试)。

- [ ] **Step 1: 写推断方法单测(失败)**

新建 `backend/src/test/java/com/wbs/project/service/UserServiceJpstnInferenceTest.java`,参考 spec §10.1 描述,至少覆盖:

```java
package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.RoleChangeLogMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceJpstnInferenceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleChangeLogMapper roleChangeLogMapper;
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void syncHrData_baMember_upgradesToDeptPm() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.syncHrInsert()).thenReturn(0);
        when(userMapper.syncHrUpdate()).thenReturn(1);
        when(userMapper.syncHrMarkResigned()).thenReturn(0);
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000001"));
        User u = new User();
        u.setId("C0000001");
        u.setRole("member");
        u.setJpstnCd("BA");
        u.setDeptCode("D001");
        u.setCompanyCd("2700");
        when(userMapper.selectByIds(List.of("C0000001"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertTrue(result.get("inferred") >= 1);
        // 断言 updateRoleAndScope 被调用且入参正确
        verify(userMapper).updateRoleAndScope(eq("C0000001"), eq("dept-project-manager"),
                anyString(), eq("2700"), isNull());
        // 断言 audit log 被写入
        ArgumentCaptor<...> logCaptor = ...;
        verify(roleChangeLogMapper).insert(...);
    }

    // E3: jpstnCd 不是 BA/BF 时不动
    @Test
    void syncHrData_unknownJpstnCd_skips() { ... }

    // E4: role 已是 admin 不动
    @Test
    void syncHrData_alreadyAdmin_skips() { ... }

    // E8: jpstnCd=BA 但 dept_code 为空跳过
    @Test
    void syncHrData_baButNoDeptCode_skips() { ... }

    // E9: jpstnCd=BA 但 company_cd 为空跳过
    @Test
    void syncHrData_baButNoCompanyCd_skips() { ... }
}
```

**注意**:
- 上述 4 个 E 场景(`unknownJpstnCd_skips` / `alreadyAdmin_skips` / `baButNoDeptCode_skips` / `baButNoCompanyCd_skips`)的完整断言由实施者按 spec §8 E3/E4/E8/E9 补全(核心断言:不调 `updateRoleAndScope`、不调 `roleChangeLogMapper.insert`)。E1 已给出完整示例,其余按 E1 模板改 mock 数据即可。
- **E14**(整事务回滚)不在单测覆盖范围,留到 Task 15 集成验收。
- mock `syncHrInsert / syncHrUpdate / syncHrMarkResigned` 返 0 是显式而非必要(`@Mock` 默认返 0),实施者可省略,但显式 mock 提高可读性。

- [ ] **Step 2: 跑测试,确认 fail**

```bash
cd backend && mvn test -Dtest=UserServiceJpstnInferenceTest
```

预期:FAIL(方法不存在 / mock 不匹配)

- [ ] **Step 3: 改 UserService — 加 import**

文件顶部加:
```java
import com.wbs.project.entity.RoleChangeLog;
import com.wbs.project.util.JpstnRoleMapping;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
```

- [ ] **Step 4: 改 UserService.syncHrData 内插步 ④**

在 `int updated = userMapper.syncHrUpdate();` 之后加:

```java
int inferred = inferRoleFromJpstnForHrSync();
```

在 `int resigned = userMapper.syncHrMarkResigned();` 之后加 `result.put("inferred", inferred);`。

完整 diff 见 spec §5.5.1。

- [ ] **Step 5: 加 inferRoleFromJpstnForHrSync 私有方法**

完整代码见 spec §5.5.2 line 254-315。要点:
- 不写 try/catch(让异常传播 → 事务回滚)
- `if (u == null) continue;` defensive coding
- `UserRole newRoleEnum = UserRole.fromCode(newRole);` 然后 enum == 比较
- `objectMapper.writeValueAsString(Collections.singletonList(u.getDeptCode()))` 序列化 BA 分支 managed_dept_codes
- `RoleChangeLog.setChangedBy("HR_SYNC")` + `reason = "自动推断:" + JpstnRoleMapping.describe(...)`
- `userMapper.updateRoleAndScope(...)` 复用,token_version +1 自动包含

- [ ] **Step 6: 加 fillRoleInferredMarkers 私有方法**

完整代码见 spec §5.5.3 line 336-352。要点:
- `Map<String, String> inferredMap = rows.stream().collect(Collectors.toMap(r -> (String) r.get("user_id"), r -> (String) r.get("jpstn_cd")));`
- 空 list 直接 return
- 遍历 users,命中的 setRoleAutoInferred(true) + setRoleInferredFromJpstn(jpstn)

- [ ] **Step 7: 接入 4 个返 User 入口**

按 spec §5.5.3 line 354-390,精确位置:
- `getAllUsers` 末尾
- `getUserById(String id)` 末尾(在 `return u` 之前,`if (u != null)` 保护)
- `getUsersByIds(List<String> ids)` 末尾
- `searchUsers(...)` 内部 `records = userMapper.searchUsers(...)` 之后、`Map.put("records", records)` 之前

- [ ] **Step 8: 跑推断方法单测,确认 pass**

```bash
cd backend && mvn test -Dtest=UserServiceJpstnInferenceTest
```

预期:所有测试 passed

- [ ] **Step 9: 写 fillRoleInferredMarkers 单测(失败)**

新建 `backend/src/test/java/com/wbs/project/service/UserServiceFillMarkersTest.java`,覆盖:
- `selectLatestHrSyncInferences` 返 2 行,断言 User.roleAutoInferred / roleInferredFromJpstn 填充正确
- 空 list 不报错

- [ ] **Step 10: 跑 fillRoleInferredMarkers 单测,确认 pass**

```bash
cd backend && mvn test -Dtest=UserServiceFillMarkersTest
```

预期:passed

- [ ] **Step 11: 顺手补 changeUserRole**

在 `UserService.changeUserRole` 现有 `RoleChangeLog` 赋值段末尾(约 L332-343)加 2 行:

```java
changeLog.setOldManagedProjectIds(target.getManagedProjectIds());
changeLog.setNewManagedProjectIds(jsonProjects);
```

**改动归属**:本 Step 的 2 行改动**计入 commit 6c**,不在 commit 6a / 6b 的 `git add` 范围(commit 6a / 6b 在 Step 8 / Step 10 完成后跑,那时本 Step 11 还没改 `UserService.java`,所以 6a / 6b 不会脏)。

- [ ] **Step 12: 全量编译验证**

```bash
cd backend && mvn clean install -DskipTests
```

预期:`BUILD SUCCESS`

- [ ] **Step 13: 全量单测**

```bash
cd backend && mvn -pl . test -DfailIfNoTests=false
```

预期:所有测试 passed(包括 JpstnRoleMappingTest / UserServiceJpstnInferenceTest / UserServiceFillMarkersTest)

- [ ] **Step 14: Commit(待用户确认)**

建议拆 3 个 commit(每个 commit 单独跑 build 验证):

```bash
# commit 6a: 推断方法 + 单测
git add backend/src/main/java/com/wbs/project/service/UserService.java \
        backend/src/test/java/com/wbs/project/service/UserServiceJpstnInferenceTest.java
git commit -m "feat(hr-sync): UserService.syncHrData 加 JPSTN_CD 启发式预填推断"

# commit 6b: fillRoleInferredMarkers + 接入点 + 单测
git add backend/src/main/java/com/wbs/project/service/UserService.java \
        backend/src/test/java/com/wbs/project/service/UserServiceFillMarkersTest.java
git commit -m "feat(hr-sync): fillRoleInferredMarkers 给 User 标自动分配来源"

# commit 6c: changeUserRole 顺手补 managed_project_ids 审计
git add backend/src/main/java/com/wbs/project/service/UserService.java
git commit -m "fix(audit): changeUserRole 切 PM 时也写 managed_project_ids 审计"
```

---

## Task 7 — 前端 User type

**Files:**
- Modify: `frontend/src/types/index.ts` (User 类型段)

- [ ] **Step 1: 加 2 个可选字段**

在 User type 末尾加:

```ts
/** HR 同步按 JPSTN_CD 推断得来(2026-06-16) */
roleAutoInferred?: boolean;
/** 推断来源 JPSTN_CD('BA' / 'BF') */
roleInferredFromJpstn?: string;
```

- [ ] **Step 2: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无新增错误

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add frontend/src/types/index.ts
git commit -m "feat(types): User 加 roleAutoInferred / roleInferredFromJpstn 字段"
```

---

## Task 8 — i18n 加 key(zh + ko)

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`
- Modify: `frontend/src/i18n/locales/ko.ts`

- [ ] **Step 1: 在 zh.ts `team.roleChange` 段附近加 `team.roleSource` 段**

```ts
roleSource: {
  autoInferred: '职级推断',
  autoInferredHint: '此角色由 HR 同步按职级自动分配,手动修改后不再自动覆盖',
  fromBa: '来源:BA(部门项目负责人)',
  fromBf: '来源:BF(项目经理)',
},
```

- [ ] **Step 2: 在 ko.ts 同位置加 `roleSource` 段**

```ts
roleSource: {
  autoInferred: '직급 추론',
  autoInferredHint: '이 역할은 HR 동기화로 직급에 따라 자동 할당되었으며,수동으로 수정하면 더 이상 자동 덮어쓰지 않습니다',
  fromBa: '출처:BA(부서 프로젝트 책임자)',
  fromBf: '출처:BF(프로젝트 매니저)',
},
```

- [ ] **Step 3: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无错

- [ ] **Step 4: Commit(待用户确认)**

```bash
git add frontend/src/i18n/locales/zh.ts frontend/src/i18n/locales/ko.ts
git commit -m "feat(i18n): 加 team.roleSource 4 个 key(zh+ko)"
```

---

## Task 9 — OrgGroup.vue Badge 改造

**Files:**
- Modify: `frontend/src/components/team/OrgGroup.vue:85-88`

- [ ] **Step 1: 改 Badge 段(包一个 div flex)**

把现有:
```vue
<Badge :variant="roleBadgeVariant(user.role)">
  {{ roleLabel(user.role) }}
</Badge>
```

改为(spec §6.3 完整版):
```vue
<div class="flex items-center gap-1">
  <Badge :variant="roleBadgeVariant(user.role)">
    {{ roleLabel(user.role) }}
  </Badge>
  <Badge
    v-if="user.roleAutoInferred"
    variant="info"
    :title="user.roleInferredFromJpstn === 'BA'
      ? $t('team.roleSource.fromBa')
      : (user.roleInferredFromJpstn === 'BF'
        ? $t('team.roleSource.fromBf')
        : $t('team.roleSource.autoInferredHint'))"
  >
    {{ $t('team.roleSource.autoInferred') }}
  </Badge>
</div>
```

- [ ] **Step 2: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无新增错误

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add frontend/src/components/team/OrgGroup.vue
git commit -m "feat(team): OrgGroup 成员卡片 role Badge 旁加'职级推断'小标"
```

---

## Task 10 — Team.vue Badge 改造 + RoleChangeDialog prop

**Files:**
- Modify: `frontend/src/views/Team.vue` (L142-145 / L249-251 两处 Badge + L497-505 RoleChangeDialog 调用处)

- [ ] **Step 1: 改两处 Badge(同 OrgGroup.vue 改法)**

把 L142-145 和 L249-251 的 `<Badge>` 都包到 `<div class="flex items-center gap-1">` 中 + 加自动分配小 Badge(完整代码同 Task 9 Step 1)。

- [ ] **Step 2: 给 RoleChangeDialog 调用处追加 prop**

在 L497-505 现有 props 中加一行:

```vue
:current-role-auto-inferred="roleChangeTarget?.roleAutoInferred || false"
```

- [ ] **Step 3: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无新增错误(因 RoleChangeDialog 的 prop 是可选的,默认 false)

- [ ] **Step 4: Commit(待用户确认)**

```bash
git add frontend/src/views/Team.vue
git commit -m "feat(team): Team.vue 两处 Badge 加自动分配小标 + RoleChangeDialog prop"
```

---

## Task 11 — RoleChangeDialog.vue 加 prop + 蓝色 hint

**Files:**
- Modify: `frontend/src/components/team/RoleChangeDialog.vue` (Props interface + withDefaults + 模板 L273-275 后)

- [ ] **Step 1: 改 Props interface**

在 `Props` interface 末尾加:

```ts
/** 当前 role 是否由 HR 同步按 JPSTN_CD 自动推断(2026-06-16) */
currentRoleAutoInferred?: boolean;
```

- [ ] **Step 2: 改 withDefaults 默认值**

在 `withDefaults` 默认值对象末尾加:

```ts
currentRoleAutoInferred: false,   // 默认 false(不影响现有调用方)
```

- [ ] **Step 3: 在模板 L273-275 警告框下方插入蓝色 hint**

定位:`<div class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">` 警告框之后。

插入:

```vue
<div
  v-if="currentRoleAutoInferred"
  class="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800"
>
  <span class="font-medium">{{ $t('team.roleSource.autoInferred') }}:</span>
  {{ $t('team.roleSource.autoInferredHint') }}
</div>
```

- [ ] **Step 4: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无错

- [ ] **Step 5: Commit(待用户确认)**

```bash
git add frontend/src/components/team/RoleChangeDialog.vue
git commit -m "feat(team): RoleChangeDialog 加 currentRoleAutoInferred prop + 自动分配 hint"
```

---

## Task 12 — api.ts syncHrUsers 返回类型扩展

**Files:**
- Modify: `frontend/src/services/api.ts:461-464`

⚠️ **必须先做本 Task**,否则 Task 13 Settings.vue 改 `result.inferred` 时 `npx vue-tsc` 会报错。

- [ ] **Step 1: 改 syncHrUsers 返回类型**

把现有:
```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number }> {
  return request<{ inserted: number; updated: number; resigned: number }>('/users/sync-hr', {
    method: 'POST',
  });
}
```

改为:
```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number; inferred: number }> {
  return request<{ inserted: number; updated: number; resigned: number; inferred: number }>('/users/sync-hr', {
    method: 'POST',
  });
}
```

- [ ] **Step 2: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无错

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add frontend/src/services/api.ts
git commit -m "feat(api): syncHrUsers 返回类型加 inferred 字段"
```

---

## Task 13 — Settings.vue 同步回调 message 含 inferred

**Files:**
- Modify: `frontend/src/views/Settings.vue` (handleSyncHr 回调段)

- [ ] **Step 1: 改 handleSyncHr 的成功分支**

**前置**:Task 12(spec §6.6.1)已先扩展 `syncHrUsers` 返回类型加 `inferred: number`,本处引用 `result.inferred` 不会触发 `vue-tsc` 报错。

找到现有的:
```ts
syncResult.value = {
  success: true,
  message: '同步完成:新增 ... / 更新 ... / 标记离职 ...',
};
```

改为(参考 spec §6.6.2):
```ts
const result = await apiService.syncHrUsers();
const msg = `同步完成:新增 ${result.inserted} / 更新 ${result.updated} / 标记离职 ${result.resigned}` +
            (result.inferred > 0 ? ` / 职级自动推断 ${result.inferred} 人(被升级用户需重新登录)` : '');
syncResult.value = { success: true, message: msg };
```

注意:项目现有 `result` 变量名可能与 `e` 冲突,实施时按现场命名调整(直接拿到的就是 `result`,不再 destruct)。

- [ ] **Step 2: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无错

- [ ] **Step 3: Commit(待用户确认)**

```bash
git add frontend/src/views/Settings.vue
git commit -m "feat(settings): HR 同步成功 toast 含职级推断计数"
```

---

## Task 14 — 前端全量校验

**Files:**
- 无文件改动,纯校验

- [ ] **Step 1: Type check**

```bash
cd frontend && npx vue-tsc --noEmit
```

预期:无错(若有错,回头定位修复)

- [ ] **Step 2: Build**

```bash
cd frontend && npm run build
```

预期:`vite build` 成功 + `vue-tsc` 通过

- [ ] **Step 3: 后端最终编译**

```bash
cd backend && mvn clean install -DskipTests
```

预期:`BUILD SUCCESS`

- [ ] **Step 4: Commit(无需 commit,本 Task 纯校验)**

---

## Task 15 — 端到端验收(spec §10.2 11 步)

**Files:**
- 无文件改动,纯手工验收

⚠️ **本 Task 需用户手动执行**(起服务、调接口、看 UI),本工具按 `CLAUDE.md` Service Startup 规则不自动起服务 / 不连 DB / 不发 HTTP。

请用户按以下 11 步验收,实施者在旁协助并贴日志:

- [ ] **Step 1**:在 `mdm_if_pa_a` 临时把 C0000001 的 `JPSTN_CD='BA'`

- [ ] **Step 2**:admin 调 `POST /api/users/sync-hr`
  - 期望响应:`{code:200, data:{inserted:0, updated:N, resigned:0, inferred:>=1}}`

- [ ] **Step 3**:`SELECT role, managed_dept_codes, managed_company_cd, token_version FROM sys_user WHERE id='C0000001'`
  - 期望:`role='dept-project-manager'`、`managed_dept_codes=["<原 dept_code>"]`、`managed_company_cd=<原 company_cd>`、`token_version` 比改动前 +1

- [ ] **Step 4**:`SELECT * FROM sys_role_change_log WHERE user_id='C0000001' ORDER BY changed_at DESC LIMIT 1`
  - 期望:`changed_by='HR_SYNC'`、`reason='自动推断:JPSTN_CD=BA → 部门项目负责人'`

- [ ] **Step 5**:C0000001 用旧 JWT 调任意接口
  - 期望:`401`(token_version +1 导致旧 token 失效)

- [ ] **Step 6**:C0000001 重新登录
  - 期望:拿到 dept-project-manager 权限

- [ ] **Step 7**:Team 页面看 C0000001 卡片
  - 期望:role Badge 旁有蓝色"职级推断"小 Badge,hover 提示"来源:BA(部门项目负责人)"

- [ ] **Step 8**:打开 C0000001 的 RoleChangeDialog
  - 期望:顶部出现蓝色 hint "此角色由 HR 同步按职级自动分配,手动修改后不再自动覆盖"

- [ ] **Step 9**:再次 `sync-hr`(JPSNT 没变)
  - 期望:`inferred=0`(role 已不是 member,不重复推断)

- [ ] **Step 10**:admin 手工把 C0000001 改回 `member`
  - 期望:生效

- [ ] **Step 11**:再次 `sync-hr`(JPSNT 仍是 BA)
  - 期望:`inferred>=1`(重新升级,与启发式预填规则一致)

---

## 实施完毕后

- [ ] **最后一步**:把 spec 文档(`docs/superpowers/specs/2026-06-16-jpstn-role-default-design.md`)与所有代码 commit 一起提交(用户之前已选"不 commit,留到实施一起提交")。

建议总 commit 顺序(用户可自由合并):
```
1. feat(audit): sys_role_change_log 增加 managed_project_ids 字段
2. feat(audit): RoleChangeLog 加 managed_project_ids 审计字段
3. feat(hr-sync): JpstnRoleMapping JPSTN_CD→role 常量映射 + 单测
4. feat(hr-sync): UserMapper 加 selectMdmActiveEmpNums + selectLatestHrSyncInferences
5. feat(hr-sync): User entity 加 roleAutoInferred Transient 字段
6. feat(hr-sync): UserService.syncHrData 加 JPSTN_CD 启发式预填推断
7. feat(hr-sync): fillRoleInferredMarkers 给 User 标自动分配来源
8. fix(audit): changeUserRole 切 PM 时也写 managed_project_ids 审计
9. feat(types): User 加 roleAutoInferred / roleInferredFromJpstn 字段
10. feat(i18n): 加 team.roleSource 4 个 key(zh+ko)
11. feat(team): OrgGroup 成员卡片 role Badge 旁加'职级推断'小标
12. feat(team): Team.vue 两处 Badge 加自动分配小标 + RoleChangeDialog prop
13. feat(team): RoleChangeDialog 加 currentRoleAutoInferred prop + 自动分配 hint
14. feat(api): syncHrUsers 返回类型加 inferred 字段
15. feat(settings): HR 同步成功 toast 含职级推断计数
16. docs(spec): JPSTN_CD 推断角色默认设置(此 spec 文档)
```

每个 commit 需用户显式确认。

---

## 风险提醒(来自 spec §9)

- **R1**:HR 同步在 `@Transactional` 中,推断路径异常会整事务回滚 → 已修复:移除 try/catch,接受整事务原子语义
- **R3**:`SUBSTRING_INDEX` 解析 reason,文案变化会失效 → 已约定 reason 格式
- **R4**:被升级用户必须重新登录 → 已在 Settings.vue toast 文案中提示"被升级用户需重新登录"
- **R10**:RoleChangeLog 加 2 列后 `selectByUserId` 返参多 2 字段 → 已评估,本项目无按位置解析的外部调用方

---

**Plan 文档结束**