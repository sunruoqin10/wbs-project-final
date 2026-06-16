# 页面访问率热力图 — 设计

> 日期:2026-06-16
> 状态:已设计,待提交 + spec review
> 前置依赖:无(完全从零搭建)

## 1. 背景与目标

`AuthInterceptor` 当前只做鉴权,不写日志;前端 `Dashboard.vue` 用 ECharts 展示项目/任务进度统计;**没有任何"页面访问率"维度的统计**。管理员无法回答"用户主要在什么时段访问哪些页面"。

**诉求**:管理员(admin)能看一张热力图,横轴是 0-23 小时,纵轴是各页面路由名,格子颜色深浅表示"该页面在该小时的日均访问次数"。

**目标**:
- 每次用户切换前端路由,落库一条访问记录
- 管理员打开 `/admin/access-heatmap`,看到 4 张 KPI 卡 + 一张 ECharts heatmap
- 窗口可切 1d / 7d / 30d / 90d,值用"日均"归一化,跨窗口可比
- 仅 admin 可看;非 admin 调 admin 接口返 403

## 2. 核心规则(决策表)

### 2.1 数据维度

| 维度 | 决定 | 理由 |
|------|------|------|
| 视图数量 | **1 个**(主视图:页面 × 24h) | 用户取消了原计划的"页面 × 用户"副视图,降低复杂度 |
| 指标 | **PV(访问次数)** | 不去重,不计算 UV;最简单、最直观 |
| 归一化 | **日均**(`count(*) / days`) | 1d/7d/30d/90d 窗口下数字可对比,不会出现"90d 数字是 1d 的 90 倍" |
| 时间窗口 | **1d / 7d / 30d / 90d 四选一**(顶栏 select 切换) | 简单枚举,后端强校验 |
| 窗口形状 | **总是 24 小时**(0-23 列) | 跨窗口认知一致,文案说"日均"符合直觉 |
| 配色 | **灰→蓝 4 档离散**(0 / 0-1 / 1-5 / 5+) | 与项目主题色 `primary-*` 一致,中性,无好坏暗示 |
| 下钻 | **仅 tooltip**,无独立详情页 | 用户明确选择"够用就好" |
| 页面布局 | **顶栏 + 4 KPI 卡 + 主热力图 + 底栏** | 模仿现有 `Dashboard.vue` 4 卡风格,保持项目统一 |
| 权限 | **仅 admin** | 隐私与权限最严 |
| 菜单位置 | **Sidebar 与 `SchedulerManagement` 平级** | 现有唯一 admin-only 项的并列位置,改动最小 |
| 鉴权方式 | **路径前缀 hack**:`AuthInterceptor.preHandle` 末尾检查 `/api/admin/**` 强制 role=admin | admin 接口暂只有这 1 处,>5 个再抽 `@RequiresAdmin` 注解 |
| 埋点 | **前端 `router.afterEach` 同步 POST** | 50 人内网场景,简单可靠;失败静默吞掉 |
| 刷新 | **手动 + 窗口切换触发**,无自动轮询 | YAGNI |
| 数据留存 | **90 天**,超期硬删除(后续单独 PR 加 scheduler) | 用户规模 50 人 × 50 PV/天 × 90 天 ≈ 22.5 万行,单表无压力 |
| i18n | **zh + ko 同步**,`en.ts` 暂不建 | 项目当前 `en.ts` 不存在,本 spec 不补 |
| 写入策略 | **失败静默** | 不影响用户操作;dev 模式 `console.warn` 一次 |

### 2.2 写入触发与过滤

```
user 进入页面
  ↓
router.afterEach(to)
  ↓
filter:
  - to.name 必须存在(排除 /test /documents-test 等调试页)
  - to.path 不能是 /login 或 /forbidden(白名单)
  ↓
apiService.postPageView({ pagePath: to.fullPath, pageName: to.name, occurredAt: ISO_NOW })
  ↓ catch 静默
```

### 2.3 后端校验

| 入参 | 校验 | 失败处理 |
|------|------|---------|
| `pagePath` | 必须以 `/` 开头,长度 ≤ 255 | `Result.error(400, ...)` |
| `pageName` | 长度 ≤ 100(表约束) | `Result.error(400, ...)` |
| `occurredAt` | 不能 > `now + 1min`(防时钟漂移) | `Result.error(400, ...)` |
| `userId` | 从 `request.getAttribute("userId")` 拿(防前端伪造) | — |
| `window` 查询参数 | 必须 ∈ {1d, 7d, 30d, 90d} | `Result.error(400, ...)` |
| admin 接口调用者 role | 必须 = `admin` | `Result.error(403, ...)` |

## 3. 架构与数据流

```
┌─────────────────────────────────────────────────────────────┐
│  浏览器 (任意已登录用户)                                     │
│                                                              │
│   user 切换页面                                              │
│      ↓                                                       │
│   vue-router afterEach hook (App.vue 挂载)                   │
│      ↓                                                       │
│   useAccessLog().install()                                   │
│      ↓ POST /api/page-views { path, name, ts }               │
│      ↓ headers: Authorization, X-User-Id, X-User-Role        │
└──────────────┬──────────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────────┐
│  Spring Boot (后端)                                          │
│                                                              │
│   AuthInterceptor.preHandle 鉴权 (现有)                      │
│      - 检查 Authorization / X-User-Id / X-User-Role          │
│      - 【新】URL.startsWith("/api/admin/") 时强制 role=admin │
│      ↓                                                       │
│   PageViewController.record()  (新)                          │
│      - userId = request.getAttribute("userId")              │
│      - 校验 pagePath / pageName / occurredAt                │
│      - pageViewService.insert(...)                           │
│      ↓                                                       │
│   PageViewMapper / sys_page_view 表 (新)                     │
│                                                              │
│   ┄┄┄┄┄┄┄┄┄┄┄ admin 读路径(被 AuthInterceptor 拦) ┄┄┄┄┄┄┄┄┄┄│
│                                                              │
│   AdminPageViewController.heatmap()  (新)                    │
│      ↓                                                       │
│   PageViewService.getHeatmap(window)                         │
│      - SQL 1: matrix 主查询 (page, hour) → COUNT/天数        │
│      - SQL 2: page 列表 + 各自日均 (TOP-N by totalVisits)    │
│      - Java 侧二次计算 KPI(topPage, peakHour)                │
│      ↓                                                       │
│   返回 HeatmapResponse { window, kpi, xAxis, yAxis, matrix } │
└──────────────┬──────────────────────────────────────────────┘
               │
               ↑ GET /api/admin/page-views/heatmap?window=7d
               │
┌─────────────────────────────────────────────────────────────┐
│  浏览器 (admin 打开 /admin/access-heatmap)                    │
│                                                              │
│   AccessHeatmapView.vue (新)                                  │
│      - 4 KPI 卡 + ECharts heatmap + 底栏                      │
│      - 顶栏 select 切换 1d/7d/30d/90d                        │
│      - 失败 fallback 渲染错误条 + 刷新按钮                    │
└─────────────────────────────────────────────────────────────┘
```

## 4. 数据库变更

### 4.1 `backend/add_page_view_table.sql`(新建)

**重要**:本 SQL 必须在服务启动之前跑(否则 `PageViewMapper.xml` 引用 `sys_page_view` 时启动报错)。

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
    INDEX idx_pv_occurred_at (occurred_at),                              -- 时间范围扫描
    INDEX idx_pv_page_occurred (page_name, occurred_at),                 -- 热力图主查询
    INDEX idx_pv_user_occurred (user_id, occurred_at)                    -- 用户维度(预留,本期不用)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志';
```

**索引决策**:
- `idx_pv_occurred_at`:`DELETE` 清理任务 + 1d 窗口主驱动
- `idx_pv_page_occurred`:热力图主查询 `WHERE page_name IN (...) AND occurred_at BETWEEN ?` 走索引覆盖
- `idx_pv_user_occurred`:本期不用(已取消副视图 B),但保留——零成本、未来灵活

**不做 partition / 不做 pre-aggregate**:90 天数据 MySQL 聚合 < 200ms,运维成本 > 收益。

## 5. 后端改动

### 5.1 新文件:`backend/src/main/java/com/wbs/project/entity/PageView.java`

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

### 5.2 新文件:`backend/src/main/java/com/wbs/project/mapper/PageViewMapper.java`

```java
package com.wbs.project.mapper;

import com.wbs.project.entity.PageView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PageViewMapper {
    int insert(PageView record);

    /** 热力图主查询:返回 [page_name, hour, daily_avg] */
    List<Map<String, Object>> aggregateByPageAndHour(
        @Param("fromTs") LocalDateTime fromTs,
        @Param("toTs")   LocalDateTime toTs
    );

    /** 页面列表 + 各页面窗口内总 PV + 日均 */
    List<Map<String, Object>> aggregateByPage(
        @Param("fromTs") LocalDateTime fromTs,
        @Param("toTs")   LocalDateTime toTs
    );

    /** 清理超期日志(后续 PR 用,本期不挂 scheduler) */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
```

### 5.3 新文件:`backend/src/main/resources/mapper/PageViewMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.wbs.project.mapper.PageViewMapper">

    <insert id="insert" parameterType="com.wbs.project.entity.PageView"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO sys_page_view (user_id, page_path, page_name, occurred_at)
        VALUES (#{userId}, #{pagePath}, #{pageName}, #{occurredAt})
    </insert>

    <!-- 热力图主查询:返回每页每小时的总 PV,在 Java 侧 / days 转日均 -->
    <select id="aggregateByPageAndHour" resultType="java.util.HashMap">
        SELECT page_name                                       AS pageName,
               HOUR(occurred_at)                               AS hour,
               COUNT(*)                                        AS pv
          FROM sys_page_view
         WHERE occurred_at &gt;= #{fromTs}
           AND occurred_at &lt;= #{toTs}
         GROUP BY page_name, HOUR(occurred_at)
    </select>

    <!-- 页面列表:总 PV + 日均,按总 PV 倒序 -->
    <select id="aggregateByPage" resultType="java.util.HashMap">
        SELECT page_name                                       AS pageName,
               COUNT(*)                                        AS totalVisits
          FROM sys_page_view
         WHERE occurred_at &gt;= #{fromTs}
           AND occurred_at &lt;= #{toTs}
         GROUP BY page_name
         ORDER BY totalVisits DESC
    </select>

    <delete id="deleteOlderThan">
        DELETE FROM sys_page_view WHERE occurred_at &lt; #{cutoff}
    </delete>

</mapper>
```

### 5.4 新文件:`backend/src/main/java/com/wbs/project/dto/HeatmapResponse.java`

```java
package com.wbs.project.dto;

import lombok.Data;
import java.util.List;

@Data
public class HeatmapResponse {
    private String window;            // '1d' | '7d' | '30d' | '90d'
    private String fromTs;            // ISO 字符串
    private String toTs;              // ISO 字符串
    private Integer days;             // 1 / 7 / 30 / 90
    private Kpi kpi;
    private List<Integer> xAxis;      // [0..23]
    private List<String> yAxis;       // 页面名(按总 PV 倒序)
    private List<List<Double>> matrix;// [yAxis.length][24] 日均 PV(无数据=0)
    private Double maxValue;          // matrix 中最大值(供前端 visualMap 配色)

    @Data
    public static class Kpi {
        private Long totalVisits;     // 窗口内总 PV
        private Double dailyAvg;      // 日均 PV(保留 1 位小数)
        private String topPageName;   // 最热页面
        private Double topPageAvg;    // 最热页面日均 PV
        private Integer peakHour;     // 全局最热小时(0-23)
        private Double peakHourAvg;   // peakHour 的日均 PV
    }
}
```

### 5.5 新文件:`backend/src/main/java/com/wbs/project/service/PageViewService.java`

```java
package com.wbs.project.service;

import com.wbs.project.dto.HeatmapResponse;
import com.wbs.project.entity.PageView;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.PageViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PageViewService {

    private final PageViewMapper pageViewMapper;

    /** 合法窗口白名单 */
    private static final Set<String> VALID_WINDOWS =
        Set.of("1d", "7d", "30d", "90d");

    /** 写入一条访问记录(校验在 controller 层完成,这里只做入库) */
    public void record(String userId, String pagePath, String pageName, LocalDateTime occurredAt) {
        PageView pv = new PageView();
        pv.setUserId(userId);
        pv.setPagePath(pagePath);
        pv.setPageName(pageName);
        pv.setOccurredAt(occurredAt);
        pageViewMapper.insert(pv);
    }

    /** 读热力图(窗口+天数为唯一入参,fromTs/toTs 在内部推算) */
    public HeatmapResponse getHeatmap(String window) {
        if (!VALID_WINDOWS.contains(window)) {
            throw new BusinessException(400, "window 必须是 1d/7d/30d/90d");
        }
        int days = parseDays(window);
        LocalDateTime toTs = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);  // 今天 23:59:59
        LocalDateTime fromTs = toTs.toLocalDate().minusDays(days - 1L).atStartOfDay();  // (今天-天数+1) 00:00:00

        // 1) 主查询:matrix 原始数据
        List<Map<String, Object>> raw = pageViewMapper.aggregateByPageAndHour(fromTs, toTs);

        // 2) 页面列表(按总 PV 倒序)
        List<Map<String, Object>> pageRows = pageViewMapper.aggregateByPage(fromTs, toTs);

        // 3) 拼装 yAxis(按 pageRows 顺序 = 总 PV 倒序)
        List<String> yAxis = new ArrayList<>();
        Map<String, Long> pageTotal = new LinkedHashMap<>();
        for (Map<String, Object> r : pageRows) {
            String name = (String) r.get("pageName");
            Long total = ((Number) r.get("totalVisits")).longValue();
            yAxis.add(name);
            pageTotal.put(name, total);
        }

        // 4) 拼装 matrix [yAxis.length][24],缺位填 0
        double[][] matrix = new double[yAxis.size()][24];
        Map<String, Integer> pageIdx = new HashMap<>();
        for (int i = 0; i < yAxis.size(); i++) pageIdx.put(yAxis.get(i), i);

        long totalVisits = 0;
        double maxValue = 0.0;
        for (Map<String, Object> r : raw) {
            String name = (String) r.get("pageName");
            Integer hour = ((Number) r.get("hour")).intValue();
            Long pv = ((Number) r.get("pv")).longValue();
            Integer yi = pageIdx.get(name);
            if (yi == null) continue;  // 不在 yAxis(理论上不会,防御)
            double dailyAvg = round1(pv.doubleValue() / days);
            matrix[yi][hour] = dailyAvg;
            totalVisits += pv;
            if (dailyAvg > maxValue) maxValue = dailyAvg;
        }

        // 5) KPI 二次计算
        HeatmapResponse.Kpi kpi = new HeatmapResponse.Kpi();
        kpi.setTotalVisits(totalVisits);
        kpi.setDailyAvg(round1(totalVisits / (double) days));

        // topPage = yAxis 第一个(已经按总 PV 倒序);无数据时 yAxis 空 → topPageName=null
        if (!yAxis.isEmpty()) {
            String topPage = yAxis.get(0);
            kpi.setTopPageName(topPage);
            double topPageDailyAvg = pageTotal.get(topPage).doubleValue() / days;
            kpi.setTopPageAvg(round1(topPageDailyAvg));
        }

        // peakHour = 把 matrix 横向加和,取 max 对应的 hour
        int peakHour = -1;
        double peakHourAvg = 0.0;
        for (int h = 0; h < 24; h++) {
            double sum = 0;
            for (int y = 0; y < yAxis.size(); y++) sum += matrix[y][h];
            if (sum > peakHourAvg) {
                peakHourAvg = sum;
                peakHour = h;
            }
        }
        kpi.setPeakHour(peakHour >= 0 ? peakHour : 0);
        kpi.setPeakHourAvg(round1(peakHourAvg));

        // 6) 装填响应
        HeatmapResponse resp = new HeatmapResponse();
        resp.setWindow(window);
        resp.setFromTs(fromTs.toString());
        resp.setToTs(toTs.toString());
        resp.setDays(days);
        resp.setKpi(kpi);
        resp.setXAxis(new ArrayList<>(List.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23)));
        resp.setYAxis(yAxis);
        List<List<Double>> matrixOut = new ArrayList<>();
        for (double[] row : matrix) {
            List<Double> r = new ArrayList<>(24);
            for (double v : row) r.add(v);
            matrixOut.add(r);
        }
        resp.setMatrix(matrixOut);
        resp.setMaxValue(round1(maxValue));
        return resp;
    }

    private static int parseDays(String window) {
        return switch (window) {
            case "1d" -> 1;
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new BusinessException(400, "window 非法");
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
```

### 5.6 新文件:`backend/src/main/java/com/wbs/project/controller/PageViewController.java`

写接口(全员):

```java
package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.PageView;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.service.PageViewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/page-views")
@RequiredArgsConstructor
public class PageViewController {

    private final PageViewService pageViewService;

    @PostMapping
    public Result<Void> record(
        @RequestBody PageView body,
        HttpServletRequest request
    ) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");

        // 校验
        if (body.getPagePath() == null || !body.getPagePath().startsWith("/")) {
            throw new BusinessException(400, "pagePath 必须以 / 开头");
        }
        if (body.getPageName() == null || body.getPageName().isBlank()) {
            throw new BusinessException(400, "pageName 不能为空");
        }
        if (body.getPagePath().length() > 255) {
            throw new BusinessException(400, "pagePath 长度超过 255");
        }
        if (body.getPageName().length() > 100) {
            throw new BusinessException(400, "pageName 长度超过 100");
        }
        // occurredAt 由 Jackson + jackson-datatype-jsr310 + PageView.@JsonFormat 直接反序列化为 LocalDateTime
        // (前端发 ISO 8601 带 T 即可,如 "2026-06-16T14:23:45")
        if (body.getOccurredAt() == null) {
            throw new BusinessException(400, "occurredAt 不能为空");
        }
        if (body.getOccurredAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BusinessException(400, "occurredAt 不能晚于当前时间 1 分钟");
        }

        pageViewService.record(userId, body.getPagePath(), body.getPageName(), body.getOccurredAt());
        return Result.success(null);
    }
}
```

### 5.7 新文件:`backend/src/main/java/com/wbs/project/controller/AdminPageViewController.java`

读接口(admin-only):

```java
package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.HeatmapResponse;
import com.wbs.project.service.PageViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/page-views")
@RequiredArgsConstructor
public class AdminPageViewController {

    private final PageViewService pageViewService;

    @GetMapping("/heatmap")
    public Result<HeatmapResponse> heatmap(@RequestParam String window) {
        return Result.success(pageViewService.getHeatmap(window));
    }
}
```

> 鉴权由 `AuthInterceptor` 路径前缀检查承担(见 §5.8),不在 controller 加注解。

### 5.8 改动:`AuthInterceptor.java`(在 preHandle 末尾加 admin 路径检查)

```java
// 在 preHandle 方法末尾,return true 之前增加:
if (request.getRequestURI().startsWith("/api/admin/")) {
    if (!"admin".equals(role)) {
        throw new BusinessException(403, "需要管理员权限");
    }
}
```

> 现有 `AuthInterceptor` 已经能拿到 `role` 变量(JWT 解析后),无需新解析。

## 6. 前端改动

### 6.1 新文件:`frontend/src/composables/useAccessLog.ts`

```ts
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { apiService } from '@/services/api';

let installed = false;   // 模块级单例,HMR 也不会重复装

export function useAccessLog() {
  const router = useRouter();

  const install = () => {
    if (installed) return;
    installed = true;

    router.afterEach((to) => {
      // 排除:无 name(调试页)、登录页、403
      if (!to.name) return;
      if (to.path === '/login' || to.path === '/forbidden') return;

      apiService.postPageView({
        pagePath: to.fullPath,
        pageName: String(to.name),
        occurredAt: new Date().toISOString(),
      }).catch((err) => {
        if (import.meta.env.DEV) {
          console.warn('[useAccessLog] post failed:', err);
        }
        // 生产环境静默吞掉,不影响用户操作
      });
    });
  };

  onMounted(install);
}
```

### 6.2 改动:`frontend/src/App.vue`

在 `<script setup>` 末尾追加一行:

```ts
import { useAccessLog } from './composables/useAccessLog';
useAccessLog();
```

### 6.3 改动:`frontend/src/services/api.ts`

在 `ApiService` 类末尾(其他 `stopScheduler` 等方法之后)加:

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

并在文件顶部 import 处加类型引用:

```ts
import type { HeatmapResponse } from '@/types';
```

### 6.4 改动:`frontend/src/types/index.ts`

新增类型(放在文件末尾):

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

### 6.5 新文件:`frontend/src/views/AccessHeatmapView.vue`

(关键部分,完整组件约 250 行)

```vue
<template>
  <div class="p-6 space-y-6">
    <!-- 顶栏 -->
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold text-text">{{ $t('admin.accessHeatmap.title') }}</h1>
      <div class="flex items-center gap-3">
        <label class="text-sm text-text-muted">{{ $t('admin.accessHeatmap.controls.windowLabel') }}</label>
        <select
          v-model="window"
          class="rounded-md border border-border bg-surface px-3 py-1.5 text-sm"
          @change="fetchData"
        >
          <option value="1d">{{ $t('admin.accessHeatmap.controls.windowOptions.d1') }}</option>
          <option value="7d">{{ $t('admin.accessHeatmap.controls.windowOptions.d7') }}</option>
          <option value="30d">{{ $t('admin.accessHeatmap.controls.windowOptions.d30') }}</option>
          <option value="90d">{{ $t('admin.accessHeatmap.controls.windowOptions.d90') }}</option>
        </select>
        <button
          class="rounded-md bg-primary-500 px-3 py-1.5 text-sm text-white hover:bg-primary-600 disabled:opacity-50"
          :disabled="loading"
          @click="fetchData"
        >
          {{ loading
            ? $t('admin.accessHeatmap.controls.refreshing')
            : $t('admin.accessHeatmap.controls.refresh') }}
        </button>
      </div>
    </div>

    <p class="text-sm text-text-muted">{{ $t('admin.accessHeatmap.description') }}</p>

    <!-- 4 KPI 卡 -->
    <div v-if="!error" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <KpiCard
        :title="$t('admin.accessHeatmap.kpi.totalVisits')"
        :value="data?.kpi.totalVisits ?? '—'"
        :subtitle="$t('admin.accessHeatmap.kpi.totalVisitsSub')"
      />
      <KpiCard
        :title="$t('admin.accessHeatmap.kpi.dailyAvg')"
        :value="data?.kpi.dailyAvg ?? '—'"
        :subtitle="$t('admin.accessHeatmap.kpi.dailyAvgSub')"
      />
      <KpiCard
        :title="$t('admin.accessHeatmap.kpi.topPage')"
        :value="data?.kpi.topPageName ?? '—'"
        :subtitle="data?.kpi.topPageAvg != null
          ? $t('admin.accessHeatmap.kpi.topPageSub', { avg: data.kpi.topPageAvg })
          : ''"
      />
      <KpiCard
        :title="$t('admin.accessHeatmap.kpi.peakHour')"
        :value="data?.kpi.peakHour != null ? pad(data.kpi.peakHour) + ':00' : '—'"
        :subtitle="data?.kpi.peakHourAvg != null
          ? $t('admin.accessHeatmap.kpi.peakHourSub', { avg: data.kpi.peakHourAvg })
          : ''"
      />
    </div>

    <!-- 热力图 -->
    <div class="rounded-lg border border-border bg-surface p-4 shadow-sm">
      <div v-if="error" class="py-12 text-center">
        <p class="text-error mb-3">{{ $t('admin.accessHeatmap.errors.loadFailed') }}</p>
        <button
          class="rounded-md bg-primary-500 px-4 py-1.5 text-sm text-white"
          @click="fetchData"
        >
          {{ $t('admin.accessHeatmap.controls.refresh') }}
        </button>
      </div>
      <div v-else-if="loading && !data" class="h-96 animate-pulse rounded bg-background" />
      <div v-else ref="chartEl" class="h-[480px] w-full" />
    </div>

    <!-- 底栏 -->
    <div class="text-xs text-text-muted">
      {{ $t('admin.accessHeatmap.footer.lastUpdated') }}: {{ lastUpdatedStr }} ·
      {{ $t('admin.accessHeatmap.footer.dataRange') }}: {{ $t('admin.accessHeatmap.footer.dataRangeValue') }} ·
      {{ $t('admin.accessHeatmap.footer.dataSource') }}: {{ $t('admin.accessHeatmap.footer.dataSourceValue') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import { apiService } from '@/services/api';
import type { HeatmapResponse } from '@/types';
import KpiCard from '@/components/common/KpiCard.vue';

const window = ref<'1d' | '7d' | '30d' | '90d'>('7d');
const data = ref<HeatmapResponse | null>(null);
const loading = ref(false);
const error = ref(false);
const chartEl = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;
const lastUpdatedStr = ref('');

const pad = (n: number) => String(n).padStart(2, '0');

const fetchData = async () => {
  loading.value = true;
  error.value = false;
  try {
    const r = await apiService.getHeatmap(window.value);
    data.value = r;
    lastUpdatedStr.value = new Date().toLocaleTimeString();
    await nextTick();
    renderChart(r);
  } catch (e) {
    error.value = true;
    data.value = null;
  } finally {
    loading.value = false;
  }
};

const renderChart = (d: HeatmapResponse) => {
  if (!chartEl.value) return;
  if (!chart) chart = echarts.init(chartEl.value);

  const seriesData: [number, number, number][] = [];
  let maxV = 0;
  d.matrix.forEach((row, yi) => {
    row.forEach((v, xi) => {
      seriesData.push([xi, yi, v]);
      if (v > maxV) maxV = v;
    });
  });

  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: (p: any) => {
        const yi = p.data[1] as number;
        const xi = p.data[0] as number;
        const v = p.data[2] as number;
        return `${d.yAxis[yi]}<br/>${pad(xi)}:00<br/>${$t('admin.accessHeatmap.chart.tooltipAvg', { avg: v })}`;
      },
    },
    grid: { left: 140, right: 30, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: d.xAxis.map((h) => pad(h)),
      name: $t('admin.accessHeatmap.chart.hourAxis'),
      nameLocation: 'end',
      splitArea: { show: true },
    },
    yAxis: {
      type: 'category',
      data: d.yAxis,
      name: $t('admin.accessHeatmap.chart.pageAxis'),
      nameLocation: 'end',
      splitArea: { show: true },
    },
    visualMap: {
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 10,
      pieces: [
        { min: 0,    max: 0,    color: '#f5f5f5', label: '0' },
        { min: 0.1,  max: 1,    color: '#bfdbfe', label: '0-1' },
        { min: 1.1,  max: 5,    color: '#60a5fa', label: '1-5' },
        { min: 5.1,             color: '#1d4ed8', label: '5+' },
      ],
    },
    series: [{
      type: 'heatmap',
      data: seriesData,
      label: { show: false },
      emphasis: { itemStyle: { borderColor: '#000', borderWidth: 1 } },
    }],
  }, true);
};

watch(window, () => { /* select @change 已触发 fetchData,这里仅占位 */ });

onMounted(() => {
  fetchData();
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});

const onResize = () => chart?.resize();
</script>
```

> `KpiCard.vue` 组件若不存在,实施时新建(从 `Dashboard.vue` 抽出;若抽组件成本 > 收益,可直接 inline 4 个卡,本 spec 留 1 个 fallback)。
>
> 若 `views/AccessHeatmapView.vue` 暂不抽 KpiCard,可改为 inline:`<div class="rounded-lg border bg-surface p-4 shadow-sm"><p class="text-sm text-text-muted">...</p><p class="text-2xl font-semibold mt-1">...</p></div>` × 4。**实施阶段决定**。

### 6.6 改动:`frontend/src/router/index.ts`

新增路由(参考 `scheduler-management` 的写法):

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

### 6.7 改动:`frontend/src/components/layout/Sidebar.vue`

在 `menuItems` 内的 admin-only 段(L134-139 附近)追加一项:

```ts
...(permissionStore.currentRole === 'admin' ? [
  { key: 'admin-access-heatmap', label: 'sidebar.adminAccessHeatmap', icon: '...', path: '/admin/access-heatmap' },
  { key: 'scheduler-management', label: 'sidebar.schedulerManagement', icon: '...', path: '/scheduler-management' },
] : []),
```

> icon 复用现有 inline SVG 风格;具体 path 写实施时填。

### 6.8 i18n(`zh.ts` + `ko.ts`)

#### 6.8.1 `frontend/src/i18n/locales/zh.ts` 新增

```ts
admin: {
  accessHeatmap: {
    title: '访问率热力图',
    description: '各页面在不同小时的访问热度,数值已按"日均"归一化。',
    controls: {
      windowLabel: '时间窗口',
      windowOptions: {
        d1: '今日',
        d7: '最近 7 天',
        d30: '最近 30 天',
        d90: '最近 90 天',
      },
      refresh: '刷新',
      refreshing: '刷新中…',
    },
    kpi: {
      totalVisits: '总访问',
      totalVisitsSub: '窗口内累计',
      dailyAvg: '日均访问',
      dailyAvgSub: '总访问 ÷ 天数',
      topPage: '最热页面',
      topPageSub: '日均 {avg} 次',
      peakHour: '最热时段',
      peakHourSub: '日均 {avg} 次',
    },
    chart: {
      hourAxis: '小时',
      pageAxis: '页面',
      tooltipAvg: '日均 {avg} 次',
      empty: '尚无访问数据',
    },
    footer: {
      lastUpdated: '最后更新',
      dataRange: '数据范围',
      dataRangeValue: '最近 90 天',
      dataSource: '数据源',
      dataSourceValue: 'sys_page_view',
    },
    errors: {
      loadFailed: '统计服务暂不可用,请稍后重试',
      forbidden: '需要管理员权限',
    },
  },
},

// 在 sidebar 段加:
sidebar: {
  // 现有
  adminAccessHeatmap: '访问率热力图',
},

// 在 routes 段加:
routes: {
  // 现有
  adminAccessHeatmap: '访问率热力图',
},
```

#### 6.8.2 `frontend/src/i18n/locales/ko.ts` 新增

```ts
admin: {
  accessHeatmap: {
    title: '페이지 방문율 히트맵',
    description: '각 페이지의 시간대별 방문热度,값은 "일 평균"으로 정규화되었습니다.',
    controls: {
      windowLabel: '기간',
      windowOptions: {
        d1: '오늘',
        d7: '최근 7일',
        d30: '최근 30일',
        d90: '최근 90일',
      },
      refresh: '새로고침',
      refreshing: '새로고침 중…',
    },
    kpi: {
      totalVisits: '총 방문',
      totalVisitsSub: '기간 내 누적',
      dailyAvg: '일 평균 방문',
      dailyAvgSub: '총 방문 ÷ 일수',
      topPage: '최다 방문 페이지',
      topPageSub: '일 평균 {avg}회',
      peakHour: '최다 방문 시간',
      peakHourSub: '일 평균 {avg}회',
    },
    chart: {
      hourAxis: '시간',
      pageAxis: '페이지',
      tooltipAvg: '일 평균 {avg}회',
      empty: '방문 데이터가 없습니다',
    },
    footer: {
      lastUpdated: '마지막 업데이트',
      dataRange: '데이터 범위',
      dataRangeValue: '최근 90일',
      dataSource: '데이터 소스',
      dataSourceValue: 'sys_page_view',
    },
    errors: {
      loadFailed: '통계 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요',
      forbidden: '관리자 권한이 필요합니다',
    },
  },
},

sidebar: {
  // 现有
  adminAccessHeatmap: '페이지 방문 히트맵',
},

routes: {
  // 现有
  adminAccessHeatmap: '페이지 방문 히트맵',
},
```

## 7. 不在本次范围(YAGNI)

- 用户维度副视图(已取消,见 §2.1)
- 页面平均停留时长指标
- 趋势环比 / 同比
- 自动轮询 / 实时刷新
- 数据导出 CSV / PNG
- `frontend/src/i18n/locales/en.ts` 补建
- `sys_page_view` 自动清理 scheduler(后续单独 PR)
- `pageName` 路由重命名迁移工具
- 抽 `@RequiresAdmin` 注解(admin 接口只 1 处,不值得)
- 抽 `views/admin/` 子目录(项目当前 views 全部平铺,跟着惯例)
- 抽 `HeatmapChart.vue` 组件(单页面单图,过早抽象)
- 详细访问明细列表(只展示聚合,不要"全量日志")
- Prometheus / 监控埋点
- 翻页 / 滚动 / 缩放 Y 轴(路由数 22,不需要)

## 8. 边界场景

| # | 场景 | 预期行为 |
|---|------|---------|
| E1 | 用户首次访问,/login → /dashboard | POST /api/page-views 一次,pageName=Dashboard |
| E2 | 同一用户 5 秒内 3 次切页面 | 3 条记录,不去重(刷新算新访问) |
| E3 | postPageView 失败(网络/500) | 静默吞掉,dev 模式 console.warn 一次,用户操作不受影响 |
| E4 | 非 admin 用户打开 /admin/access-heatmap | 路由进入后 GET /heatmap 返 403,前端显示错误条 |
| E5 | admin 切换 1d → 7d | 顶栏 select @change 触发 fetchData,4 KPI + 热力图整体重渲染 |
| E6 | 窗口内无任何访问数据(全新部署) | 4 KPI 全 0 / null;热力图 1 行"Dashboard"灰色 + "尚无访问数据" |
| E7 | 某个页面 1d 窗口内 0 次访问 | 该页面不出现在 yAxis(被 SQL 排除);用户看到"少了这一行"是预期 |
| E8 | 7d 窗口,某页面在 3-5 点 0 次,14 点 50 次 | 矩阵中 3-5 灰,14 深蓝;math avg = 50/7 ≈ 7.1,落入"5+"档 |
| E9 | 90d 窗口,某页面总 PV 100 | 日均 = 100/90 ≈ 1.1,落入"1-5"档 |
| E10 | occurredAt 时间错误格式 | 后端 400 "occurredAt 时间格式错误" |
| E11 | occurredAt 在未来 2 分钟 | 后端 400 "occurredAt 不能晚于当前时间 1 分钟" |
| E12 | pagePath 是 "../etc/passwd" | 后端 400 "pagePath 必须以 / 开头" |
| E13 | pageName 是空字符串 | 后端 400 "pageName 不能为空" |
| E14 | 路由 meta.name 缺失(/test) | `useAccessLog` 跳过(`!to.name` 直接 return) |
| E15 | 路由 path 是 /login | `useAccessLog` 跳过(白名单) |
| E16 | 后端 sys_page_view 表不存在 | mapper 调用时 SQL 报错 → 500 → 前端错误条 |
| E17 | admin 用户 token 过期 | AuthInterceptor 401,前端跳登录页(沿用) |
| E18 | App.vue HMR 反复挂载 | `installed` 模块级 singleton,只装 1 次 afterEach |
| E19 | ECharts dispose 后再 setOption(组件 unmount 后) | `onBeforeUnmount` dispose 兜底,不会内存泄漏 |
| E20 | 1d 窗口在凌晨 0-8 点数据稀疏 | 4 档离散色阶保证颜色对比;maxValue 来自后端,1d 也会有合理分布 |

## 9. 风险与缓解

| # | 风险 | 严重度 | 缓解 |
|---|------|--------|------|
| R1 | `AuthInterceptor` 加 admin 路径检查,影响范围 /api/admin/**,可能误伤未来其他 admin 接口 | 低 | 检查精确前缀 + 注释说明;若误伤,后续抽 `@RequiresAdmin` 注解时再细化 |
| R2 | 后端 SQL 走 `(page_name, occurred_at)` 复合索引,90d 窗口下 GROUP BY 慢 | 低 | 22.5 万行规模 MySQL < 200ms;90d 后清理任务会把表控制在 22.5 万行以内 |
| R3 | 路由 name 重命名后历史数据混乱(/projects → ProjectDetail 改名为 Projects) | 低 | 接受(YAGNI);`pagePath` 仍可查 |
| R4 | 上线后立即有大量 PV 写表,磁盘 / IO 抖动 | 极低 | 50 人 × 50 PV/天 = 2500 行/天,单表 22.5 万行,MySQL InnoDB buffer pool 32MB 起步足够 |
| R5 | `useAccessLog` 在生产 HMR 后保留旧 hook | 低 | module-level `installed` 标志,prod 模式无 HMR,dev 模式重置也只装 1 次 |
| R6 | ECharts 在窄屏下文字截断 | 低 | 响应式:窗口 < 640px 时把 grid.left 从 140 改为 100;实施时再细化 |
| R7 | KPI 卡副标题 `日均 X 次` 在 topPageAvg=null 时显示 `日均 null 次` | 中 | 模板 `v-if="data?.kpi.topPageAvg != null"` 包裹,无数据时不渲染副标题 |
| R8 | 前端 `apiService.postPageView` 失败 → 用户切换 100 个页面,100 个静默失败 | 低 | dev 模式 console.warn;生产环境无感知是预期(不影响功能) |
| R9 | `sys_page_view` 90 天后不清理,表持续增长 | 中 | 后续 PR 加 scheduler 兜底;本 spec 不阻塞上线 |
| R10 | `en.ts` 缺失,未来若用户切英文,新增 key 会缺翻译 | 中 | 维持项目双语言现状;若要英文支持单独排期补全 |
| R11 | `AuthInterceptor` 修改后,现有依赖 `request.getAttribute("userId")` 的代码可能受 admin 检查影响 | 低 | admin 检查在 preHandle **末尾**,只决定是否抛 403;不修改 userId / role attribute |
| R12 | App.vue 改 useAccessLog(),可能影响其他 onMounted 副作用 | 低 | 只在 onMounted 末尾追加一行,无副作用 |
| R13 | `services/api.ts` 加新方法后,类型 import 错误导致 vue-tsc 失败 | 低 | import 在文件顶部,IDE 实时校验,实施时跑 `npx vue-tsc` 兜底 |
| R14 | KPI 卡组件 KpiCard 不存在,临时 inline 又觉得丑 | 低 | 实施时先 inline;若 review 时觉得丑,再抽 |

## 10. 测试策略

### 10.1 后端单元测试(`backend/src/test/`,新增)

| 测试类 | 覆盖范围 | 用例数 |
|--------|---------|--------|
| `PageViewServiceTest` | 1) 校验 window 非法 2) 1d 窗口空数据 3) 1d 窗口有数据 4) 7d 窗口归一化正确(同 1d 数据,日均数值应为 1d 的 1/7)5) 90d 窗口空数据返 200 + 空 matrix 6) KPI 计算:topPage / peakHour 正确 | 6-8 |
| `PageViewControllerTest`(可选) | MockMvc 测:`POST /api/page-views` 缺 userId 返 401;pagePath 不以 / 开头返 400;正常入库 200 | 3 |

> 不引入 `@DataJpaTest` / H2,沿用项目做法(JUnit + Mockito),DB 部分靠 `mvn clean install` + 手动验收。

### 10.2 前端

按 `AGENTS.md` "no frontend test runner" 现状,**不**引入 Vitest。本期前端**零测试**,验证靠:
- `npx vue-tsc`(类型)
- `npm run build`(打包)
- 手动验收(见 §10.3)

### 10.3 集成 / 手工验收

| 验收步骤 | 期望 |
|---------|------|
| 1. 执行 `mysql -uroot -p db_webwbs < backend/add_page_view_table.sql` | 表创建成功,3 索引齐全 |
| 2. 启动后端,启动前端 | 无报错 |
| 3. 任意用户登录,在 14:00-15:00 切 3 个页面(Dashboard / Projects / Team) | `sys_page_view` 多 3 条;pageName=路由 name |
| 4. admin 登录,打开 /admin/access-heatmap,默认 7d | 看到 4 KPI + 热力图;Dashboard 行有 14 点格子深蓝(假设 §10.3.3 期间切到) |
| 5. 切窗口到 1d | 数据重渲染;今日 14 点深蓝;其他时间灰色 |
| 6. 切窗口到 90d | 数据重渲染;同一行 14 点日均变小(7d 是 1, 90d 是 7/90) |
| 7. 非 admin 用户(普通 member)打开 /admin/access-heatmap URL | 页面跳 /forbidden 或显示错误条(因 GET 返 403) |
| 8. hover 14 点格子 | tooltip 显示 "Projects · 14:00 / 日均 X 次" |
| 9. 关闭网络,刷新页面 | 4 KPI 全 "—",热力图区显示"统计服务暂不可用" + 刷新按钮 |
| 10. 切语言 zh → ko | 顶栏 / KPI / 底栏 / tooltip 文案全部变韩文 |
| 11. 跑 `mvn clean install -DskipTests` | 通过 |
| 12. 跑 `npx vue-tsc` | 通过 |
| 13. 跑 `npm run build` | 通过 |

## 11. 实施步骤

| # | 步骤 | 风险等级 | 顺序 |
|---|------|---------|------|
| 1 | 写 `backend/add_page_view_table.sql`,用户手动跑 | 低,CREATE IF NOT EXISTS | 第一 |
| 2 | 新建 `PageView.java` / `PageViewMapper.java` / `PageViewMapper.xml` | 低 | 第一 |
| 3 | 新建 `HeatmapResponse.java`(DTO) | 低 | 第一 |
| 4 | 新建 `PageViewService.java`(含校验 / 聚合 / KPI 计算) | 中(核心) | 第一 |
| 5 | 新建 `PageViewController.java`(写) + `AdminPageViewController.java`(读) | 低 | 第一 |
| 6 | 改 `AuthInterceptor.java`,preHandle 末尾加 admin 路径检查 | 低(小心改现有) | 第一 |
| 7 | 写 `PageViewServiceTest`(单测) | 低 | 第一 |
| 8 | 跑 `mvn clean install -DskipTests` 编译 | 校验 | 第一 |
| 9 | 跑 §10.1 单测 | 校验 | 第一 |
| 10 | 改 `types/index.ts`:加 `HeatmapResponse` / `HeatmapKpi` | 低 | 第二 |
| 11 | 改 `services/api.ts`:加 `postPageView` + `getHeatmap` | 低 | 第二 |
| 12 | 新建 `composables/useAccessLog.ts` | 低 | 第二 |
| 13 | 改 `App.vue`:加 `useAccessLog()` | 低 | 第二 |
| 14 | 新建 `views/AccessHeatmapView.vue` | 中(核心 UI) | 第二 |
| 15 | 改 `router/index.ts`:加 `/admin/access-heatmap` | 低 | 第二 |
| 16 | 改 `Sidebar.vue`:加 admin-only 菜单项 | 低 | 第二 |
| 17 | 改 `i18n/locales/zh.ts` + `ko.ts`:加 admin.accessHeatmap.* / sidebar.adminAccessHeatmap / routes.adminAccessHeatmap | 低 | 第二 |
| 18 | 跑 `npx vue-tsc` + `npm run build` | 校验 | 第二 |
| 19 | 跑 §10.3 手工验收 1-10 步 | 验收 | 第二 |

**部署顺序**(项目惯例):
- 先发后端(SQL + Java)
- 再发前端
- 天然灰度:前端不上线就无 PV 流量,但热力图页面可访问
- 回滚:revert 即可,后端数据可保留(下次再发不丢历史)
