# JPSTN_CD 推断角色默认设置 — 设计

> 日期:2026-06-16
> 状态:已设计,待提交 + spec review
> 前置 spec: [`2026-06-16-hr-sync-jpstn-design.md`](./2026-06-16-hr-sync-jpstn-design.md)(`sys_user.jpstn_cd / jpstn_nam` 已落地)

## 1. 背景与目标

`sys_user.jpstn_cd / jpstn_nam`(职级)已于 2026-06-16 由 `mdm_if_pa_a.JPSTN_CD / JPSTN_NAM` 同步进来,但目前**没有任何业务逻辑引用** —— 角色(role)仍由 HR 同步时硬编码为 `'member'`(`UserMapper.xml:syncHrInsert`),真正的角色分配由 admin / dept-pm 在 Team 页面手工 `changeUserRole`。

**诉求**:按职级自动给一个默认角色:
- `JPSTN_CD = BA` → 默认 `dept-project-manager`(部门项目负责人)
- `JPSTN_CD = BF` → 默认 `project-manager`(项目经理)

**目标**:HR 同步时,如果用户的 `sys_user.role` 仍为默认 `'member'`,按 `JPSTN_CD` 自动升级 role 并填好 `managed_*` 字段、写审计 `sys_role_change_log`、让前端用 Badge 标出来。

## 2. 核心规则(决策表)

### 2.1 推断映射

| JPSTN_CD | 推断出的 role | managed_dept_codes | managed_company_cd | managed_project_ids |
|---------|--------------|--------------------|--------------------|---------------------|
| `BA` | `dept-project-manager` | `["<user.dept_code>"]`(单部门 JSON 数组) | `<user.company_cd>` | 清空 |
| `BF` | `project-manager` | 清空 | 清空 | `"[]"`(空,后续手动分项目) |
| 其他(含空 / null) | (不动) | (不动) | (不动) | (不动) |

### 2.2 触发条件(启发式预填)

```
if sys_user.role == 'member' AND sys_user.jpstnCd ∈ {BA, BF}:
    按 2.1 表升级 role + managed_*
    token_version += 1
    写 sys_role_change_log (changed_by='HR_SYNC')
else:
    skip  # 包括 role 已是 admin/dept-pm/pm/viewer 的用户
```

**关键原则**:
- 只在 role 仍为默认 `member` 时才推断升级
- 一旦被 admin / dept-pm 手工改过(role 不是 member),HR 同步不再覆盖
- **不反向降级**(JPSNT 从 BA 变成 X1 时,role=dept-pm 保持不变;本次 YAGNI,见 §7)
- 离职(`status='T'`)的用户不参与推断(由 `selectMdmActiveEmpNums` 自然过滤)

### 2.3 异常 / 跳过场景

| 场景 | 行为 |
|------|------|
| JPSNT=BA 但 `user.dept_code` 为空 | 跳过 + WARN 日志,不升级 |
| JPSNT=BA 但 `user.company_cd` 为空 | 跳过 + WARN 日志,不升级 |
| HR sync-hr 在步 ④ 某条 update 失败 | 整 `@Transactional` 回滚(连同 `syncHrInsert` / `syncHrUpdate`),HR 重跑即可 |

## 3. 架构与数据流

```
admin 点击 Settings → 同步按钮
   │
   └─→ POST /api/users/sync-hr           (现有,UserController.java:114)
         │
         └─→ UserService.syncHrData()     (UserService.java:228)  @Transactional
               │
               ├─ ① 现有 admin 安全闸: selectAdminIdsNotInMdm()        ← 不动
               │
               ├─ ② syncHrInsert()         ← 不动 SQL
               ├─ ③ syncHrUpdate()         ← 不动 SQL
               │
               ├─ ④ ★ inferRoleFromJpstnForHrSync()  [新]
               │      │
               │      ├─ userMapper.selectMdmActiveEmpNums()           [新 select]
               │      ├─ userMapper.selectByIds(empNums)               ← 复用
               │      └─ 对每个 user (try-catch 单条):
               │            if role == 'member' AND jpstnCd ∈ {BA,BF}:
               │              ├─ JpstnRoleMapping.inferRoleCode(jpstnCd)  [新 util]
               │              ├─ 按 2.1 表构造 managed_* JSON
               │              ├─ userMapper.updateRoleAndScope(...)     ← 复用(包含 token_version+1)
               │              └─ roleChangeLogMapper.insert({changedBy: 'HR_SYNC',
               │                                                reason: '自动推断:JPSTN_CD=BA → ...'})
               │
               ├─ ⑤ syncHrMarkResigned()    ← 不动
               │
               └─ return {inserted, updated, resigned, inferred}
```

**职责边界**:
- 不引入新接口;HR 同步外的任何路径(`createUser` / `updateUser` / `changeUserRole` / 登录)都不受影响
- 推断是 `syncHrData` 内的"附加副作用",失败时整 `@Transactional` 回滚(HR 重跑即可,无脏数据)

## 4. 数据库变更

### 4.1 `backend/add_role_change_log_project_ids.sql`(新建)

**重要**:本 SQL 必须在服务启动之前跑(否则 `RoleChangeLogMapper.xml` 加了 resultMap 字段后,MyBatis 启动会因 JSON 列不存在而失败)。

```sql
-- ===============================================================
-- 2026-06-16: sys_role_change_log 增加 managed_project_ids 字段
-- 来源 spec: docs/superpowers/specs/2026-06-16-jpstn-role-default-design.md
-- 范围: HR 推断 BF→PM 与手工切 PM 时,把 managed_project_ids 一并记入审计
-- 现状: 2026-06-12 引入 PM 时漏记,本次顺手补
-- ⚠️ 必须先执行本 SQL 再启动服务,否则 MyBatis resultMap 字段映射失败
-- 要求 MySQL 5.6+(ALGORITHM=INPLACE, LOCK=NEE 语法支持)
-- ===============================================================
ALTER TABLE sys_role_change_log
  ADD COLUMN old_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更前管辖项目 ID(JSON 数组)' AFTER new_managed_company_cd,
  ADD COLUMN new_managed_project_ids JSON DEFAULT NULL
    COMMENT '变更后管辖项目 ID(JSON 数组)' AFTER old_managed_project_ids,
  ALGORITHM=INPLACE, LOCK=NONE;
```

无新索引(查 `MAX(id) GROUP BY user_id` 走 `idx_user_id` 已存在,见 `add_role_management_v2.sql` L84)。

### 4.2 不动 `sys_user`(本次纯运行时推断,不存"是否推断过"的列)

## 5. 后端改动

### 5.1 新文件:`backend/src/main/java/com/wbs/project/util/JpstnRoleMapping.java`

```java
package com.wbs.project.util;

import com.wbs.project.enums.UserRole;

/**
 * JPSTN_CD → role 推断常量(2026-06-16 引入)
 *
 * 触发条件(在 UserService 中判断):
 *   - 当前 sys_user.role == 'member'
 *   - 当前 sys_user.jpstnCd ∈ {BA, BF}
 *
 * 命中后由 UserService.syncHrData 步 ④ 升级 role / managed_* + 写 audit。
 *
 * 未来 BA/BF 之外的职级若需走默认映射,在此处加一行即可。
 */
public final class JpstnRoleMapping {

    /** BA → 部门项目负责人 */
    public static final String JPSTN_DEPT_PM   = "BA";
    /** BF → 项目经理 */
    public static final String JPSTN_PROJECT_PM = "BF";

    private JpstnRoleMapping() {}

    /**
     * @return 推断出的 UserRole.code;null 表示不升级(保持 member 或不动)
     */
    public static String inferRoleCode(String jpstnCd) {
        if (jpstnCd == null) return null;
        if (JPSTN_DEPT_PM.equals(jpstnCd))   return UserRole.DEPT_PROJECT_MANAGER.code;
        if (JPSTN_PROJECT_PM.equals(jpstnCd)) return UserRole.PROJECT_MANAGER.code;
        return null;
    }

    /** 给审计 reason 用的可读描述 */
    public static String describe(String jpstnCd) {
        String code = inferRoleCode(jpstnCd);
        if (code == null) return null;
        return "JPSTN_CD=" + jpstnCd + " → " + UserRole.fromCode(code).getDescription();
    }
}
```

### 5.2 `UserMapper.xml` 改动 — 加 2 个 select

```xml
<!-- 5.2.1 配套 1: mdm 中 C/H 在职的 EMP_NUM 列表(覆盖 insert + update 两条路径) -->
<select id="selectMdmActiveEmpNums" resultType="java.lang.String">
    SELECT DISTINCT p.EMP_NUM
      FROM mdm_if_pa_a p
     WHERE p.EMP_NAM IS NOT NULL
       AND p.EMAIL_ADDR IS NOT NULL
       AND p.ACT_CLSS_CD IN ('C','H')
       AND p.EMP_NUM IS NOT NULL
</select>

<!-- 5.2.2 配套 2: 批量拿最近一次 HR_SYNC 推断的 JPSTN_CD 来源(从 reason 字符串解析) -->
<select id="selectLatestHrSyncInferences" resultType="java.util.HashMap">
    SELECT user_id,
           SUBSTRING_INDEX(SUBSTRING_INDEX(reason, 'JPSTN_CD=', -1), ' ', 1) AS jpstn_cd
      FROM sys_role_change_log
     WHERE changed_by = 'HR_SYNC'
       AND user_id IN
       <foreach collection="ids" item="id" open="(" separator="," close=")">
           #{id}
       </foreach>
       AND id IN (
         SELECT MAX(id) FROM sys_role_change_log
          WHERE changed_by = 'HR_SYNC'
            AND user_id IN
            <foreach collection="ids" item="id2" open="(" separator="," close=")">
                #{id2}
            </foreach>
          GROUP BY user_id
       )
</select>
```

### 5.3 `UserMapper.java` 接口补充

```java
List<String> selectMdmActiveEmpNums();
List<java.util.Map<String, Object>> selectLatestHrSyncInferences(List<String> ids);  // SQL resultType="java.util.HashMap" 实际返 Object,需在 get 时 (String) 强转
```

### 5.4 `User.java` 加 2 个标记字段(无注解,靠 MyBatis XML / Jackson 默认行为)

**为什么不用 `@Transient`**:项目是 MyBatis-only(pom.xml 无 JPA 依赖),`@Transient` 是 JPA 注解,加它必须先引入 `spring-boot-starter-data-jpa`(30+ 传递依赖)。MyBatis 的持久化由 XML mapper 决定,XML 里没写的字段天然不入库;Jackson 默认会序列化所有 getter/setter 暴露的字段,前端要的就是这俩字段,正好是 feature 不是 bug。所以**不加任何注解,纯字段即可**。

```java
// === HR 推断标记(2026-06-16 新增,前端 Badge 用;不入库靠 MyBatis XML 不列这俩字段,JPA 注解不需要) ===
private Boolean roleAutoInferred;          // true = 此 role 由 HR_SYNC 按 JPSTN_CD 推断得来

private String roleInferredFromJpstn;      // 推断来源 JPSTN_CD('BA' / 'BF'),前端 hover 提示用
```

### 5.5 `UserService.java` 改动

#### 5.5.1 `syncHrData` 内插步 ④

```java
@Transactional
public java.util.Map<String, Integer> syncHrData() {
    // ① 现有 admin 安全闸(不动)
    java.util.List<String> adminIds = userMapper.selectAdminIdsNotInMdm();
    if (!adminIds.isEmpty()) { /* 抛 409,原有逻辑 */ }

    // ② ③ 现有 syncHrInsert / syncHrUpdate(不动)
    int inserted = userMapper.syncHrInsert();
    int updated  = userMapper.syncHrUpdate();

    // ④ ★ 新增:HR 同步后按 JPSTN_CD 推断角色(启发式预填)
    int inferred = inferRoleFromJpstnForHrSync();

    // ⑤ 现有 syncHrMarkResigned(不动)
    int resigned = userMapper.syncHrMarkResigned();

    java.util.Map<String, Integer> result = new java.util.HashMap<>();
    result.put("inserted", inserted);
    result.put("updated",  updated);
    result.put("resigned", resigned);
    result.put("inferred", inferred);   // 新增统计
    return result;
}
```

#### 5.5.2 新增 `inferRoleFromJpstnForHrSync()` 私有方法

```java
// 需要 import: java.util.Collections / java.util.List
// 不需要 import: java.util.Map / java.util.stream.Collectors(本方法不用)

/**
 * 对"mdm 中 C/H 在职"的全体用户跑 JPSTN_CD → role 推断。
 * 仅当 sys_user.role == 'member' 且 jpstnCd ∈ {BA, BF} 时升级 role + managed_* + 写 audit。
 *
 * 事务语义:由外层 syncHrData 的 @Transactional 整事务包裹。
 * - 单条推断失败 → 异常向上传播 → 整事务回滚(连同 syncHrInsert / syncHrUpdate),HR 重跑即可。
 * - 这里不再写 try/catch:写了也无效,事务回滚照样发生,反而误导。
 */
private int inferRoleFromJpstnForHrSync() {
    List<String> empNums = userMapper.selectMdmActiveEmpNums();
    if (empNums == null || empNums.isEmpty()) return 0;

    List<User> users = userMapper.selectByIds(empNums);
    int inferred = 0;
    for (User u : users) {
        // 二次过滤保护:selectMdmActiveEmpNums 已 WHERE ACT_CLSS_CD IN ('C','H'),
        // 但 sys_user.status 在 syncHrMarkResigned(步 ⑤)之前可能与 mdm 不一致;
        // selectByIds 还 AND status != 'T',实测几乎不会触发 null,
        // 保留此 continue 作为 defensive coding 防 NPE,避免循环中某条 mapper 抛异常导致整循环崩
        if (u == null) continue;

        // 启发式预填:仅当 role 仍为默认 member 时才覆盖
        if (!"member".equals(u.getRole())) continue;
        String newRole = JpstnRoleMapping.inferRoleCode(u.getJpstnCd());
        if (newRole == null) continue;
        UserRole newRoleEnum = UserRole.fromCode(newRole);   // 与 changeUserRole 风格一致,后续用 enum == 比较

        String oldRole = u.getRole();
        // role=member 时 managed_* 无业务意义,audit 里 old_* 强制 null,避免"member 拥有 dept 管理权"的语义混乱
        String oldJsonCodes  = null;
        String oldCompanyCd  = null;
        String oldJsonProj   = null;
        String jsonCodes     = null;
        String companyCd     = null;
        String jsonProjects  = null;

        if (newRoleEnum == UserRole.DEPT_PROJECT_MANAGER) {
            // BA → dept-pm:managed_dept_codes = [user.dept_code], managed_company_cd = user.company_cd
            if (u.getDeptCode() == null || u.getDeptCode().isEmpty()) {
                log.warn("HR 推断跳过: user={} 职级=BA 但 dept_code 为空", u.getId());
                continue;
            }
            if (u.getCompanyCd() == null || u.getCompanyCd().isEmpty()) {
                log.warn("HR 推断跳过: user={} 职级=BA 但 company_cd 为空", u.getId());
                continue;
            }
            // 序列化风格与 changeUserRole 一致(objectMapper.writeValueAsString),
            // 而不是手拼 JSON,避免引号 / 转义错
            jsonCodes = objectMapper.writeValueAsString(Collections.singletonList(u.getDeptCode()));
            companyCd = u.getCompanyCd();
        } else if (newRoleEnum == UserRole.PROJECT_MANAGER) {
            // BF → pm:managed_project_ids = '[]'
            jsonProjects = "[]";
        }

        // 写审计(reuse RoleChangeLog,与 changeUserRole 同口径)
        RoleChangeLog changeLog = new RoleChangeLog();
        changeLog.setUserId(u.getId());
        changeLog.setOldRole(oldRole);
        changeLog.setNewRole(newRole);
        changeLog.setOldManagedDeptCodes(oldJsonCodes);
        changeLog.setNewManagedDeptCodes(jsonCodes);
        changeLog.setOldManagedCompanyCd(oldCompanyCd);
        changeLog.setNewManagedCompanyCd(companyCd);
        changeLog.setOldManagedProjectIds(oldJsonProj);
        changeLog.setNewManagedProjectIds(jsonProjects);
        changeLog.setChangedBy("HR_SYNC");          // ★ 标记来源(手工切 PM 时是 operatorId,不冲突)
        changeLog.setChangedAt(LocalDateTime.now());
        changeLog.setReason("自动推断:" + JpstnRoleMapping.describe(u.getJpstnCd()));
        roleChangeLogMapper.insert(changeLog);

        // 复用现有 mapper(token_version +1 自动包含)
        userMapper.updateRoleAndScope(u.getId(), newRole, jsonCodes, companyCd, jsonProjects);
        log.info("HR 推断: user={} jpstnCd={} {} → {}", u.getId(), u.getJpstnCd(), oldRole, newRole);
        inferred++;
    }
    return inferred;
}
```

#### 5.5.3 新增 `fillRoleInferredMarkers()` 私有方法 + 接入 4 个返 User 入口

```java
/**
 * 给一组 User 填充 roleAutoInferred / roleInferredFromJpstn(2026-06-16 新增)
 * 来源: sys_role_change_log 中 changed_by='HR_SYNC' 的最近一条记录
 * 性能: 单 SQL 批量查,O(1) round-trip;不影响现有 list / search / selectById
 */
private void fillRoleInferredMarkers(List<User> users) {
    if (users == null || users.isEmpty()) return;
    List<String> ids = users.stream().map(User::getId).collect(Collectors.toList());
    List<Map<String, String>> rows = userMapper.selectLatestHrSyncInferences(ids);
    // 显式 (String) cast:userMapper.selectLatestHrSyncInferences 实际返 List<Map<String, Object>>,
    // 这里用 Java 泛型擦除,需在 get 时强转,否则 mvn compile 报 unchecked warning
    Map<String, String> inferredMap = rows.stream()
        .collect(Collectors.toMap(r -> (String) r.get("user_id"), r -> (String) r.get("jpstn_cd")));
    for (User u : users) {
        String jpstn = inferredMap.get(u.getId());
        if (jpstn != null) {
            u.setRoleAutoInferred(true);
            u.setRoleInferredFromJpstn(jpstn);
        }
    }
}
```

接入点(精确位置,逐个方法末尾调 `fillRoleInferredMarkers`,只对 `List<User>` 起效):

```java
// === UserService.getAllUsers() 末尾 ===
public List<User> getAllUsers() {
    List<User> list = userMapper.selectAll();
    fillRoleInferredMarkers(list);
    return list;
}

// === UserService.getUserById(String id) 末尾(UserService.java L44) ===
public User getUserById(String id) {
    User u = userMapper.selectById(id);
    if (u != null) fillRoleInferredMarkers(Collections.singletonList(u));
    return u;
}

// === UserService.getUsersByIds(List<String> ids) 末尾(UserService.java L58) ===
public List<User> getUsersByIds(List<String> ids) {
    List<User> list = userMapper.selectByIds(ids);
    fillRoleInferredMarkers(list);
    return list;
}

// === UserService.searchUsers(...) 内部 records 装入 Map 之前(UserService.java L213) ===
// searchUsers 签名返 Map<String, Object>(含 records / total),不在 List<User> 末尾,
// 而是在 userMapper.searchUsers(keyword, offset, pageSize) 拿到 records 后立刻调
public java.util.Map<String, Object> searchUsers(String keyword, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    List<User> records = userMapper.searchUsers(keyword, offset, pageSize);
    fillRoleInferredMarkers(records);   // ★ 在 records 装入 Map 之前
    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("records", records);
    result.put("total", userMapper.countSearchUsers(keyword));
    return result;
}
```

#### 5.5.4 顺手补 `changeUserRole`(口径对齐)

在 `UserService.changeUserRole` 现有 `RoleChangeLog` 赋值段末尾加 2 行 setter(让手工切 PM 也写 `managed_project_ids` 审计):

```java
changeLog.setOldManagedProjectIds(target.getManagedProjectIds());
changeLog.setNewManagedProjectIds(jsonProjects);
```

### 5.6 `RoleChangeLog.java` 实体加 2 字段

```java
private String oldManagedProjectIds;     // 变更前管辖项目 ID(JSON 数组字符串)
private String newManagedProjectIds;     // 变更后管辖项目 ID(JSON 数组字符串)
```

### 5.7 `RoleChangeLogMapper.xml` 改 3 处(resultMap / insert / selectByUserId)

```xml
<!-- resultMap: 在 <result column="new_managed_company_cd" property="newManagedCompanyCd"/> 这行之后插入 -->
<result column="old_managed_project_ids" property="oldManagedProjectIds"/>
<result column="new_managed_project_ids" property="newManagedProjectIds"/>

<!-- insert values 段加 2 列 -->
INSERT INTO sys_role_change_log
  (user_id, old_role, new_role,
   old_managed_dept_codes, new_managed_dept_codes,
   old_managed_company_cd, new_managed_company_cd,
   old_managed_project_ids, new_managed_project_ids,
   changed_by, changed_at, reason)
VALUES
  (#{userId}, #{oldRole}, #{newRole},
   #{oldManagedDeptCodes}, #{newManagedDeptCodes},
   #{oldManagedCompanyCd}, #{newManagedCompanyCd},
   #{oldManagedProjectIds}, #{newManagedProjectIds},
   #{changedBy}, #{changedAt}, #{reason})

<!-- selectByUserId select 段也加 2 列,避免 audit 接口返回缺字段 -->
SELECT id, user_id, old_role, new_role,
       old_managed_dept_codes, new_managed_dept_codes,
       old_managed_company_cd, new_managed_company_cd,
       old_managed_project_ids, new_managed_project_ids,
       changed_by, changed_at, reason
  FROM sys_role_change_log
 WHERE user_id = #{userId}
 ORDER BY changed_at DESC
```

### 5.8 字段长度 / 取值校验

| 字段 | 类型 / 长度 | HR 推断写入值 | 是否合规 |
|------|------------|--------------|---------|
| `changed_by` | VARCHAR(20) | `'HR_SYNC'` (7 字符) | ✅ |
| `reason` | VARCHAR(500) | `'自动推断:JPSTN_CD=BA → 部门项目负责人'` (≈30 字符) | ✅ |
| `old_managed_dept_codes` / `new_managed_dept_codes` | JSON | `"[\"DEPT001\"]"` / `"[]"` | ✅ |
| `old_managed_company_cd` / `new_managed_company_cd` | VARCHAR(20) | `'2700'` / `'8400'` | ✅ |
| `old_managed_project_ids` / `new_managed_project_ids`(新增) | JSON | `'[]'` | ✅ |

### 5.9 token_version + JWT 失效链(无新代码,纯复用)

```
HR syncHrData → inferRoleFromJpstnForHrSync
    │
    ├─ userMapper.updateRoleAndScope(...)        ← 复用,UserMapper.xml:251
    │     └─ SET role=?, managed_*=?, token_version = token_version + 1
    │
    └─ 该用户当前 JWT (token_version=N) 立即失效
          └─ 下次请求 AuthInterceptor 比对:token 中 tokenVersion != DB 中 → 401
          └─ 前端 services/api.ts 拿到 401 → 跳登录页
```

## 6. 前端改动

### 6.1 `types/index.ts` User 加 2 个可选字段

```ts
/** HR 同步按 JPSTN_CD 推断得来(2026-06-16) */
roleAutoInferred?: boolean;
/** 推断来源 JPSTN_CD('BA' / 'BF') */
roleInferredFromJpstn?: string;
```

### 6.2 i18n(`zh.ts` + `ko.ts`)

**重要**:`frontend/src/i18n/locales/` 当前只有 `zh.ts` 和 `ko.ts`(无 `en.ts`),本次**只改这两个文件**。`en.ts` 不在本次范围(见 §7)。

`team.roleSource` 段新增 4 个 key:

| key | zh | ko |
|-----|----|----|
| `team.roleSource.autoInferred` | `职级推断` | `직급 추론` |
| `team.roleSource.autoInferredHint` | `此角色由 HR 同步按职级自动分配,手动修改后不再自动覆盖` | `이 역할은 HR 동기화로 직급에 따라 자동 할당되었으며,수동으로 수정하면 더 이상 자동 덮어쓰지 않습니다` |
| `team.roleSource.fromBa` | `来源:BA(部门项目负责人)` | `출처:BA(부서 프로젝트 책임자)` |
| `team.roleSource.fromBf` | `来源:BF(项目经理)` | `출처:BF(프로젝트 매니저)` |

### 6.3 `OrgGroup.vue` 改动(成员卡片 Badge 旁加自动分配小标)

```vue
<!-- 原 86-88 改为: -->
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

### 6.4 `Team.vue` 改动(Badge 旁加自动分配小标 + RoleChangeDialog prop)

**两处 Badge**(L143-145 / L249-251)同 §6.3 改法。

**调用 `RoleChangeDialog` 处**(L497-505 区间)追加 prop:
```vue
:current-role-auto-inferred="roleChangeTarget?.roleAutoInferred || false"
```

### 6.5 `RoleChangeDialog.vue` 改动(顶部 hint)

**新增 prop**(包含 `withDefaults` 默认值,保持与既有 prop 风格一致):
```ts
interface Props {
  // ... 既有
  /** 当前 role 是否由 HR 同步按 JPSTN_CD 自动推断(2026-06-16) */
  currentRoleAutoInferred?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  // ... 既有默认值
  currentRoleAutoInferred: false,   // 默认 false(不影响现有调用方)
});
```

**顶部 hint 区**(在 amber-50 警告框 `t('team.roleChange.warning')` 即 L273-275 下方)插入一段:
```vue
<div
  v-if="currentRoleAutoInferred"
  class="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800"
>
  <span class="font-medium">{{ $t('team.roleSource.autoInferred') }}:</span>
  {{ $t('team.roleSource.autoInferredHint') }}
</div>
```

### 6.6 `Settings.vue` 同步按钮 toast 文案(含 inferred 计数)

HR 同步按钮入口位于 `Settings.vue`(Team.vue 只有"修改角色"按钮,无 sync-hr 调用)。后端 `syncHrData` 现在返回 Map 含 `inferred` 计数(§5.5.1),需要在 `Settings.vue` 和 `services/api.ts` **两处** 配合修改:

**6.6.1 `frontend/src/services/api.ts` L461-464 扩展返回类型**(必须先做,否则 `npx vue-tsc` 因 `result.inferred` 不存在报错):

```ts
async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number; inferred: number }> {
  return request<{ inserted: number; updated: number; resigned: number; inferred: number }>('/users/sync-hr', {
    method: 'POST',
  });
}
```

**6.6.2 `frontend/src/views/Settings.vue` L545-565 同步回调修改**(项目惯例是用 `syncResult.value = { success, message }` 模板渲染,不是 `toast.success(...)`):

```ts
const syncResult = ref<{ success: boolean; message: string } | null>(null);

const handleSyncHr = async () => {
  // ... 既有 loading / 校验 ...
  try {
    const result = await apiService.syncHrUsers();   // 现在含 inferred
    const msg = `同步完成:新增 ${result.inserted} / 更新 ${result.updated} / 标记离职 ${result.resigned}` +
                (result.inferred > 0 ? ` / 职级自动推断 ${result.inferred} 人(被升级用户需重新登录)` : '');
    syncResult.value = { success: true, message: msg };
  } catch (e: any) {
    syncResult.value = { success: false, message: e?.message || '同步失败' };
  }
};
```

**渲染部分**(L234-243 既有 `<div v-if="syncResult !== null">` 与 `<p>{{ syncResult.message }}</p>`)无需改,新 msg 直接复用现有渲染。

## 7. 不在本次范围

- 推断的反向降级(JPSNT 从 BA → X1 时 dept-pm 自动降回 member)
- 把推断规则的 JPSTN 列表变成数据库可配置(目前写死在 `JpstnRoleMapping` 常量)
- `jpstn_cd_snapshot` 列(从 reason 字符串解析;若未来 reason 文案变化,需要新增列)
- 周报 / 加班等其他模块按 JPSTN 做的业务规则扩展
- 后端 `UserService` 之外其他 Service 对 JPSTN 的引用
- 前端 OrgGroup 之外的页面(Dashboard / Reports / etc.)展示"职级推断"Badge
- `frontend/src/i18n/locales/en.ts` 补建(项目当前只有 `zh.ts` 和 `ko.ts`,本 spec 不新建 `en.ts`,继续维持双语言现状;后续若要英文支持单独排期)

## 8. 边界场景

| # | 场景 | 预期行为 |
|---|------|---------|
| E1 | 用户首次 HR 同步,JPSNT=BA,role=member | 升级为 dept-pm,`managed_dept_codes=["<user.dept_code>"]`,`managed_company_cd=<user.company_cd>`,写 audit `changed_by='HR_SYNC'` |
| E2 | 用户首次 HR 同步,JPSNT=BF,role=member | 升级为 PM,`managed_project_ids='[]'`,写 audit |
| E3 | 用户首次 HR 同步,JPSNT 既非 BA 也非 BF,role=member | 不动 role(保持 member),audit 不写 |
| E4 | 用户已是 admin,再次 HR 同步,JPSNT=BA | 不动(启发式预填只覆盖 member) |
| E5 | 用户已是 dept-pm(被手工改过),再次 HR 同步,JPSNT=BF | 不动(已被手工覆盖) |
| E6 | 用户从前 JPSNT=BA → 现 JPSNT=X1(被调岗),role=dept-pm | 不动(启发式预填不降级) |
| E7 | 用户从前 JPSNT=null → 现 JPSNT=BA,role=member | 升级为 dept-pm(覆盖) |
| E8 | JPSNT=BA 但 `user.dept_code` 为空 | **跳过**(warn 日志),不升级 |
| E9 | JPSNT=BA 但 `user.company_cd` 为空 | **跳过**(warn 日志),不升级 |
| E10 | 离职 / 休职(`status='T'`)用户 | 不参与推断(由 `selectMdmActiveEmpNums` 自然过滤) |
| E11 | HR 同步后推断命中 100 个用户 | 全部 `token_version +1`,前端 100 个用户需重登录 |
| E12 | admin 手工改回 `member`(被推断后又改回) | 下次 HR 同步,JPSNT 仍是 BA,**会再次升级为 dept-pm**(启发式预填规则一致;若想"覆盖后不再被推断",需要更精细的状态机,本次 YAGNI) |
| E13 | E12 反向:admin 手工把 dept-pm 改成 admin,JPSNT=BA | 下次 HR 同步不动(启发式预填只覆盖 member) |
| E14 | 当前 sync 在推断路径中,第 N 条 update 失败(网络 / DB 抖动) | **整 `@Transactional` 回滚**:连同 `syncHrInsert` / `syncHrUpdate`,HR 重跑即可 |
| E15 | RoleChangeLog 表已存在但没 `managed_project_ids` 字段 | §4.1 SQL 加列后才生效,否则第 2 条 setter 字段会被丢弃 |

## 9. 风险与缓解

| # | 风险 | 严重度 | 缓解 |
|---|------|--------|------|
| R1 | `@Transactional` 内单条 catch,但其他语句失败时整事务回滚,导致前面已写入的推断也被回滚 | 中 | 接受"整事务原子"语义 —— 要么全部成功,要么全部回滚;HR 同步是低频管理动作,失败重跑即可 |
| R2 | `selectMdmActiveEmpNums` 把不在 mdm 中的老用户也排除,无法推断他们的 JPSNT | 低 | 符合预期:不在 mdm = 即将被 `syncHrMarkResigned` 标 T,推断无意义 |
| R3 | `SUBSTRING_INDEX(...)` 解析 reason,如果将来 reason 文案变了会失效 | 低 | reason 字符串约定:`"自动推断:JPSTN_CD=BA → 部门项目负责人"`;§5.5.2 已硬编码此格式;后续若改文案需同步改 SQL;或加列 `jpstn_cd_snapshot` 解决(本次 YAGNI) |
| R4 | 推断完成后,**所有被升级用户必须重新登录**才生效;若 admin 一次性同步 100 人,100 个在线用户会同时跳登录 | 中 | HR 同步是 admin 手动操作,可接受;前端 services/api.ts 已统一处理 401;在 Team.vue 同步按钮 toast 文案增加提示"被自动升级的用户需重新登录" |
| R5 | `selectLatestHrSyncInferences` 走 `MAX(id) GROUP BY user_id`,表大了后单 SQL 仍是 N 行扫表,但有 `idx_user_id` 索引,可控 | 低 | `sys_role_change_log` 表通常 < 万行,索引已存在(`add_role_management_v2.sql` L84),足够 |
| R6 | `fillRoleInferredMarkers` 覆盖 `getAllUsers` / `searchUsers` / `getUserById` / `getUsersByIds` 4 个入口,容易遗漏 | 低 | 集中在 `UserService` 4 个方法末尾调一次;后续 review 时检查 |
| R7 | Settings.vue 个人卡片里 JPSTN_CD 是 disabled 只读,但 admin 改 user role 时仍可能误以为"JPSTN 也会改",产生预期偏差 | 低 | §6.5 RoleChangeDialog 顶部 hint 已说明"手动改后不再自动覆盖";Settings 个人卡片不在 admin 改 role 流程里,无歧义 |
| R8 | `selectMdmActiveEmpNums` 在 `selectAdminIdsNotInMdm` 安全闸**之后**调用,意味着如果 admin 阻断,本步不执行 | 低 | 符合预期:admin 阻断意味着根本不该跑推断 |
| R9 | 推断只覆盖 member → PM/dept-pm;无法做"反向降级" | 中 | 显式规则;若要降级(BA → X1 时 dept-pm 降回 member)需要更复杂的状态机,本次 YAGNI;HR / admin 后续手工调 |
| R10 | `sys_role_change_log` 加 2 列后,`selectByUserId` 返参多 2 字段(向后兼容) | 低 | 返参是 `RoleChangeLog` 实体 JSON,字段为可空;前端按 key 读取(`h.oldManagedProjectIds`)不会因字段缺失报错,新字段默认 undefined;若外部系统按位置解析(无 key 名)才会出问题,本项目无此调用方 |

## 10. 测试策略

### 10.1 单元测试(`backend/src/test/`,新增)

| 测试类 | 覆盖范围 |
|--------|---------|
| `JpstnRoleMappingTest` | `inferRoleCode('BA') → 'dept-project-manager'`;`inferRoleCode('BF') → 'project-manager'`;`inferRoleCode(null / 'X1' / '') → null` |
| `UserServiceJpstnInferenceTest` | 场景 E1/E3/E4/E5/E7/E8/E9:mock UserMapper / RoleChangeLogMapper,断言 `updateRoleAndScope` 入参 + `roleChangeLogMapper.insert` 入参(尤其 `changedBy='HR_SYNC'` 与 `reason`) |
| `UserServiceFillMarkersTest` | `fillRoleInferredMarkers`:mock `selectLatestHrSyncInferences` 返 2 行,断言 User.roleAutoInferred / roleInferredFromJpstn 填充正确;空 list 不报错 |

### 10.2 集成 / 手工验收

| 验收步骤 | 期望 |
|---------|------|
| 1. 在 `mdm_if_pa_a` 临时把 C0000001 的 `JPSTN_CD='BA'` | |
| 2. admin 调 `POST /api/users/sync-hr` | 返回 `{inserted:0, updated:N, resigned:0, inferred:>=1}` |
| 3. 查 `SELECT role, managed_dept_codes, managed_company_cd, token_version FROM sys_user WHERE id='C0000001'`,再与改动前 snapshot 对比 token_version 字段,确认 +1 | `role='dept-project-manager'`,`managed_dept_codes=["<原 dept_code>"]`,`managed_company_cd=<原 company_cd>`,`token_version` = 改动前值 + 1 |
| 4. 查 `SELECT * FROM sys_role_change_log WHERE user_id='C0000001' ORDER BY changed_at DESC LIMIT 1` | `changed_by='HR_SYNC'`,`reason='自动推断:JPSTN_CD=BA → 部门项目负责人'` |
| 5. C0000001 用旧 JWT 调任意接口 | 401(被踢下线) |
| 6. C0000001 重新登录 | 拿到新 role 权限 |
| 7. Team 页面看 C0000001 卡片 | role Badge 旁有"职级推断"小 Badge,hover 提示"来源:BA(部门项目负责人)" |
| 8. 打开 C0000001 的 RoleChangeDialog | 顶部蓝色 hint 出现 |
| 9. 再次 `sync-hr`(JPSNT 没变) | 不重复推断(role 已不是 member),`inferred=0` |
| 10. admin 手工把 C0000001 改回 `member` | 生效 |
| 11. 再次 `sync-hr`(JPSNT 仍是 BA) | 重新推断升级(规则一致),`inferred>=1` |

## 11. 实施步骤

| # | 步骤 | 风险等级 |
|---|------|---------|
| 1 | 写 `backend/add_role_change_log_project_ids.sql`,跑(用户手动) | 低,加列 INPLACE |
| 2 | 改 `RoleChangeLog.java` + `RoleChangeLogMapper.xml` 加 2 字段 | 低 |
| 3 | 新建 `backend/src/main/java/com/wbs/project/util/JpstnRoleMapping.java` | 0 风险 |
| 4 | 改 `UserMapper.xml`:加 `selectMdmActiveEmpNums` + `selectLatestHrSyncInferences` 2 个 select | 低 |
| 5 | 改 `UserMapper.java`:加 2 个接口方法 | 低 |
| 6 | 改 `User.java`:加 2 个 `@Transient` 字段 | 低 |
| 7 | 改 `UserService.java`:加 `inferRoleFromJpstnForHrSync()` + `fillRoleInferredMarkers()`;`syncHrData` 步 ④ 调用前者;4 个返 User 入口末尾调后者;顺手补 `changeUserRole` 加 2 行 setter | 中(核心) |
| 8 | 跑 `mvn clean install -DskipTests` 编译 | 校验 |
| 9 | 跑 `mvn -pl . test -DfailIfNoTests=false`(若 §10.1 单测已加,必跑;否则可跳过) | 校验 |
| 10 | 改 `types/index.ts`:User 加 2 字段 | 低 |
| 11 | 改 `frontend/src/i18n/locales/zh.ts` + `ko.ts`(en.ts 不在本次范围):加 4 个 key | 低 |
| 12 | 改 `OrgGroup.vue` + `Team.vue`:Badge 旁加"职级推断"小 Badge + hover title | 低 |
| 13 | 改 `RoleChangeDialog.vue`:加 `currentRoleAutoInferred` prop + 顶部蓝色 hint;`Team.vue` 调用处追加 prop | 低 |
| 14a | 改 `frontend/src/services/api.ts` L461-464:扩展 `syncHrUsers` 返回类型加 `inferred: number` 字段 | 低 |
| 14b | 改 `Settings.vue` 同步回调 + 渲染:用 `syncResult.value = { success, message }` 赋值,message 含 inferred 计数 | 低 |
| 15 | 跑 `npx vue-tsc` + `npm run build` | 校验 |
| 16 | 跑一次 `sync-hr`,按 §10.2 验收 1-11 步 | 验收 |