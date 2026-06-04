# 定时任务管理页面 - 设计文档

**日期**: 2026-06-02

## 概述

为 WBS 项目管理系统新增一个专用的定时任务管理页面，支持可视化配置调度周期、启动/停止定时任务，仅限管理员角色访问。

## 架构改动

### 后端

1. **新建 `sys_scheduler_config` 表**：存储调度任务配置
2. **新建 `SchedulerManager`**：基于 `ThreadPoolTaskScheduler` 的动态调度引擎，替代现有的 `@Scheduled` 注解
3. **改造 `DelayNotificationScheduler`**：移除 `@Scheduled`，改为由 `SchedulerManager` 统一调度
4. **新增 `SchedulerConfigController`**：管理接口（CRUD + 启动/停止/手动触发）
5. **所有管理接口需 admin 角色权限校验**

### 前端

1. **新增 `SchedulerManagement.vue`** 页面：卡片式任务列表
2. **新增 `CronBuilder.vue`** 组件：可视化构建 cron 表达式
3. **路由**：`/scheduler-management`，`meta: { permission: 'settings:edit' }`（仅 admin）
4. **侧边栏**：新增菜单项，仅 admin 可见

## 数据库设计

### sys_scheduler_config

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(32) | 主键 |
| name | VARCHAR(100) | 任务名称 |
| description | VARCHAR(255) | 任务描述 |
| cron_expression | VARCHAR(50) | cron 表达式 |
| enabled | TINYINT(1) | 是否启用，默认 1 |
| last_run_time | DATETIME | 上次执行时间 |
| next_run_time | DATETIME | 下次执行时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## API 设计

```
GET    /api/scheduler/configs        - 获取所有调度任务配置
GET    /api/scheduler/configs/{id}   - 获取单个配置
PUT    /api/scheduler/configs/{id}   - 更新配置 (cron/enabled)
POST   /api/scheduler/{id}/start     - 启动任务
POST   /api/scheduler/{id}/stop      - 停止任务
POST   /api/scheduler/{id}/trigger   - 手动触发一次
```

## 前端页面设计

### SchedulerManagement.vue
- 卡片式任务列表，每个卡片显示：名称、描述、cron、状态、上次/下次执行时间
- 操作按钮：启动/停止、手动触发、编辑
- 编辑弹窗：CronBuilder + 实时预览

### CronBuilder.vue
- 频率选择：每天/每周/每月/自定义
- 时间选择器
- 实时预览 cron 表达式和人类可读描述

### 权限控制
- 前端路由 `permission: 'settings:edit'`（仅 admin 拥有）
- 侧边栏菜单项仅 admin 可见
- 后端接口检查用户角色为 admin
