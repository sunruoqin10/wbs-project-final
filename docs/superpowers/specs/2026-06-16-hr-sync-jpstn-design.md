# HR 同步扩展:JPSTN_CD / JPSTN_NAM(职级) — 设计

> 日期:2026-06-16
> 状态:草案(待实施)
> 范围:后端 User entity / UserMapper / `add_user_jpstn_columns.sql` + 前端 User type / Settings.vue / i18n

## 1. 背景与目标

设置页面的人事信息同步(`syncHrData`)当前从 `mdm_if_pa_a` 拉取的字段不含**职级**,而职级是人事的核心维度之一,本系统目前完全没有记录。

`mdm_if_pa_a` 表已存在的相关字段:

| 源字段 | 含义 | 目标列 |
|--------|------|--------|
| `JPSTN_CD` | 职级 code | `sys_user.jpstn_cd` |
| `JPSTN_NAM` | 职级名 | `sys_user.jpstn_nam` |

`sys_user` 当前**没有**这两列,需要补加。

## 2. 数据库变更

新建 `backend/add_user_jpstn_columns.sql`(命名对齐 `add_user_status_column.sql`):

```sql
-- ===============================================================
-- 2026-06-16: sys_user 增加 jpstn_cd / jpstn_nam(HR 同步职级)
-- 来源 spec: docs/superpowers/specs/2026-06-16-hr-sync-jpstn-design.md
-- ===============================================================
ALTER TABLE sys_user
  ADD COLUMN jpstn_cd VARCHAR(50) DEFAULT NULL
    COMMENT '职级 code(来自 mdm_if_pa_a.JPSTN_CD)' AFTER company_cd,
  ADD COLUMN jpstn_nam VARCHAR(100) DEFAULT NULL
    COMMENT '职级名(来自 mdm_if_pa_a.JPSTN_NAM)' AFTER jpstn_cd,
  ALGORITHM=INPLACE, LOCK=NONE;
```

**位置选择**:`company_cd` 之后,与现有 HR 字段(`dept_code` / `sub_org_*` / `company_cd` / `chinese_nam`)聚簇。

**INPLACE / LOCK=NONE**:与 `add_user_status_column.sql` 一致,避免热路径卡锁。

## 3. 后端改动

### 3.1 `User.java` entity

在 HR 扩展字段段(`chinese_nam` 之后)新增:

```java
// === HR 同步扩展字段(2026-06-16 新增,职级) ===
private String jpstnCd;    // 职级 code(来自 mdm_if_pa_a.JPSTN_CD)
private String jpstnNam;   // 职级名(来自 mdm_if_pa_a.JPSTN_NAM)
```

### 3.2 `UserMapper.xml`

**3.2.1 resultMap**(L6-28 区间内 `sub_org_nam` / `company_cd` 之后):
```xml
<result column="jpstn_cd"  property="jpstnCd"/>
<result column="jpstn_nam" property="jpstnNam"/>
```

**3.2.2 Base_Column_List**:`company_cd` 之后加 `jpstn_cd, jpstn_nam`。

**3.2.3 syncHrInsert**(L164-188):在 `IFNULL(p.COMPANY_CD, '')` 之后补:
```xml
IFNULL(p.JPSTN_CD, ''),
IFNULL(p.JPSTN_NAM, ''),
```

**3.2.4 syncHrUpdate**(L191-209):在 `u.company_cd = ...` 之后补:
```xml
u.jpstn_cd  = IFNULL(p.JPSTN_CD, ''),
u.jpstn_nam = IFNULL(p.JPSTN_NAM, ''),
```

**3.2.5 syncHrMarkResigned / selectAdminIdsNotInMdm**:不需要改,只动 status。

**3.2.6 通用 `update` 段(L113-125)**:不加 `jpstn_*`,**保持只读语义** — 职级是 HR 控制字段,不允许业务侧覆盖;`update` 只放行业务自身字段(name/email/avatar/department/skills/password)。与现有 `chinese_nam` 同样不进 `update` 的设计一致。

## 4. 前端改动

### 4.1 `User` type(types/index.ts L12-23 区间)

```ts
chineseNam?: string;
// === HR 同步扩展字段(2026-06-16,职级) ===
jpstnCd?: string;
jpstnNam?: string;
status?: 'C' | 'H' | 'T';
```

### 4.2 `Settings.vue` 个人信息卡片

在 `department` 字段之后加 2 个**只读**展示项(HR 数据,不允许用户自己改):

```vue
<Input :label="t('settings.profile.jpstnCd')" :modelValue="currentUser?.jpstnCd || '-'" disabled />
<Input :label="t('settings.profile.jpstnNam')" :modelValue="currentUser?.jpstnNam || '-'" disabled />
```

**为什么 disabled 而非可编辑**:职级是 HR 数据源(`mdm_if_pa_a`)单向回流,前端如果开放编辑会和下次 HR 同步冲突,沿用 `chinese_nam` / `deptCode` 不在表单的"隐式只读"惯例。

### 4.3 i18n(`zh.ts` / `ko.ts` / `en.ts`)

`settings.profile` 段新增:
```ts
jpstnCd: '职级编码',
jpstnNam: '职级名称',
```

(同步 `ko.ts` / `en.ts`)

### 4.4 `User.parseUserData` 不需要改

新增字段是 plain string,无需 JSON 解析。

## 5. 行为对比

| 场景 | 改前 | 改后 |
|------|------|------|
| 首次 HR 同步:mdm 中有 JPSTN | sys_user 没有这两个字段 | sys_user.jpstn_cd / jpstn_nam 写入 |
| 已存在用户再次同步 | JPSTN 不会被刷新(字段不存在) | 每次 update 时按 mdm 覆盖 |
| 用户在 Settings 改职级 | (前端无 UI) | disabled,无法改 |
| `selectAll` / `selectById` 等查询 | 返回 User 不含 JPSTN | 返回 User 包含 JPSTN(透传前端) |

## 6. 不在本次范围

- 职级在权限 / 审批链路上的应用(暂不按职级分配权限)
- 历史数据的回填(只对同步后才生效;若需旧数据回填可手动重跑同步)
- `Team.vue` 等其他页面的职级展示(本次仅 Settings)
- 离职 / 休职记录的职级清除(同步 update 不清,保持最后一次在职值)

## 7. 实施步骤

1. 写 `backend/add_user_jpstn_columns.sql` 并手动跑(用户执行)
2. 改 `User.java` 加字段
3. 改 `UserMapper.xml` 4 处(resultMap / Base_Column_List / syncHrInsert / syncHrUpdate)
4. 改前端 `User` type
5. 改 `Settings.vue` 加 2 个只读 input
6. 改 3 个 i18n 文件加 2 个 key
7. 跑 `mvn clean compile` + `npx vue-tsc` 校验
8. 跑一次 `syncHrData` 验证 JPSTN 落地

## 8. 风险

| 风险 | 缓解 |
|------|------|
| 旧列加锁 | `ALGORITHM=INPLACE, LOCK=NONE` 已在 SQL 中声明 |
| mdm_if_pa_a 字段类型不匹配 | JPSTN_CD / JPSTN_NAM 按字符串处理,VARCHAR(50/100) 留足缓冲 |
| i18n 缺 key 致前端显示 raw key | 三个语言文件都加;若某语言暂不需要翻译,留 fallback 中文 |
