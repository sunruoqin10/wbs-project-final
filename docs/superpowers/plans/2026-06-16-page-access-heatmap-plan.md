# 页面访问率热力图 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 admin 提供"页面 × 24 小时"访问率热力图(数据按"日均"归一化,窗口可切 1d/7d/30d/90d),后端新增 `sys_page_view` 表 + 写/读 controller,前端在 router 钩子里同步上报,admin 路由 `/admin/access-heatmap` 展示 4 KPI 卡 + ECharts heatmap。

**Architecture:**
- 写入:前端 `router.afterEach` → `POST /api/page-views` → `sys_page_view` 表(失败静默)
- 读取:`GET /api/admin/page-views/heatmap?window=Nd` → SQL 聚合 → `HeatmapResponse { kpi, xAxis[0..23], yAxis[pageName], matrix[yAxis][24] }`
- 鉴权:`AuthInterceptor` 在 `/api/admin/**` 路径前缀强制 role=admin
- 校验:PageView entity 用 `@JsonFormat("yyyy-MM-dd'T'HH:mm:ss")` 直接反序列化;controller 校验 pagePath / pageName / occurredAt

**Tech Stack:**
- Backend: Spring Boot 3.2.0 + MyBatis + MySQL 8 + Lombok + Jackson(JSR-310)
- Frontend: Vue 3 + TS + Vite + ECharts 5 + Pinia + Tailwind + vue-i18n
- Test: JUnit 5 + Mockito(后端);前端无 test runner,靠 `vue-tsc` + `npm run build` + 手动验收

**前置文档:** `docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md`

---

## 重要执行约定(给执行者)

1. **每一步都要跑验证命令并看实际输出**,不要凭"应该对了"放行
2. **绝不要自动 git commit / push** — 项目规则 + 用户偏好。**每到一个"commit"步骤,先展示 commit message + 文件列表给用户,等用户明确说"提交"再执行**
3. **不要 `mvn spring-boot:run` / `npm run dev`** — 让用户自己启服务做手动验收
4. **所有路径用 Unix 风格 + 前向斜杠**(Windows bash 环境)
5. 后端修改后跑 `mvn -pl . test -DfailIfNoTests=false` 确认不破坏现有
6. 前端修改后跑 `npx vue-tsc` + `npm run build` 验证
7. 每个 task 内部步骤走"修改 → 验证 → 提交(等批准)"三段式

---

## Phase 1: 数据库

### Task 1: 创建 `sys_page_view` 表

**Files:**
- Create: `backend/add_page_view_table.sql`

- [ ] **Step 1: 写 SQL 文件**

完整内容(直接复制自 spec §4.1):

```sql
-- ===============================================================
-- 2026-06-16: 新增 sys_page_view 表(供 admin 访问率热力图)
-- 来源 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md
-- 范围: 记录每次前端路由切换的访问日志
-- ⚠️ 必须先执行本 SQL 再启动服务
-- ===============================================================
CREATE TABLE IF NOT EXISTS sys_page_view (
    id           BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    user_id      VARCHAR(20)  NOT NULL                        COMMENT '访问者 userId(C0000001 格式)',
    page_path    VARCHAR(255) NOT NULL                        COMMENT '前端路由路径,例 /projects/123',
    page_name    VARCHAR(100) NOT NULL                        COMMENT '路由 name 字段,例 ProjectDetail(便于 GROUP BY)',
    occurred_at  DATETIME     NOT NULL                        COMMENT '访问发生时间(精确到秒)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    INDEX idx_pv_occurred_at (occurred_at),
    INDEX idx_pv_page_occurred (page_name, occurred_at),
    INDEX idx_pv_user_occurred (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志';
```

- [ ] **Step 2: 本地 MySQL 跑一次,验证表结构 + 3 个索引齐全**

```bash
mysql -uroot -p db_webwbs < backend/add_page_view_table.sql
mysql -uroot -p db_webwbs -e "SHOW CREATE TABLE sys_page_view\G" | head -30
mysql -uroot -p db_webwbs -e "SHOW INDEX FROM sys_page_view" | awk '{print $3}' | sort -u
```

Expected: 第 1 条命令无输出 = 成功;第 2 条打印完整表结构(含 3 索引名);第 3 条输出含 `idx_pv_occurred_at` / `idx_pv_page_occurred` / `idx_pv_user_occurred` / `PRIMARY`。

**提交节点**:此 task 完成后,**单独提交 SQL**。先给用户看 commit message:

```
feat(db): 新增 sys_page_view 表(访问率热力图)

- 1 张表 + 3 索引(occurred_at / page_name+occurred_at / user_id+occurred_at)
- 文件: backend/add_page_view_table.sql
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md
```

等用户说"提交"再 `git add` + `git commit`(**绝不**自己执行)。

---

## Phase 2: 后端 - 实体 + Mapper + DTO

### Task 2: 创建 PageView entity

**Files:**
- Create: `backend/src/main/java/com/wbs/project/entity/PageView.java`

- [ ] **Step 1: 写文件**

直接复制自 spec §5.1:

```java
package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PageView {
    private Long id;
    private String userId;
    private String pagePath;
    private String pageName;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 验证编译通过**

```bash
cd backend && mvn compile -DskipTests -q
```

Expected: 无错误,BUILD SUCCESS。

**等用户批准后 commit**(本 task 单独,或与下一个 task 一起,看用户偏好)。

### Task 3: 创建 PageViewMapper 接口 + XML

**Files:**
- Create: `backend/src/main/java/com/wbs/project/mapper/PageViewMapper.java`
- Create: `backend/src/main/resources/mapper/PageViewMapper.xml`

- [ ] **Step 1: 写 Java 接口** — 直接复制自 spec §5.2
- [ ] **Step 2: 写 XML** — 直接复制自 spec §5.3
- [ ] **Step 3: 验证编译**

```bash
cd backend && mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS。

### Task 4: 创建 HeatmapResponse DTO

**Files:**
- Create: `backend/src/main/java/com/wbs/project/dto/HeatmapResponse.java`

- [ ] **Step 1: 写文件** — 直接复制自 spec §5.4(注意是 `com.wbs.project.dto` 包,**确认该包已存在**,不存在就 `mkdir -p backend/src/main/java/com/wbs/project/dto`)
- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -DskipTests -q
```

**提交节点**:Task 2-4 可以合并一次提交,commit message:

```
feat(backend): 新增 PageView 实体 / Mapper / HeatmapResponse DTO

- entity/PageView.java
- mapper/PageViewMapper.java + mapper/PageViewMapper.xml
- dto/HeatmapResponse.java
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md
```

---

## Phase 3: 后端 - Service(TDD)

> 项目现状:后端 `src/test/` 已有零星测试骨架(UserServiceFillMarkersTest 等已存在,见 Explore 结果)。JUnit 5 + Mockito 可用,`@DataJpaTest` / H2 不用。

### Task 5: 写 PageViewServiceTest(失败版)

**Files:**
- Create: `backend/src/test/java/com/wbs/project/service/PageViewServiceTest.java`

- [ ] **Step 1: 写测试**(直接复制)

```java
package com.wbs.project.service;

import com.wbs.project.dto.HeatmapResponse;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.PageViewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageViewServiceTest {

    @Mock
    private PageViewMapper pageViewMapper;

    @InjectMocks
    private PageViewService pageViewService;

    // === 边界 1: window 非法 ===
    @Test
    void getHeatmap_invalidWindow_throws() {
        assertThrows(BusinessException.class,
            () -> pageViewService.getHeatmap("2d"));
        assertThrows(BusinessException.class,
            () -> pageViewService.getHeatmap(""));
        assertThrows(BusinessException.class,
            () -> pageViewService.getHeatmap(null));
    }

    // === 边界 2: 1d 窗口空数据 ===
    @Test
    void getHeatmap_emptyData_returnsEmptyMatrix() {
        when(pageViewMapper.aggregateByPageAndHour(any(), any()))
            .thenReturn(List.of());
        when(pageViewMapper.aggregateByPage(any(), any()))
            .thenReturn(List.of());

        HeatmapResponse r = pageViewService.getHeatmap("1d");

        assertEquals("1d", r.getWindow());
        assertEquals(1, r.getDays());
        assertEquals(24, r.getXAxis().size());
        assertTrue(r.getYAxis().isEmpty());
        assertTrue(r.getMatrix().isEmpty());
        assertEquals(0L, r.getKpi().getTotalVisits());
        assertNull(r.getKpi().getTopPageName());
    }

    // === 边界 3: 1d 窗口有数据(单页单小时) ===
    @Test
    void getHeatmap_singlePageSingleHour_normalizesByDays() {
        // 模拟: 1d 窗口,Dashboard 在 14 点有 5 次访问
        when(pageViewMapper.aggregateByPageAndHour(any(), any()))
            .thenReturn(List.of(Map.of(
                "pageName", "Dashboard",
                "hour", 14,
                "pv", 5L
            )));
        when(pageViewMapper.aggregateByPage(any(), any()))
            .thenReturn(List.of(Map.of(
                "pageName", "Dashboard",
                "totalVisits", 5L
            )));

        HeatmapResponse r = pageViewService.getHeatmap("1d");

        assertEquals(5L, r.getKpi().getTotalVisits());
        assertEquals("Dashboard", r.getKpi().getTopPageName());
        assertEquals(5.0, r.getKpi().getTopPageAvg());  // 1d 日均=原值
        assertEquals(Integer.valueOf(14), r.getKpi().getPeakHour());

        // matrix[Dashboard 行][14 列] = 5.0
        assertEquals(1, r.getMatrix().size());
        assertEquals(24, r.getMatrix().get(0).size());
        assertEquals(5.0, r.getMatrix().get(0).get(14));
        assertEquals(0.0, r.getMatrix().get(0).get(0));  // 其他小时=0
    }

    // === 边界 4: 7d 窗口归一化(同 1d 数据,日均应为原值 1/7) ===
    @Test
    void getHeatmap_7d_normalizesBySeven() {
        when(pageViewMapper.aggregateByPageAndHour(any(), any()))
            .thenReturn(List.of(Map.of(
                "pageName", "Dashboard",
                "hour", 14,
                "pv", 7L  // 7d 共 7 次
            )));
        when(pageViewMapper.aggregateByPage(any(), any()))
            .thenReturn(List.of(Map.of(
                "pageName", "Dashboard",
                "totalVisits", 7L
            )));

        HeatmapResponse r = pageViewService.getHeatmap("7d");

        assertEquals(7, r.getDays());
        assertEquals(7L, r.getKpi().getTotalVisits());
        assertEquals(1.0, r.getKpi().getDailyAvg());   // 7/7=1.0
        assertEquals(1.0, r.getKpi().getTopPageAvg()); // 7/7=1.0
        assertEquals(1.0, r.getMatrix().get(0).get(14));
    }

    // === 边界 5: 多页 peakHour 取加和最大的小时 ===
    @Test
    void getHeatmap_peakHour_sumsAcrossPages() {
        // Dashboard 14 点 5 次,Projects 14 点 3 次,Projects 10 点 20 次
        when(pageViewMapper.aggregateByPageAndHour(any(), any()))
            .thenReturn(List.of(
                Map.of("pageName", "Dashboard", "hour", 14, "pv", 5L),
                Map.of("pageName", "Projects",  "hour", 14, "pv", 3L),
                Map.of("pageName", "Projects",  "hour", 10, "pv", 20L)
            ));
        when(pageViewMapper.aggregateByPage(any(), any()))
            .thenReturn(List.of(
                Map.of("pageName", "Dashboard", "totalVisits", 5L),
                Map.of("pageName", "Projects",  "totalVisits", 23L)
            ));

        HeatmapResponse r = pageViewService.getHeatmap("1d");

        // 14 点加和 = 5+3 = 8;10 点加和 = 20;20 > 8,peakHour=10
        assertEquals(Integer.valueOf(10), r.getKpi().getPeakHour());
        // 页面按总 PV 倒序:Projects(23) 在前
        assertEquals(2, r.getYAxis().size());
        assertEquals("Projects", r.getYAxis().get(0));
        assertEquals("Dashboard", r.getYAxis().get(1));
    }
}
```

- [ ] **Step 2: 跑测试,确认编译失败(因 PageViewService 还不存在)**

```bash
cd backend && mvn -pl . test -Dtest=PageViewServiceTest -DfailIfNoTests=false -q 2>&1 | tail -20
```

Expected: 编译错误 "cannot find symbol: class PageViewService" 或类似。**这是预期失败,继续下一步**。

### Task 6: 写 PageViewService 实现

**Files:**
- Create: `backend/src/main/java/com/wbs/project/service/PageViewService.java`

- [ ] **Step 1: 写文件** — 直接复制自 spec §5.5(完整 ~150 行)
- [ ] **Step 2: 跑测试,确认通过**

```bash
cd backend && mvn -pl . test -Dtest=PageViewServiceTest -DfailIfNoTests=false -q 2>&1 | tail -20
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`,BUILD SUCCESS。

- [ ] **Step 3: 跑全量测试,确认不破坏现有**

```bash
cd backend && mvn -pl . test -DfailIfNoTests=false -q 2>&1 | tail -20
```

Expected: 所有现有测试 + 新增 5 个全通过。

**提交节点**:Task 5-6 一起提交,commit message:

```
feat(backend): PageViewService + 单测(访问率热力图读路径)

- service/PageViewService.java
  - getHeatmap(window):SQL 聚合 + KPI 计算 + 矩阵拼装
  - record():简单入库
- test/service/PageViewServiceTest.java: 5 个用例覆盖 window 校验 / 空数据 / 1d / 7d 归一化 / peakHour
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md §5.5
```

---

## Phase 4: 后端 - Controllers + Interceptor

### Task 7: 创建 PageViewController(写)

**Files:**
- Create: `backend/src/main/java/com/wbs/project/controller/PageViewController.java`

- [ ] **Step 1: 写文件** — 直接复制自 spec §5.6
- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS。

### Task 8: 创建 AdminPageViewController(读)

**Files:**
- Create: `backend/src/main/java/com/wbs/project/controller/AdminPageViewController.java`

- [ ] **Step 1: 写文件** — 直接复制自 spec §5.7
- [ ] **Step 2: 验证编译** — 同上

### Task 9: 改 AuthInterceptor 加 admin 路径检查

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/interceptor/AuthInterceptor.java`

- [ ] **Step 1: 阅读现有 AuthInterceptor**,定位 preHandle 末尾(找 return true 或 return false 之前的位置)

```bash
cd backend && cat src/main/java/com/wbs/project/interceptor/AuthInterceptor.java
```

- [ ] **Step 2: 在 preHandle 末尾、return true 之前插入 admin 路径检查**

定位原则:在 `request.setAttribute("userRole", role);` 这类 attribute 写入之后,return true 之前插入。

插入代码(spec §5.8):

```java
if (request.getRequestURI().startsWith("/api/admin/")) {
    if (!"admin".equals(role)) {
        throw new com.wbs.project.exception.BusinessException(403, "需要管理员权限");
    }
}
```

> 注意:`throw BusinessException` 后 **不** 写 return,让异常冒泡到 GlobalExceptionHandler。
> 如果现有文件已经 import 了 `BusinessException`,直接用类名;否则用全限定名(实施时根据文件实际 import 调整)。

- [ ] **Step 3: 验证编译 + 跑全量测试**

```bash
cd backend && mvn clean install -DskipTests -q 2>&1 | tail -10
cd backend && mvn -pl . test -DfailIfNoTests=false -q 2>&1 | tail -10
```

Expected: 编译 BUILD SUCCESS;测试全部通过(没有新单测覆盖 AuthInterceptor,因为本设计 YAGNI 不引入 controller 集成测试)。

**提交节点**:Task 7-9 一起提交,commit message:

```
feat(backend): PageView 写/读 Controller + AuthInterceptor admin 检查

- controller/PageViewController.java:POST /api/page-views
- controller/AdminPageViewController.java:GET /api/admin/page-views/heatmap
- interceptor/AuthInterceptor.java:/api/admin/** 强制 role=admin
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md §5.6-5.8
```

---

## Phase 5: 前端 - Types + API 客户端

### Task 10: 加 HeatmapResponse 类型

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1: 阅读现有 types/index.ts 末尾结构**

```bash
tail -20 frontend/src/types/index.ts
```

- [ ] **Step 2: 在文件末尾追加**(直接复制自 spec §6.4)

```ts
// === 访问率热力图(2026-06-16) ===

export interface HeatmapKpi {
  totalVisits: number;
  dailyAvg: number;
  topPageName: string | null;
  topPageAvg: number | null;
  peakHour: number;
  peakHourAvg: number;
}

export interface HeatmapResponse {
  window: '1d' | '7d' | '30d' | '90d';
  fromTs: string;
  toTs: string;
  days: number;
  kpi: HeatmapKpi;
  xAxis: number[];
  yAxis: string[];
  matrix: number[][];
  maxValue: number;
}
```

- [ ] **Step 3: 验证类型检查**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

Expected: 编译成功(其他文件尚未引用此类型,所以不会报错)。

### Task 11: services/api.ts 加 postPageView + getHeatmap

**Files:**
- Modify: `frontend/src/services/api.ts`

- [ ] **Step 1: 定位 ApiService 类末尾**

```bash
grep -n "^}" frontend/src/services/api.ts | tail -5
```

找到类结束 `}` 的行号(在 `export const apiService = new ApiService();` 之前)。

- [ ] **Step 2: 在类结束 `}` 之前插入 2 个方法**

插入内容(spec §6.3):

```ts
  // === Page View API(2026-06-16 访问率热力图) ===

  async postPageView(payload: {
    pagePath: string;
    pageName: string;
    occurredAt: string;
  }): Promise<void> {
    return request<void>('/page-views', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  async getHeatmap(window: '1d' | '7d' | '30d' | '90d'): Promise<HeatmapResponse> {
    return request<HeatmapResponse>(`/admin/page-views/heatmap?window=${window}`);
  }
```

- [ ] **Step 3: 顶部 import 加 `HeatmapResponse`**

定位 import 段(第 3 行附近),在 `import type { ... } from '@/types';` 末尾加 `HeatmapResponse` 或 `HeatmapKpi`:

修改前:
```ts
import type { Project, Task, User, DelayStats, ... } from '@/types';
```

修改后(在 `WeeklyReportApprovalLog,` 之后追加 `HeatmapResponse, HeatmapKpi`):
```ts
import type { Project, Task, User, DelayStats, ..., WeeklyReportApprovalLog, HeatmapResponse, HeatmapKpi, Document, OrgNode, ... } from '@/types';
```

具体插入位置以现有 import 行的实际内容为准。

- [ ] **Step 4: 验证类型检查**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

Expected: 编译成功。

**提交节点**:Task 10-11 一起提交,commit message:

```
feat(frontend): HeatmapResponse 类型 + postPageView/getHeatmap API 方法

- types/index.ts:HeatmapResponse / HeatmapKpi 接口
- services/api.ts:apiService.postPageView / apiService.getHeatmap
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md §6.3-6.4
```

---

## Phase 6: 前端 - Composable + View + Routing + Sidebar

### Task 12: 新建 useAccessLog composable

**Files:**
- Create: `frontend/src/composables/useAccessLog.ts`

- [ ] **Step 1: 写文件** — 直接复制自 spec §6.1

### Task 13: App.vue 接入 useAccessLog

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: 在 `<script setup>` 末尾追加 2 行**

修改后:

```ts
<script setup lang="ts">
import { onMounted } from 'vue';
import { useUserStore } from './stores/user';
import { useAccessLog } from './composables/useAccessLog';

const userStore = useUserStore();

onMounted(async () => {
  // 恢复认证信息并加载用户数据
  await userStore.restoreAuth();
  await userStore.loadUsers();

  // 启动访问日志监听
  useAccessLog();

  console.log('App initialized, current user:', userStore.currentUser);
});
</script>
```

> 注意:`useAccessLog()` 在 onMounted 内调用而非 setup 顶层,确保 router 已 ready。
>
> **onMounted 链路说明**:`useAccessLog()` 内部又注册了自己的 `onMounted(install)`。`onMounted` 在 setup 阶段注册到当前激活组件实例,这里在 `App.vue` 的 `onMounted` 回调里调用 `useAccessLog()`,等价于在 App 的 setup 中(运行时)调用 — Vue 会把内部的 `onMounted` 注册到 App 这个实例上。链路:**App 挂载 → onMounted 触发 → useAccessLog() → install() → router.afterEach 装上**。

- [ ] **Step 2: 验证类型检查**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

Expected: 编译成功。

### Task 14: 新建 AccessHeatmapView.vue(主页面)

**Files:**
- Create: `frontend/src/views/AccessHeatmapView.vue`

- [ ] **Step 1: 写文件** — 直接复制自 spec §6.5 完整内容(约 250 行)
- [ ] **Step 2: 验证类型检查**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

Expected: 编译成功。

- [ ] **Step 3(决策点):KpiCard.vue 是否存在**

```bash
ls frontend/src/components/common/KpiCard.vue 2>&1
```

- 若存在 → 继续用 `<KpiCard>` 引用,无变化
- 若不存在 → 把 view 文件里的 4 个 `<KpiCard>` 全部 inline 化:用 `<div class="rounded-lg border border-border bg-surface p-4 shadow-sm"><p class="text-sm text-text-muted">title</p><p class="text-2xl font-semibold mt-1">value</p><p v-if="subtitle" class="text-xs text-text-muted mt-1">subtitle</p></div>` 替换每个 `<KpiCard>`,并删除文件顶部的 `import KpiCard from '@/components/common/KpiCard.vue'` 一行

- [ ] **Step 4: 重新跑类型检查确认 inline 后无错**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

### Task 15: router/index.ts 加新路由

**Files:**
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: 定位现有 admin-only 路由(scheduler-management)**

```bash
grep -n "SchedulerManagement\|scheduler-management" frontend/src/router/index.ts
```

- [ ] **Step 2: 在 scheduler-management 路由后插入新路由**

```ts
  {
    path: '/admin/access-heatmap',
    name: 'AdminAccessHeatmap',
    component: () => import('@/views/AccessHeatmapView.vue'),
    meta: {
      permission: 'admin',
      title: 'routes.adminAccessHeatmap',
    },
  },
```

### Task 16: Sidebar.vue 加菜单项

**Files:**
- Modify: `frontend/src/components/layout/Sidebar.vue`

- [ ] **Step 1: 定位 admin-only 段(L134-139 附近)**

```bash
grep -n "scheduler-management\|currentRole === 'admin'" frontend/src/components/layout/Sidebar.vue
```

- [ ] **Step 2: 在 admin-only 段内,scheduler-management 项前插入新项**

修改前(参考):
```ts
...(permissionStore.currentRole === 'admin' ? [
  { key: 'scheduler-management', label: 'sidebar.schedulerManagement', icon: '...', path: '/scheduler-management' },
] : []),
```

修改后:
```ts
...(permissionStore.currentRole === 'admin' ? [
  { key: 'admin-access-heatmap', label: 'sidebar.adminAccessHeatmap', icon: '...', path: '/admin/access-heatmap' },
  { key: 'scheduler-management', label: 'sidebar.schedulerManagement', icon: '...', path: '/scheduler-management' },
] : []),
```

> **icon 字段**(必须做):打开 `frontend/src/components/layout/Sidebar.vue`,**直接复制** scheduler-management 项的 `icon` 字段字符串(它是一长串 SVG path / data URI),粘贴到新项的 icon 字段。**不要写 `'...'`,vue-tsc 会过但 UI 会显示空白**。
> 
> 实施时也可以临时把 scheduler-management 的 icon 改名为"icon 1"复用,然后给新项用同样的"icon 1"引用,或者两个用同一个 icon 字符串(看现有数据结构怎么引用)。

- [ ] **Step 3: 验证类型检查 + 构建**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
cd frontend && npm run build 2>&1 | tail -15
```

Expected: 编译 + 构建都通过。

**提交节点**:Task 12-16 一起提交,commit message:

```
feat(frontend): 访问率热力图页面 + 路由 + 菜单 + 上报 composable

- composables/useAccessLog.ts:router.afterEach 同步上报
- App.vue:onMounted 启动上报
- views/AccessHeatmapView.vue:ECharts heatmap + 4 KPI 卡
- router/index.ts:/admin/access-heatmap
- components/layout/Sidebar.vue:admin-only 菜单项
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md §6.1,6.5-6.7
```

注意:此时 i18n keys 还没加,Sidebar 会显示原始 key 字符串(例如 "sidebar.adminAccessHeatmap")。**这是预期**,下一步加 i18n 后正常。

---

## Phase 7: i18n

### Task 17: 加 zh.ts keys

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`

- [ ] **Step 1: 检查现有 `admin:` 命名空间是否存在**

```bash
grep -n "^  admin:\|^admin:" frontend/src/i18n/locales/zh.ts
```

- 若存在(看到 `admin: { ... }` 整段)→ 把 `accessHeatmap` 段插入到 `admin` 内的合适位置(其他子键之后,`},` 闭合之前)
- 若不存在 → 在文件末尾、`};` 闭合之前新增 `admin: { accessHeatmap: { ... } },` 整段(spec §6.8.1)

- [ ] **Step 2: 定位 sidebar / routes 段**

```bash
grep -n "sidebar:\|routes:" frontend/src/i18n/locales/zh.ts
```

- [ ] **Step 3: 追加 `adminAccessHeatmap` 到 sidebar 和 routes 段** — 直接复制自 spec §6.8.1 的 sidebar / routes 增量

- [ ] **Step 2: 追加 admin.accessHeatmap 段 + sidebar / routes 增量**

参考 spec §6.8.1 完整内容,直接复制粘贴。

> 关键:如果文件里没有 `admin:` 这一层,新增;如果已有,把 `accessHeatmap` 作为 admin 的子键插入。

- [ ] **Step 3: 验证类型检查**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
```

Expected: 通过(若 TS 报"missing key",检查键拼写)。

### Task 18: 加 ko.ts keys

**Files:**
- Modify: `frontend/src/i18n/locales/ko.ts`

- [ ] **Step 1: 检查现有 `admin:` 命名空间是否存在**

```bash
grep -n "^  admin:\|^admin:" frontend/src/i18n/locales/ko.ts
```

- 若存在 → 合并到现有 `admin` 段
- 若不存在 → 新增整段(spec §6.8.2)

- [ ] **Step 2: 加 sidebar / routes 段(同 zh 流程)**
- [ ] **Step 3: 验证**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
cd frontend && npm run build 2>&1 | tail -15
```

Expected: vue-tsc + npm run build 都通过。

**提交节点**:Task 17-18 一起提交,commit message:

```
feat(i18n): 访问率热力图 zh/ko 文案

- locales/zh.ts:admin.accessHeatmap.* + sidebar.adminAccessHeatmap + routes.adminAccessHeatmap
- locales/ko.ts:同上(韩文翻译)
- 关联 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md §6.8
```

注意:`en.ts` 暂不补(项目当前无 en.ts,本 spec 范围内不创建)。

---

## Phase 8: 最终验证

### Task 19: 跑全量验证

**Files:** 无新增(只跑命令)

- [ ] **Step 1: 后端编译 + 全量测试**

```bash
cd backend && mvn clean install -DskipTests -q 2>&1 | tail -10
cd backend && mvn -pl . test -DfailIfNoTests=false -q 2>&1 | tail -10
```

Expected: 编译 BUILD SUCCESS;测试全部通过(包含 PageViewServiceTest 5 用例)。

- [ ] **Step 2: 前端类型检查 + 构建**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
cd frontend && npm run build 2>&1 | tail -15
```

Expected: 编译 + 构建都通过,无 TS 错误。

- [ ] **Step 3: 跑完所有自动验证后,把结果汇总给用户**

告诉用户:
- 后端编译/测试结果
- 前端 vue-tsc / npm run build 结果
- 询问用户是否要启动服务做手动验收(由用户决定,不要自己启 `mvn spring-boot:run` / `npm run dev`)

**手动验收清单**(用户自验,不在此 task 内):参考 spec §10.3 的 13 步。

---

## 部署说明(给用户,不在 task 内)

- 后端先发(SQL + Java)
- 前端后发
- 天然灰度:前端不上线则无 PV 流量,但 `/admin/access-heatmap` 路由已存在(只有 admin 看得到菜单)
- 回滚:revert 即可,后端数据可保留(下次再发不丢历史)

---

## 不在本次范围(再次提醒,见 spec §7)

- ❌ 副视图(页面 × 用户)
- ❌ 页面平均停留时长
- ❌ 趋势环比 / 同比
- ❌ 自动轮询 / 实时刷新
- ❌ 数据导出
- ❌ `en.ts` 补建
- ❌ `sys_page_view` 自动清理 scheduler(后续 PR)
- ❌ 抽 `@RequiresAdmin` 注解
- ❌ 抽 `views/admin/` 子目录
- ❌ Prometheus 埋点
- ❌ 翻页 / 缩放

---

## 实施完成后,提醒用户

1. spec 文件(本 task 之前完成,未提交)是否要 commit
2. 部署顺序:后端先发,前端后发
3. 上线后观察 1-2 天,看 `sys_page_view` 表的数据量是否符合预期
4. 如有用户反馈,再决定是否做 §7 中的 YAGNI 项
