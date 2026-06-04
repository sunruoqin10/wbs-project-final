# 定时任务管理页面 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 WBS 项目管理系统新增定时任务管理页面，支持可视化配置 cron 表达式、启停定时任务，仅 admin 角色可访问。

**Architecture:** 后端基于 `ThreadPoolTaskScheduler` 实现动态调度引擎 `SchedulerManager`，替代现有 `@Scheduled` 注解，配置持久化到 `sys_scheduler_config` 表。前端新增 `SchedulerManagement.vue` 页面和 `CronBuilder.vue` 可视化组件，通过路由守卫和侧边栏条件渲染实现 admin 权限控制。

**Tech Stack:** Spring Boot + MyBatis + ThreadPoolTaskScheduler / Vue 3 + TypeScript + Tailwind CSS + Pinia

---

## 文件结构

### 新建文件
| 文件 | 职责 |
|---|---|
| `backend/src/main/java/com/wbs/project/entity/SchedulerConfig.java` | 调度配置实体 |
| `backend/src/main/java/com/wbs/project/mapper/SchedulerConfigMapper.java` | 调度配置 Mapper 接口 |
| `backend/src/main/resources/mapper/SchedulerConfigMapper.xml` | MyBatis XML 映射 |
| `backend/src/main/java/com/wbs/project/scheduler/SchedulerManager.java` | 核心调度管理器 |
| `backend/src/main/java/com/wbs/project/controller/SchedulerConfigController.java` | 调度管理 REST 接口 |
| `backend/init_scheduler_config.sql` | 建表 + 初始数据 SQL |
| `frontend/src/types/scheduler.ts` | 前端 SchedulerConfig 类型 |
| `frontend/src/components/scheduler/CronBuilder.vue` | 可视化 Cron 构建器 |
| `frontend/src/views/SchedulerManagement.vue` | 定时任务管理页面 |

### 修改文件
| 文件 | 改动 |
|---|---|
| `backend/.../scheduler/DelayNotificationScheduler.java` | 移除 `@Scheduled`，实现 `Runnable` |
| `backend/.../controller/DelayNotificationController.java` | 重构为调用 SchedulerManager |
| `frontend/src/services/api.ts` | 新增 6 个调度管理 API 方法 |
| `frontend/src/router/index.ts` | 新增 `/scheduler-management` 路由 |
| `frontend/src/components/layout/Sidebar.vue` | 新增菜单项（仅 admin） |
| `frontend/src/components/layout/Header.vue` | 新增 pageTitle 映射 |
| `frontend/src/i18n/locales/zh.ts` | 新增中文翻译 |
| `frontend/src/i18n/locales/ko.ts` | 新增韩文翻译 |

---

### Task 1: 建表 SQL + SchedulerConfig 实体

**Files:**
- Create: `backend/init_scheduler_config.sql`
- Create: `backend/src/main/java/com/wbs/project/entity/SchedulerConfig.java`

- [ ] **Step 1: 编写建表 SQL**

```sql
-- init_scheduler_config.sql
CREATE TABLE IF NOT EXISTS sys_scheduler_config (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    cron_expression VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_run_time DATETIME,
    next_run_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_scheduler_config (id, name, description, cron_expression, enabled)
VALUES ('delay-notification', '延期通知', '每天检查延期任务并发送邮件通知', '0 0 9 * * ?', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);
```

- [ ] **Step 2: 编写 SchedulerConfig 实体**

```java
package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerConfig {
    private String id;
    private String name;
    private String description;
    private String cronExpression;
    private Boolean enabled;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### Task 2: SchedulerConfigMapper

**Files:**
- Create: `backend/src/main/java/com/wbs/project/mapper/SchedulerConfigMapper.java`
- Create: `backend/src/main/resources/mapper/SchedulerConfigMapper.xml`

- [ ] **Step 1: 编写 Mapper 接口**

```java
package com.wbs.project.mapper;

import com.wbs.project.entity.SchedulerConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SchedulerConfigMapper {

    List<SchedulerConfig> selectAll();

    SchedulerConfig selectById(@Param("id") String id);

    int insert(SchedulerConfig config);

    int update(SchedulerConfig config);

    int updateRunTime(@Param("id") String id,
                      @Param("lastRunTime") java.time.LocalDateTime lastRunTime,
                      @Param("nextRunTime") java.time.LocalDateTime nextRunTime);
}
```

- [ ] **Step 2: 编写 MyBatis XML 映射**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.wbs.project.mapper.SchedulerConfigMapper">

    <resultMap id="SchedulerConfigMap" type="com.wbs.project.entity.SchedulerConfig">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="description" property="description"/>
        <result column="cron_expression" property="cronExpression"/>
        <result column="enabled" property="enabled"/>
        <result column="last_run_time" property="lastRunTime"/>
        <result column="next_run_time" property="nextRunTime"/>
        <result column="created_at" property="createdAt"/>
        <result column="updated_at" property="updatedAt"/>
    </resultMap>

    <select id="selectAll" resultMap="SchedulerConfigMap">
        SELECT * FROM sys_scheduler_config ORDER BY id
    </select>

    <select id="selectById" resultMap="SchedulerConfigMap">
        SELECT * FROM sys_scheduler_config WHERE id = #{id}
    </select>

    <insert id="insert">
        INSERT INTO sys_scheduler_config (id, name, description, cron_expression, enabled, created_at, updated_at)
        VALUES (#{id}, #{name}, #{description}, #{cronExpression}, #{enabled}, NOW(), NOW())
    </insert>

    <update id="update">
        UPDATE sys_scheduler_config
        SET name = #{name},
            description = #{description},
            cron_expression = #{cronExpression},
            enabled = #{enabled},
            updated_at = NOW()
        WHERE id = #{id}
    </update>

    <update id="updateRunTime">
        UPDATE sys_scheduler_config
        SET last_run_time = #{lastRunTime},
            next_run_time = #{nextRunTime}
        WHERE id = #{id}
    </update>
</mapper>
```

---

### Task 3: 核心调度管理器 SchedulerManager

**Files:**
- Create: `backend/src/main/java/com/wbs/project/scheduler/SchedulerManager.java`

- [ ] **Step 1: 创建 SchedulerManager**

```java
package com.wbs.project.scheduler;

import com.wbs.project.entity.SchedulerConfig;
import com.wbs.project.mapper.SchedulerConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerManager {

    private final SchedulerConfigMapper schedulerConfigMapper;
    private final ApplicationContext applicationContext;

    private ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> taskBeanMap = Map.of(
        "delay-notification", "delayNotificationScheduler"
    );

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("scheduler-");
        taskScheduler.initialize();

        List<SchedulerConfig> configs = schedulerConfigMapper.selectAll();
        for (SchedulerConfig config : configs) {
            if (Boolean.TRUE.equals(config.getEnabled())) {
                startScheduler(config);
            }
        }
        log.info("SchedulerManager initialized, loaded {} configs", configs.size());
    }

    public List<SchedulerConfig> getAllConfigs() {
        return schedulerConfigMapper.selectAll();
    }

    public SchedulerConfig getConfig(String id) {
        return schedulerConfigMapper.selectById(id);
    }

    public SchedulerConfig updateConfig(SchedulerConfig config) {
        SchedulerConfig existing = schedulerConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new RuntimeException("调度任务不存在: " + config.getId());
        }

        boolean wasRunning = Boolean.TRUE.equals(existing.getEnabled());
        boolean willRun = Boolean.TRUE.equals(config.getEnabled());

        if (wasRunning) {
            stopSchedulerInternal(config.getId());
        }

        schedulerConfigMapper.update(config);
        SchedulerConfig updated = schedulerConfigMapper.selectById(config.getId());

        if (willRun) {
            startScheduler(updated);
        }

        return updated;
    }

    public void startScheduler(String id) {
        SchedulerConfig config = schedulerConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("调度任务不存在: " + id);
        }
        config.setEnabled(true);
        schedulerConfigMapper.update(config);
        startScheduler(config);
    }

    public void stopScheduler(String id) {
        SchedulerConfig config = schedulerConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("调度任务不存在: " + id);
        }
        config.setEnabled(false);
        schedulerConfigMapper.update(config);
        stopSchedulerInternal(id);
    }

    public void triggerNow(String id) {
        String beanName = taskBeanMap.get(id);
        if (beanName == null) {
            throw new RuntimeException("未知的调度任务: " + id);
        }
        Runnable task = (Runnable) applicationContext.getBean(beanName);
        try {
            task.run();
            schedulerConfigMapper.updateRunTime(id, LocalDateTime.now(), null);
            log.info("Task {} triggered manually", id);
        } catch (Exception e) {
            log.error("Error triggering task {}", id, e);
            throw new RuntimeException("任务执行失败: " + e.getMessage());
        }
    }

    private void startScheduler(SchedulerConfig config) {
        String beanName = taskBeanMap.get(config.getId());
        if (beanName == null) {
            log.warn("No bean mapping for scheduler: {}", config.getId());
            return;
        }

        try {
            Runnable task = (Runnable) applicationContext.getBean(beanName);
            ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(config.getCronExpression()));
            scheduledTasks.put(config.getId(), future);
            log.info("Scheduler {} started with cron: {}", config.getId(), config.getCronExpression());
        } catch (Exception e) {
            log.error("Failed to start scheduler: {}", config.getId(), e);
        }
    }

    private void stopSchedulerInternal(String id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            log.info("Scheduler {} stopped", id);
        }
    }
}
```

---

### Task 4: 改造 DelayNotificationScheduler

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/scheduler/DelayNotificationScheduler.java`

- [ ] **Step 1: 移除 @Scheduled 注解，实现 Runnable 接口**

将 `DelayNotificationScheduler.java` 中的类声明修改为：

```java
@Slf4j
@Component("delayNotificationScheduler")
@RequiredArgsConstructor
public class DelayNotificationScheduler implements Runnable {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final EmailNotificationService emailNotificationService;
    private final DelayNotificationRecordMapper delayNotificationRecordMapper;
```

移除 `@Scheduled(cron = "0 0 9 * * ?")` 注解。方法签名保持 `public void checkAndSendDelayNotifications()` 不变。

重写 `run()` 方法：

```java
@Override
public void run() {
    checkAndSendDelayNotifications();
}
```

保留 `checkAndSendDelayNotificationsManual()` 方法和 `recordNotification()` 方法不变。其余所有方法体保持不变。

---

### Task 5: 重构 DelayNotificationController

**Files:**
- Modify: `backend/src/main/java/com/wbs/project/controller/DelayNotificationController.java`

- [ ] **Step 1: 重构为调用 SchedulerManager**

```java
package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delay-notifications")
@RequiredArgsConstructor
public class DelayNotificationController {

    private final SchedulerManager schedulerManager;

    @PostMapping("/trigger")
    public Result<String> triggerDelayCheck() {
        schedulerManager.triggerNow("delay-notification");
        return Result.success("延期检查已触发");
    }
}
```

---

### Task 6: SchedulerConfigController

**Files:**
- Create: `backend/src/main/java/com/wbs/project/controller/SchedulerConfigController.java`

- [ ] **Step 1: 创建管理接口控制器**

```java
package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.SchedulerConfig;
import com.wbs.project.scheduler.SchedulerManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerConfigController {

    private final SchedulerManager schedulerManager;

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (role == null || !"admin".equals(role)) {
            throw new RuntimeException("仅管理员可操作");
        }
    }

    @GetMapping("/configs")
    public Result<List<SchedulerConfig>> getAllConfigs(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(schedulerManager.getAllConfigs());
    }

    @GetMapping("/configs/{id}")
    public Result<SchedulerConfig> getConfig(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(schedulerManager.getConfig(id));
    }

    @PutMapping("/configs/{id}")
    public Result<SchedulerConfig> updateConfig(@PathVariable String id,
                                                 @RequestBody SchedulerConfig config,
                                                 HttpServletRequest request) {
        checkAdmin(request);
        config.setId(id);
        return Result.success(schedulerManager.updateConfig(config));
    }

    @PostMapping("/{id}/start")
    public Result<String> startScheduler(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.startScheduler(id);
        return Result.success("任务已启动");
    }

    @PostMapping("/{id}/stop")
    public Result<String> stopScheduler(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.stopScheduler(id);
        return Result.success("任务已停止");
    }

    @PostMapping("/{id}/trigger")
    public Result<String> triggerNow(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.triggerNow(id);
        return Result.success("任务已手动触发");
    }
}
```

---

### Task 7: 前端 TypeScript 类型

**Files:**
- Create: `frontend/src/types/scheduler.ts`

- [ ] **Step 1: 创建 SchedulerConfig 类型**

```typescript
export interface SchedulerConfig {
  id: string;
  name: string;
  description: string;
  cronExpression: string;
  enabled: boolean;
  lastRunTime?: string;
  nextRunTime?: string;
  createdAt: string;
  updatedAt: string;
}
```

---

### Task 8: 前端 API 方法

**Files:**
- Modify: `frontend/src/services/api.ts`

在 `ApiService` 类中添加以下方法（放在 `// Helpers` 或类的末尾）：

- [ ] **Step 1: 添加调度管理 API 方法**

```typescript
import type { SchedulerConfig } from '@/types/scheduler';

// 在 ApiService 类中添加:
  async getSchedulerConfigs(): Promise<SchedulerConfig[]> {
    return request<SchedulerConfig[]>('/scheduler/configs');
  }

  async getSchedulerConfig(id: string): Promise<SchedulerConfig> {
    return request<SchedulerConfig>(`/scheduler/configs/${id}`);
  }

  async updateSchedulerConfig(id: string, data: Partial<SchedulerConfig>): Promise<SchedulerConfig> {
    return request<SchedulerConfig>(`/scheduler/configs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async startScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/start`, {
      method: 'POST',
    });
  }

  async stopScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/stop`, {
      method: 'POST',
    });
  }

  async triggerScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/trigger`, {
      method: 'POST',
    });
  }
```

导出语句也需修改：在文件末尾的 `export const apiService = new ApiService();` 之前，确保 `SchedulerConfig` 类型已在 api.ts 顶部 import。

---

### Task 9: CronBuilder 可视化组件

**Files:**
- Create: `frontend/src/components/scheduler/CronBuilder.vue`

- [ ] **Step 1: 创建 CronBuilder 组件**

```vue
<template>
  <div class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-secondary-700 mb-2">频率</label>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="option in frequencyOptions"
          :key="option.value"
          @click="selectFrequency(option.value)"
          :class="[
            'rounded-lg border-2 px-4 py-2 text-sm font-medium transition-all',
            frequency === option.value
              ? 'border-primary-500 bg-primary-50 text-primary-700'
              : 'border-secondary-200 text-secondary-600 hover:border-secondary-300'
          ]"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div v-if="frequency === 'weekly'" class="grid grid-cols-7 gap-2">
      <button
        v-for="(label, index) in weekDayLabels"
        :key="index"
        @click="toggleWeekDay(index)"
        :class="[
          'rounded-lg border-2 px-2 py-2 text-xs font-medium text-center transition-all',
          weekDays.includes(index)
            ? 'border-primary-500 bg-primary-50 text-primary-700'
            : 'border-secondary-200 text-secondary-500 hover:border-secondary-300'
        ]"
      >
        {{ label }}
      </button>
    </div>

    <div v-if="frequency === 'monthly'" class="grid grid-cols-7 gap-2">
      <button
        v-for="day in 31"
        :key="day"
        @click="monthDay = day"
        :class="[
          'rounded-lg border-2 px-1 py-2 text-xs font-medium text-center transition-all',
          monthDay === day
            ? 'border-primary-500 bg-primary-50 text-primary-700'
            : 'border-secondary-200 text-secondary-500 hover:border-secondary-300'
        ]"
      >
        {{ day }}
      </button>
    </div>

    <div>
      <label class="block text-sm font-medium text-secondary-700 mb-2">执行时间</label>
      <div class="flex items-center gap-2">
        <select
          v-model="hour"
          class="rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
        >
          <option v-for="h in 24" :key="h - 1" :value="h - 1">
            {{ String(h - 1).padStart(2, '0') }}
          </option>
        </select>
        <span class="text-secondary-500">:</span>
        <select
          v-model="minute"
          class="rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
        >
          <option v-for="m in 60" :key="m - 1" :value="m - 1">
            {{ String(m - 1).padStart(2, '0') }}
          </option>
        </select>
        <span class="text-sm text-secondary-500">时:分</span>
      </div>
    </div>

    <div class="rounded-lg bg-secondary-50 p-4">
      <div class="flex items-center justify-between">
        <span class="text-sm font-medium text-secondary-700">Cron 表达式</span>
        <code class="rounded bg-white px-3 py-1 text-sm font-mono text-primary-700 border border-secondary-200">
          {{ cronExpression }}
        </code>
      </div>
      <p class="mt-2 text-xs text-secondary-500">{{ humanReadable }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const frequencyOptions = [
  { value: 'daily', label: '每天' },
  { value: 'weekly', label: '每周' },
  { value: 'monthly', label: '每月' },
  { value: 'custom', label: '自定义' },
];

const weekDayLabels = ['日', '一', '二', '三', '四', '五', '六'];

const frequency = ref<'daily' | 'weekly' | 'monthly' | 'custom'>('daily');
const hour = ref(9);
const minute = ref(0);
const weekDays = ref<number[]>([1, 2, 3, 4, 5]);
const monthDay = ref(1);
const customCron = ref('');

const cronExpression = computed(() => {
  if (frequency.value === 'custom') {
    return customCron.value || props.modelValue;
  }
  const m = minute.value;
  const h = hour.value;
  if (frequency.value === 'daily') {
    return `0 ${m} ${h} * * ?`;
  }
  if (frequency.value === 'weekly') {
    const days = weekDays.value.length > 0 ? weekDays.value.join(',') : '*';
    return `0 ${m} ${h} ? * ${days}`;
  }
  if (frequency.value === 'monthly') {
    return `0 ${m} ${h} ${monthDay.value} * ?`;
  }
  return props.modelValue;
});

const humanReadable = computed(() => {
  const timeStr = `${String(hour.value).padStart(2, '0')}:${String(minute.value).padStart(2, '0')}`;
  if (frequency.value === 'daily') return `每天 ${timeStr} 执行`;
  if (frequency.value === 'weekly') {
    const names = weekDays.value.map(d => weekDayLabels[d]).join('、');
    return `每周${names} ${timeStr} 执行`;
  }
  if (frequency.value === 'monthly') return `每月 ${monthDay.value} 日 ${timeStr} 执行`;
  if (frequency.value === 'custom') return '自定义 Cron 表达式';
  return '';
});

function selectFrequency(val: string) {
  frequency.value = val as typeof frequency.value;
}

function toggleWeekDay(index: number) {
  const idx = weekDays.value.indexOf(index);
  if (idx === -1) {
    weekDays.value.push(index);
  } else {
    weekDays.value.splice(idx, 1);
  }
}

watch(cronExpression, (val) => {
  emit('update:modelValue', val);
});

watch(() => props.modelValue, (val) => {
  parseCron(val);
}, { immediate: true });

function parseCron(cron: string) {
  if (!cron) return;
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) {
    frequency.value = 'custom';
    customCron.value = cron;
    return;
  }
  const [, m, h, , month, dayOfWeek] = parts;
  minute.value = parseInt(m) || 0;
  hour.value = parseInt(h) || 0;

  if (dayOfWeek === '?' && month === '*') {
    frequency.value = 'daily';
  } else if (month === '*') {
    frequency.value = 'weekly';
    weekDays.value = dayOfWeek.split(',').map(Number).filter(n => !isNaN(n));
  } else {
    frequency.value = 'monthly';
    monthDay.value = parseInt(month) || 1;
  }
}
</script>
```

---

### Task 10: SchedulerManagement 页面

**Files:**
- Create: `frontend/src/views/SchedulerManagement.vue`

- [ ] **Step 1: 创建管理页面**

```vue
<template>
  <MainLayout>
    <div class="space-y-6">
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">{{ $t('scheduler.title') }}</h1>
        <p class="mt-1 text-sm text-secondary-600">{{ $t('scheduler.subtitle') }}</p>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card v-for="config in configs" :key="config.id">
          <div class="space-y-4">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="text-lg font-semibold text-secondary-900">{{ config.name }}</h3>
                <p class="mt-1 text-sm text-secondary-500">{{ config.description }}</p>
              </div>
              <Badge :variant="config.enabled ? 'success' : 'secondary'">
                {{ config.enabled ? $t('scheduler.running') : $t('scheduler.stopped') }}
              </Badge>
            </div>

            <div class="rounded-lg bg-secondary-50 p-3">
              <div class="flex items-center justify-between">
                <span class="text-sm text-secondary-500">Cron</span>
                <code class="rounded bg-white px-2 py-1 text-sm font-mono text-primary-700 border border-secondary-200">
                  {{ config.cronExpression }}
                </code>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-secondary-500">{{ $t('scheduler.lastRun') }}:</span>
                <span class="ml-1 text-secondary-700">{{ formatTime(config.lastRunTime) }}</span>
              </div>
              <div>
                <span class="text-secondary-500">{{ $t('scheduler.nextRun') }}:</span>
                <span class="ml-1 text-secondary-700">{{ formatTime(config.nextRunTime) }}</span>
              </div>
            </div>

            <div class="flex items-center gap-2 pt-2 border-t border-secondary-100">
              <Button
                v-if="config.enabled"
                variant="secondary"
                size="sm"
                @click="handleStop(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.stop') }}
              </Button>
              <Button
                v-else
                variant="primary"
                size="sm"
                @click="handleStart(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.start') }}
              </Button>
              <Button
                variant="secondary"
                size="sm"
                @click="handleTrigger(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.triggerNow') }}
              </Button>
              <Button
                variant="secondary"
                size="sm"
                @click="openEdit(config)"
              >
                {{ $t('scheduler.edit') }}
              </Button>
            </div>
          </div>
        </Card>
      </div>

      <div v-if="configs.length === 0 && !loading" class="text-center py-12 text-secondary-500">
        {{ $t('scheduler.empty') }}
      </div>
    </div>

    <Modal :open="editModalOpen" :title="$t('scheduler.edit')" size="lg" @close="editModalOpen = false">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('scheduler.cronExpression') }}</label>
          <CronBuilder v-model="editForm.cronExpression" />
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="secondary" @click="editModalOpen = false">{{ $t('common.cancel') }}</Button>
          <Button variant="primary" @click="handleSave" :loading="saving">
            {{ $t('common.save') }}
          </Button>
        </div>
      </div>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Modal from '@/components/common/Modal.vue';
import CronBuilder from '@/components/scheduler/CronBuilder.vue';
import apiService from '@/services/api';
import type { SchedulerConfig } from '@/types/scheduler';
import dayjs from 'dayjs';

const { t } = useI18n();

const configs = ref<SchedulerConfig[]>([]);
const loading = ref(false);
const actionLoading = ref<string | null>(null);
const editModalOpen = ref(false);
const saving = ref(false);
const editForm = ref<Partial<SchedulerConfig>>({});

const currentEditId = ref('');

function formatTime(time?: string) {
  if (!time) return t('scheduler.never');
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
}

async function loadConfigs() {
  loading.value = true;
  try {
    configs.value = await apiService.getSchedulerConfigs();
  } catch (e) {
    console.error('Failed to load scheduler configs', e);
  } finally {
    loading.value = false;
  }
}

async function handleStart(id: string) {
  actionLoading.value = id;
  try {
    await apiService.startScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

async function handleStop(id: string) {
  actionLoading.value = id;
  try {
    await apiService.stopScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

async function handleTrigger(id: string) {
  actionLoading.value = id;
  try {
    await apiService.triggerScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

function openEdit(config: SchedulerConfig) {
  currentEditId.value = config.id;
  editForm.value = {
    cronExpression: config.cronExpression,
    enabled: config.enabled,
  };
  editModalOpen.value = true;
}

async function handleSave() {
  saving.value = true;
  try {
    await apiService.updateSchedulerConfig(currentEditId.value, editForm.value);
    editModalOpen.value = false;
    await loadConfigs();
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadConfigs();
});
</script>
```

---

### Task 11: 路由配置

**Files:**
- Modify: `frontend/src/router/index.ts`

在 routes 数组中，`/settings` 路由之后添加：

- [ ] **Step 1: 添加路由**

```typescript
  {
    path: '/scheduler-management',
    name: 'SchedulerManagement',
    component: () => import('@/views/SchedulerManagement.vue'),
    meta: { titleKey: 'routes.schedulerManagement', permission: 'settings:edit' }
  },
```

---

### Task 12: 侧边栏菜单项

**Files:**
- Modify: `frontend/src/components/layout/Sidebar.vue`

在 `menuItems` 数组中，`settings` 菜单项之前添加：

- [ ] **Step 1: 添加菜单项（仅 admin 可见）**

```typescript
import { usePermissionStore } from '@/stores/permission';

// 在 script setup 中添加:
const permissionStore = usePermissionStore();

// 在 menuItems computed 中，settings 之前添加:
...(permissionStore.currentRole === 'admin' ? [{
  name: 'scheduler',
  label: t('routes.schedulerManagement'),
  to: '/scheduler-management',
  icon: '<svg class="h-6 w-6 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>'
}] : []),
```

注意：将现有 `Sidebar.vue` 中的 `menuItems` computed 从数组字面量改为在 settings 项之前插入上述条件项。

---

### Task 13: Header 页面标题

**Files:**
- Modify: `frontend/src/components/layout/Header.vue`

在 `routeKeyMap` 中添加：

- [ ] **Step 1: 添加页面标题映射**

```typescript
    SchedulerManagement: 'routes.schedulerManagement',
```

---

### Task 14: 国际化翻译

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`
- Modify: `frontend/src/i18n/locales/ko.ts`

- [ ] **Step 1: 中文翻译**

在 `zh.ts` 的 `routes` 对象中，`settings` 之后添加：

```typescript
    schedulerManagement: '定时任务管理',
```

在文件末尾的最后一个 key 后添加（注意 JSON 格式，不要多加逗号）：

```typescript
  scheduler: {
    title: '定时任务管理',
    subtitle: '管理系统定时任务的调度周期和启停状态',
    running: '运行中',
    stopped: '已停止',
    lastRun: '上次执行',
    nextRun: '下次执行',
    never: '从未执行',
    start: '启动',
    stop: '停止',
    triggerNow: '手动触发',
    edit: '编辑配置',
    cronExpression: 'Cron 表达式',
    empty: '暂无定时任务',
  },
```

- [ ] **Step 2: 韩文翻译**

在 `ko.ts` 的 `routes` 对象中，`settings` 之后添加：

```typescript
    schedulerManagement: '스케줄러 관리',
```

在文件末尾添加：

```typescript
  scheduler: {
    title: '스케줄러 관리',
    subtitle: '시스템 스케줄러의 주기와 상태를 관리합니다',
    running: '실행 중',
    stopped: '중지됨',
    lastRun: '마지막 실행',
    nextRun: '다음 실행',
    never: '실행 기록 없음',
    start: '시작',
    stop: '중지',
    triggerNow: '수동 실행',
    edit: '설정 편집',
    cronExpression: 'Cron 표현식',
    empty: '스케줄러가 없습니다',
  },
```

---

### Task 15: 连接 Agent Router（如果有的话）

确保 `/api/scheduler/**` 请求不受 `WebConfig.java` 中 auth 白名单限制（即这些接口需要认证），因此不需要修改 `WebConfig.java`。

### Task 16: 验证测试

- [ ] **Step 1: 前端类型检查**

```bash
cd frontend; npx vue-tsc
```

- [ ] **Step 2: 后端编译**

```bash
cd backend; mvn compile
```

- [ ] **Step 3: 手动验证清单**

1. 以 admin 角色登录，侧边栏应出现"定时任务管理"菜单项
2. 以非 admin 角色登录，侧边栏不应出现该菜单项
3. 进入页面能看到延期通知任务卡片
4. 点击"编辑配置"能用可视化构建器修改 cron 表达式并保存
5. 点击"停止"后任务状态变为"已停止"
6. 点击"启动"后任务状态变为"运行中"
7. 点击"手动触发"能立即执行一次延期检查
8. 非 admin 用户直接访问 `/scheduler-management` 应被重定向到 403 页面
