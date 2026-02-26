# 报表统计页面Tab页设计方案

## 一、功能概述

将当前的报表统计页面重构为Tab页结构，包含三个Tab页：

* **Tab 1**: 报表统计（保留当前功能）

* **Tab 2**: 本周项目进度报告（新增功能）

* **Tab 3**: 月报项目进度报告（新增功能）

## 二、Tab页结构设计

### 2.1 整体布局

```
┌─────────────────────────────────────────────────────┐
│  页面标题：报表统计                                    │
│  ┌───────────────────────────────────────────────┐  │
│  │ [报表统计] [本周项目进度报告] [月报项目进度报告]   │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  Tab页内容区域                                       │
│  ┌───────────────────────────────────────────────┐  │
│  │                                               │  │
│  │        当前Tab页的具体内容                      │  │
│  │                                               │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 2.2 Tab切换组件设计

使用Tailwind CSS实现Tab切换，支持：

* 响应式设计

* 平滑过渡动画

* 激活状态高亮显示

* 底部指示条

## 三、Tab 1：报表统计

### 3.1 功能描述

保留当前的报表统计功能，包括：

* 概览统计卡片

* 图表展示

* 导出功能

### 3.2 内容结构

```
┌─────────────────────────────────────────────────────┐
│ 概览统计                                             │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │项目总数  │ │完成率    │ │任务总数  │ │进行中    │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
├─────────────────────────────────────────────────────┤
│ 图表展示                                             │
│ ┌────────────────────┐ ┌────────────────────┐     │
│ │ 项目状态分布        │ │ 任务优先级分布      │     │
│ └────────────────────┘ └────────────────────┘     │
│ ┌────────────────────┐ ┌────────────────────┐     │
│ │ 项目完成进度        │ │ 团队绩效          │     │
│ └────────────────────┘ └────────────────────┘     │
├─────────────────────────────────────────────────────┤
│ 导出选项                                             │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │项目Excel │ │统计Excel │ │综合报表  │             │
│ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────┘
```

## 四、Tab 2：本周项目进度报告

### 4.1 功能描述

展示本周的项目进度报告，包含四个主要模块：

1. **项目汇总** - 本周项目整体情况
2. **任务详情** - 本周任务完成情况
3. **团队绩效** - 本周团队成员表现
4. **下周计划** - 下周工作安排

### 4.2 页面头部信息

```
┌─────────────────────────────────────────────────────┐
│ 本周项目进度报告                                      │
│ ┌───────────────────────────────────────────────┐  │
│ │ 报告周期：2024-01-15 ~ 2024-01-21              │  │
│ │ 生成时间：2024-01-21 14:30                      │  │
│ │ [导出周报] [刷新数据]                           │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 4.3 模块1：项目汇总

#### 4.3.1 统计卡片

```
┌─────────────────────────────────────────────────────┐
│ 项目汇总                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │活跃项目  │ │新增项目  │ │完成项目  │ │延期项目  │ │
│ │    5     │ │    2     │ │    3     │ │    1     │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
└─────────────────────────────────────────────────────┘
```

#### 4.3.2 项目列表表格

| 项目名称    | 负责人 | 进度   | 本周任务  | 状态  | 操作 |
| ------- | --- | ---- | ----- | --- | -- |
| WBS系统开发 | 张三  | 85%  | 12/15 | 进行中 | 查看 |
| 用户界面优化  | 李四  | 60%  | 8/10  | 进行中 | 查看 |
| 数据库迁移   | 王五  | 100% | 5/5   | 已完成 | 查看 |

### 4.4 模块2：任务详情

#### 4.4.1 任务统计图表

```
┌─────────────────────────────────────────────────────┐
│ 本周任务统计                                         │
├─────────────────────────────────────────────────────┤
│ ┌────────────────────┐ ┌────────────────────┐     │
│ │ 任务状态分布        │ │ 任务优先级分布      │     │
│ │ (饼图)             │ │ (饼图)             │     │
│ └────────────────────┘ └────────────────────┘     │
└─────────────────────────────────────────────────────┘
```

#### 4.4.2 任务列表表格

| 任务名称    | 所属项目    | 负责人 | 优先级 | 截止日期       | 状态  |
| ------- | ------- | --- | --- | ---------- | --- |
| 实现登录功能  | WBS系统开发 | 张三  | 高   | 2024-01-20 | 已完成 |
| 优化首页加载  | WBS系统开发 | 李四  | 中   | 2024-01-21 | 进行中 |
| 编写API文档 | 数据库迁移   | 王五  | 低   | 2024-01-22 | 未开始 |

### 4.5 模块3：团队绩效

#### 4.5.1 绩效统计卡片

```
┌─────────────────────────────────────────────────────┐
│ 团队绩效                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │本周完成任务│ │平均效率  │ │加班时长  │             │
│ │   45个    │ │  92%     │ │ 12小时   │             │
│ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────┘
```

#### 4.5.2 成员绩效排行

```
┌─────────────────────────────────────────────────────┐
│ 成员绩效排行榜                                       │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 🥇 张三   | 完成:15 | 效率:98% | 加班:3h     │  │
│ │ 🥈 李四   | 完成:12 | 效率:95% | 加班:5h     │  │
│ │ 🥉 王五   | 完成:10 | 效率:88% | 加班:2h     │  │
│ │ 4. 赵六   | 完成:8  | 效率:85% | 加班:2h     │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

#### 4.5.3 绩效趋势图

```
┌─────────────────────────────────────────────────────┐
│ 本周每日完成任务趋势                                 │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │     (折线图 - X轴:周一到周日, Y轴:任务数)      │  │
│ │                                               │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 4.6 模块4：下周计划

#### 4.6.1 计划概览

```
┌─────────────────────────────────────────────────────┐
│ 下周计划                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │计划任务  │ │重点项目  │ │关键里程碑 │             │
│ │   50个   │ │   3个    │ │   2个    │             │
│ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────┘
```

#### 4.6.2 下周任务安排

| 任务名称   | 所属项目    | 负责人 | 优先级 | 计划开始       | 计划结束       |
| ------ | ------- | --- | --- | ---------- | ---------- |
| 完成单元测试 | WBS系统开发 | 张三  | 高   | 2024-01-22 | 2024-01-24 |
| 实现权限管理 | WBS系统开发 | 李四  | 高   | 2024-01-22 | 2024-01-25 |
| 性能优化   | 用户界面优化  | 王五  | 中   | 2024-01-23 | 2024-01-26 |

#### 4.6.3 关键里程碑

```
┌─────────────────────────────────────────────────────┐
│ 关键里程碑                                           │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 🔴 2024-01-23: Beta版本发布                   │  │
│ │ 🟢 2024-01-25: 用户验收测试                    │  │
│ │ 🔵 2024-01-26: 正式上线                        │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## 五、Tab 3：月报项目进度报告

### 5.1 功能描述

展示本月的项目进度报告，包含五个主要模块：

1. **月度概况** - 本月项目整体情况
2. **项目进展** - 本月各项目详细进展
3. **任务统计** - 本月任务完成情况统计
4. **团队绩效** - 本月团队成员表现
5. **下月计划** - 下月工作安排

### 5.2 页面头部信息

```
┌─────────────────────────────────────────────────────┐
│ 月报项目进度报告                                      │
│ ┌───────────────────────────────────────────────┐  │
│ │ 报告周期：2024-01-01 ~ 2024-01-31              │  │
│ │ 生成时间：2024-01-31 14:30                      │  │
│ │ 月份选择：[◀ 2024年1月 ▶]                      │  │
│ │ [导出月报] [刷新数据]                           │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 5.3 模块1：月度概况

#### 5.3.1 统计卡片

```
┌─────────────────────────────────────────────────────┐
│ 月度概况                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │活跃项目  │ │新增项目  │ │完成项目  │ │延期项目  │ │
│ │    8     │ │    3     │ │    5     │ │    2     │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │本月任务  │ │已完成    │ │进行中    │ │未开始    │ │
│ │  120个   │ │   85个   │ │   25个   │ │   10个   │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
└─────────────────────────────────────────────────────┘
```

#### 5.3.2 月度趋势图表

```
┌─────────────────────────────────────────────────────┐
│ 月度趋势分析                                         │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │     (折线图 - X轴:1月到12月, Y轴:完成任务数)     │  │
│ │     展示全年任务完成趋势                         │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 5.4 模块2：项目进展

#### 5.4.1 项目进度概览

```
┌─────────────────────────────────────────────────────┐
│ 项目进度概览                                         │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 项目名称      │ 进度  │ 状态   │ 本月进度变化  │  │
│ │ WBS系统开发   │ 85%   │ 进行中 │ +15%         │  │
│ │ 用户界面优化  │ 60%   │ 进行中 │ +20%         │  │
│ │ 数据库迁移    │ 100%  │ 已完成 │ +30%         │  │
│ │ API开发       │ 40%   │ 进行中 │ +10%         │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

#### 5.4.2 项目进度条

```
┌─────────────────────────────────────────────────────┐
│ 各项目本月进度详情                                   │
├─────────────────────────────────────────────────────┤
│ WBS系统开发                                           │
│ ████████████████████░░░░░░░░ 85% (完成136/160任务)  │
│ 用户界面优化                                          │
│ ████████████░░░░░░░░░░░░░░░░ 60% (完成72/120任务)  │
│ 数据库迁移                                            │
│ ████████████████████████████ 100% (完成50/50任务)   │
└─────────────────────────────────────────────────────┘
```

### 5.5 模块3：任务统计

#### 5.5.1 任务统计图表

```
┌─────────────────────────────────────────────────────┐
│ 本月任务统计                                         │
├─────────────────────────────────────────────────────┤
│ ┌────────────────────┐ ┌────────────────────┐     │
│ │ 任务状态分布        │ │ 任务优先级分布      │     │
│ │ (饼图)             │ │ (饼图)             │     │
│ └────────────────────┘ └────────────────────┘     │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 每周完成任务数 (柱状图)                         │  │
│ │ X轴: 第1-4周, Y轴: 任务数                      │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

#### 5.5.2 任务列表表格

| 任务名称    | 所属项目    | 负责人 | 优先级 | 完成日期       | 状态  |
| ------- | ------- | --- | --- | ---------- | --- |
| 完成登录功能  | WBS系统开发 | 张三  | 高   | 2024-01-15 | 已完成 |
| 实现权限管理  | WBS系统开发 | 李四  | 高   | 2024-01-20 | 已完成 |
| 优化首页加载  | 用户界面优化  | 王五  | 中   | 2024-01-25 | 已完成 |
| 编写API文档 | 数据库迁移   | 赵六  | 低   | 2024-01-28 | 已完成 |

### 5.6 模块4：团队绩效

#### 5.6.1 绩效统计卡片

```
┌─────────────────────────────────────────────────────┐
│ 团队绩效                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │本月完成任务│ │平均效率  │ │加班时长  │             │
│ │  185个   │ │  90%     │ │ 48小时   │             │
│ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────┘
```

#### 5.6.2 成员绩效排行

```
┌─────────────────────────────────────────────────────┐
│ 本月成员绩效排行榜                                   │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 🥇 张三   | 完成:52 | 效率:95% | 加班:12h    │  │
│ │ 🥈 李四   | 完成:48 | 效率:92% | 加班:15h    │  │
│ │ 🥉 王五   | 完成:45 | 效率:88% | 加班:10h    │  │
│ │ 4. 赵六   | 完成:40 | 效率:85% | 加班:11h    │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

#### 5.6.3 绩效趋势图

```
┌─────────────────────────────────────────────────────┐
│ 本月每周绩效趋势                                     │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │     (折线图 - X轴:第1-4周, Y轴:完成任务数)      │  │
│ │     展示每周任务完成数量变化趋势                 │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 5.7 模块5：下月计划

#### 5.7.1 计划概览

```
┌─────────────────────────────────────────────────────┐
│ 下月计划                                             │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │计划任务  │ │重点项目  │ │关键里程碑 │             │
│ │  150个   │ │   5个    │ │   3个    │             │
│ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────┘
```

#### 5.7.2 下月任务安排

| 任务名称   | 所属项目    | 负责人 | 优先级 | 计划开始       | 计划结束       |
| ------ | ------- | --- | --- | ---------- | ---------- |
| 完成系统集成 | WBS系统开发 | 张三  | 高   | 2024-02-01 | 2024-02-15 |
| 性能优化测试 | WBS系统开发 | 李四  | 高   | 2024-02-01 | 2024-02-20 |
| 用户培训   | 用户界面优化  | 王五  | 中   | 2024-02-10 | 2024-02-25 |
| 部署上线   | 数据库迁移   | 赵六  | 高   | 2024-02-25 | 2024-02-28 |

#### 5.7.3 关键里程碑

```
┌─────────────────────────────────────────────────────┐
│ 下月关键里程碑                                       │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────┐  │
│ │ 🔴 2024-02-10: 系统测试完成                  │  │
│ │ 🟢 2024-02-20: 用户验收通过                  │  │
│ │ 🔵 2024-02-28: 项目正式交付                  │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## 五点五、任务判断标准说明

### 5.5.0 任务统计基础规则

在周报和月报中，所有任务统计都遵循以下基础规则：

**叶子任务定义**：

* 叶子任务是指没有子任务的任务（即 `parentTaskId` 为空或不存在其他任务以其为父任务）

* 统计时只统计叶子任务，避免重复计算

**统计规则**：

1. **优先统计叶子任务**：当任务存在子任务时，只统计其叶子子任务
2. **根任务例外**：如果一个项目只有一个根任务且该根任务没有任何叶子任务，则该根任务作为叶子任务参与统计

**叶子任务筛选逻辑**：

```typescript
function getLeafTasks(tasks: any[]) {
  // 收集所有任务ID
  const allTaskIds = new Set(tasks.map(t => t.id));
  
  // 收集所有作为父任务的任务ID
  const parentTaskIds = new Set(
    tasks
      .filter(t => t.parentTaskId)
      .map(t => t.parentTaskId)
  );
  
  // 叶子任务 = 所有任务 - 父任务
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));
  
  // 特殊情况：如果只有一个任务，且它是根任务（没有父任务）
  if (tasks.length === 1 && leafTasks.length === 0) {
    return tasks;
  }
  
  return leafTasks;
}
```

**统计流程**：

1. 首先从所有任务中筛选出叶子任务
2. 然后根据时间范围判断叶子任务属于本周/本月
3. 最后按照任务状态进行分类统计

**示例场景**：

```
项目任务树：
├── 根任务A (非叶子任务)
│   ├── 子任务A1 (叶子任务) ✓ 统计
│   ├── 子任务A2 (叶子任务) ✓ 统计
│   └── 子任务A3 (叶子任务) ✓ 统计
└── 根任务B (叶子任务) ✓ 统计

统计结果：4个叶子任务（A1, A2, A3, B）
不统计：根任务A（因为它是父任务）

特殊场景：
└── 根任务C (叶子任务) ✓ 统计（因为它是唯一任务且无子任务）

统计结果：1个叶子任务（C）
```

### 5.5.1 本周任务判断标准

本周任务是指在本周时间范围内（周一至周日）满足以下任一条件的任务：

**判断规则**：

1. **已完成任务**：任务的 `completedAt` 字段时间在本周时间范围内
2. **进行中任务**：任务的 `createdAt` 字段时间在本周时间范围内，且状态为 `in-progress`
3. **未开始任务**：任务的 `createdAt` 字段时间在本周时间范围内，且状态为 `pending`

**时间范围计算**：

```typescript
const now = dayjs(date);
const weekStart = now.startOf('week').format('YYYY-MM-DD'); // 周一 00:00:00
const weekEnd = now.endOf('week').format('YYYY-MM-DD');   // 周日 23:59:59
const weekNumber = now.weekOfYear();
```

**任务筛选逻辑**：

```typescript
function getWeeklyTasks(tasks: any[], weekRange: any) {
  const { weekStart, weekEnd } = weekRange;
  
  // 第一步：筛选出叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  // 第二步：判断叶子任务是否在本周范围内
  return leafTasks.filter(task => {
    // 优先使用完成时间，如果没有则使用创建时间
    const taskDate = dayjs(task.completedAt || task.createdAt);
    
    // 判断任务日期是否在本周范围内（包含边界）
    return taskDate.isBetween(weekStart, weekEnd, null, '[]');
  });
}
```

### 5.5.2 下周任务判断标准

下周任务是指计划在下周时间范围内（下周一至下周日）执行的任务。

**判断规则**：

1. **计划任务**：任务的 `plannedStart` 字段时间在下周时间范围内
2. **延期任务**：任务的 `dueDate` 字段时间在下周时间范围内，且状态不是 `done`

**时间范围计算**：

```typescript
const now = dayjs(date);
const nextWeekStart = now.add(1, 'week').startOf('week').format('YYYY-MM-DD');
const nextWeekEnd = now.add(1, 'week').endOf('week').format('YYYY-MM-DD');
```

**任务筛选逻辑**：

```typescript
function getNextWeekTasks(tasks: any[]) {
  const now = dayjs();
  const nextWeekStart = now.add(1, 'week').startOf('week');
  const nextWeekEnd = now.add(1, 'week').endOf('week');
  
  // 第一步：筛选出叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  // 第二步：判断叶子任务是否在下周范围内
  return leafTasks.filter(task => {
    const plannedStart = dayjs(task.plannedStart);
    const dueDate = dayjs(task.dueDate);
    
    // 计划开始时间在下周范围内，或截止时间在下周范围内且未完成
    return (plannedStart.isBetween(nextWeekStart, nextWeekEnd, null, '[]')) ||
           (task.status !== 'done' && dueDate.isBetween(nextWeekStart, nextWeekEnd, null, '[]'));
  });
}
```

### 5.5.3 月度任务判断标准

月度任务是指在本月时间范围内（1日至月末）满足以下任一条件的任务。

**判断规则**：

1. **已完成任务**：任务的 `completedAt` 字段时间在本月时间范围内
2. **进行中任务**：任务的 `createdAt` 字段时间在本月时间范围内，且状态为 `in-progress`
3. **未开始任务**：任务的 `createdAt` 字段时间在本月时间范围内，且状态为 `pending`

**时间范围计算**：

```typescript
const now = dayjs(date);
const monthStart = now.startOf('month').format('YYYY-MM-DD'); // 本月1日 00:00:00
const monthEnd = now.endOf('month').format('YYYY-MM-DD');   // 本月最后一天 23:59:59
```

**任务筛选逻辑**：

```typescript
function getMonthlyTasks(tasks: any[], monthRange: any) {
  const { monthStart, monthEnd } = monthRange;
  
  // 第一步：筛选出叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  // 第二步：判断叶子任务是否在本月范围内
  return leafTasks.filter(task => {
    const taskDate = dayjs(task.completedAt || task.createdAt);
    
    // 判断任务日期是否在本月范围内（包含边界）
    return taskDate.isBetween(monthStart, monthEnd, null, '[]');
  });
}
```

### 5.5.4 下月任务判断标准

下月任务是指计划在下月时间范围内（下月1日至下月月末）执行的任务。

**判断规则**：

1. **计划任务**：任务的 `plannedStart` 字段时间在下月时间范围内
2. **延期任务**：任务的 `dueDate` 字段时间在下月时间范围内，且状态不是 `done`

**时间范围计算**：

```typescript
const now = dayjs(date);
const nextMonthStart = now.add(1, 'month').startOf('month').format('YYYY-MM-DD');
const nextMonthEnd = now.add(1, 'month').endOf('month').format('YYYY-MM-DD');
```

**任务筛选逻辑**：

```typescript
function getNextMonthTasks(tasks: any[]) {
  const now = dayjs();
  const nextMonthStart = now.add(1, 'month').startOf('month');
  const nextMonthEnd = now.add(1, 'month').endOf('month');
  
  // 第一步：筛选出叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  // 第二步：判断叶子任务是否在下月范围内
  return leafTasks.filter(task => {
    const plannedStart = dayjs(task.plannedStart);
    const dueDate = dayjs(task.dueDate);
    
    // 计划开始时间在下月范围内，或截止时间在下月范围内且未完成
    return (plannedStart.isBetween(nextMonthStart, nextMonthEnd, null, '[]')) ||
           (task.status !== 'done' && dueDate.isBetween(nextMonthStart, nextMonthEnd, null, '[]'));
  });
}
```

### 5.5.5 边界情况处理

**跨周任务**：

* 如果任务创建时间是上周，但完成时间是本周 → 归入本周任务

* 如果任务创建时间是本周，但完成时间是下周 → 归入本周任务（进行中）

* 如果任务创建时间是下周 → 归入下周任务

**跨月任务**：

* 如果任务创建时间是上个月，但完成时间是本月 → 归入本月任务

* 如果任务创建时间是本月，但完成时间是下月 → 归入本月任务（进行中）

* 如果任务创建时间是下月 → 归入下月任务

**优先级规则**：

* `completedAt` 时间优先于 `createdAt` 时间

* `plannedStart` 时间优先于 `dueDate` 时间

## 六、数据结构设计

### 6.1 本周报告数据结构

```typescript
interface WeeklyReportData {
  // 报告基本信息
  reportInfo: {
    weekStart: string;      // 本周开始日期
    weekEnd: string;        // 本周结束日期
    generatedAt: string;    // 生成时间
    weekNumber: number;     // 周数
  };

  // 项目汇总
  projectSummary: {
    activeProjects: number;      // 活跃项目数
    newProjects: number;         // 新增项目数
    completedProjects: number;    // 完成项目数
    delayedProjects: number;     // 延期项目数
    projectList: ProjectInfo[];  // 项目列表
  };

  // 任务详情
  taskDetails: {
    totalTasks: number;          // 本周任务总数（仅统计叶子任务）
    completedTasks: number;      // 已完成任务数（仅统计叶子任务）
    inProgressTasks: number;     // 进行中任务数（仅统计叶子任务）
    pendingTasks: number;        // 未开始任务数（仅统计叶子任务）
    taskList: TaskInfo[];        // 任务列表（仅包含叶子任务）
  };

  // 团队绩效
  teamPerformance: {
    totalCompleted: number;      // 本周完成总任务数
    avgEfficiency: number;       // 平均效率
    totalOvertimeHours: number;  // 总加班时长
    memberRankings: MemberPerformance[];  // 成员绩效排行
    dailyTrend: DailyTaskCount[]; // 每日完成任务趋势
  };

  // 下周计划
  nextWeekPlan: {
    plannedTasks: number;        // 计划任务数
    keyProjects: number;         // 重点项目数
    milestones: Milestone[];     // 里程碑
    taskList: PlannedTask[];     // 下周任务列表
  };
}

interface ProjectInfo {
  id: string;
  name: string;
  owner: string;
  progress: number;
  weekTasksCompleted: number;
  weekTasksTotal: number;
  status: string;
  delayDays?: number;
}

interface TaskInfo {
  id: string;
  name: string;
  projectName: string;
  assignee: string;
  priority: string;
  dueDate: string;
  status: string;
  completedAt?: string;
}

interface MemberPerformance {
  userId: string;
  userName: string;
  completedTasks: number;
  efficiency: number;
  overtimeHours: number;
  ranking: number;
}

interface DailyTaskCount {
  date: string;
  weekday: string;
  completedCount: number;
}

interface Milestone {
  id: string;
  title: string;
  date: string;
  status: 'pending' | 'in-progress' | 'completed';
  priority: 'high' | 'medium' | 'low';
}

interface PlannedTask {
  id: string;
  name: string;
  projectName: string;
  assignee: string;
  priority: string;
  plannedStart: string;
  plannedEnd: string;
}
```

### 6.2 月报报告数据结构

```typescript
interface MonthlyReportData {
  // 报告基本信息
  reportInfo: {
    monthStart: string;      // 本月开始日期
    monthEnd: string;        // 本月结束日期
    generatedAt: string;     // 生成时间
    year: number;            // 年份
    month: number;           // 月份
    monthName: string;       // 月份名称（如：1月）
  };

  // 月度概况
  monthlyOverview: {
    activeProjects: number;       // 活跃项目数
    newProjects: number;          // 新增项目数
    completedProjects: number;    // 完成项目数
    delayedProjects: number;      // 延期项目数
    totalTasks: number;          // 本月任务总数（仅统计叶子任务）
    completedTasks: number;       // 已完成任务数（仅统计叶子任务）
    inProgressTasks: number;      // 进行中任务数（仅统计叶子任务）
    pendingTasks: number;         // 未开始任务数（仅统计叶子任务）
  };

  // 项目进展
  projectProgress: {
    projectList: MonthlyProjectInfo[];  // 项目列表
    monthlyTrend: MonthlyTrend[];       // 月度趋势
  };

  // 任务统计
  taskStatistics: {
    taskList: MonthlyTaskInfo[];        // 任务列表
    weeklyTaskCount: WeeklyTaskCount[]; // 每周任务统计
  };

  // 团队绩效
  teamPerformance: {
    totalCompleted: number;              // 本月完成总任务数
    avgEfficiency: number;               // 平均效率
    totalOvertimeHours: number;          // 总加班时长
    memberRankings: MonthlyMemberPerformance[];  // 成员绩效排行
    weeklyTrend: WeeklyTrend[];         // 每周绩效趋势
  };

  // 下月计划
  nextMonthPlan: {
    plannedTasks: number;        // 计划任务数
    keyProjects: number;         // 重点项目数
    milestones: Milestone[];     // 里程碑
    taskList: PlannedTask[];    // 下月任务列表
  };
}

interface MonthlyProjectInfo {
  id: string;
  name: string;
  owner: string;
  progress: number;
  monthProgressChange: number;  // 本月进度变化（百分比）
  monthTasksCompleted: number;  // 本月完成任务数（仅统计叶子任务）
  monthTasksTotal: number;       // 本月任务总数（仅统计叶子任务）
  status: string;
  delayDays?: number;
}

interface MonthlyTrend {
  month: string;               // 月份名称
  year: number;                // 年份
  completedTasks: number;      // 完成任务数
}

interface MonthlyTaskInfo {
  id: string;
  name: string;
  projectName: string;
  assignee: string;
  priority: string;
  completedAt: string;
  status: string;
}

interface WeeklyTaskCount {
  weekNumber: number;           // 周数（1-4）
  weekStart: string;
  weekEnd: string;
  completedCount: number;
}

interface MonthlyMemberPerformance {
  userId: string;
  userName: string;
  completedTasks: number;
  efficiency: number;
  overtimeHours: number;
  ranking: number;
}

interface WeeklyTrend {
  weekNumber: number;
  weekStart: string;
  weekEnd: string;
  completedCount: number;
}
```

### 6.3 数据计算逻辑

#### 6.3.1 获取本周日期范围

```typescript
import dayjs from 'dayjs';
import weekday from 'dayjs/plugin/weekday';
import weekOfYear from 'dayjs/plugin/weekOfYear';

dayjs.extend(weekday);
dayjs.extend(weekOfYear);

function getWeekRange(date: Date = new Date()) {
  const now = dayjs(date);
  const weekStart = now.startOf('week').format('YYYY-MM-DD');
  const weekEnd = now.endOf('week').format('YYYY-MM-DD');
  const weekNumber = now.weekOfYear();
  
  return { weekStart, weekEnd, weekNumber };
}
```

#### 6.3.2 获取本月日期范围

```typescript
function getMonthRange(date: Date = new Date()) {
  const now = dayjs(date);
  const monthStart = now.startOf('month').format('YYYY-MM-DD');
  const monthEnd = now.endOf('month').format('YYYY-MM-DD');
  const year = now.year();
  const month = now.month() + 1;
  const monthName = now.format('MMMM');
  
  return { monthStart, monthEnd, year, month, monthName };
}
```

#### 6.3.3 计算项目汇总

```typescript
function calculateProjectSummary(projects: any[], tasks: any[], weekRange: any) {
  const { weekStart, weekEnd } = weekRange;
  
  // 第一步：筛选出所有叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  const projectList = projects.map(project => {
    // 获取该项目的所有叶子任务
    const projectLeafTasks = leafTasks.filter(t => t.projectId === project.id);
    const weekTasks = projectLeafTasks.filter(t => {
      const taskDate = dayjs(t.completedAt || t.createdAt);
      return taskDate.isBetween(weekStart, weekEnd, null, '[]');
    });
    
    return {
      id: project.id,
      name: project.name,
      owner: project.ownerId,
      progress: project.progress,
      weekTasksCompleted: weekTasks.filter(t => t.status === 'done').length,
      weekTasksTotal: weekTasks.length,
      status: project.status,
      delayDays: project.delayDays
    };
  });
  
  return {
    activeProjects: projectList.filter(p => p.status === 'active').length,
    newProjects: projectList.filter(p => {
      return dayjs(p.createdAt).isBetween(weekStart, weekEnd, null, '[]');
    }).length,
    completedProjects: projectList.filter(p => p.status === 'completed').length,
    delayedProjects: projectList.filter(p => p.delayDays > 0).length,
    projectList
  };
}
```

#### 6.3.4 计算月报项目汇总

```typescript
function calculateMonthlyProjectSummary(projects: any[], tasks: any[], monthRange: any) {
  const { monthStart, monthEnd } = monthRange;
  
  // 第一步：筛选出所有叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  const projectList = projects.map(project => {
    // 获取该项目的所有叶子任务
    const projectLeafTasks = leafTasks.filter(t => t.projectId === project.id);
    const monthTasks = projectLeafTasks.filter(t => {
      const taskDate = dayjs(t.completedAt || t.createdAt);
      return taskDate.isBetween(monthStart, monthEnd, null, '[]');
    });
    
    // 计算本月进度变化
    const previousProgress = project.previousProgress || 0;
    const progressChange = project.progress - previousProgress;
    
    return {
      id: project.id,
      name: project.name,
      owner: project.ownerId,
      progress: project.progress,
      monthProgressChange: progressChange,
      monthTasksCompleted: monthTasks.filter(t => t.status === 'done').length,
      monthTasksTotal: monthTasks.length,
      status: project.status,
      delayDays: project.delayDays
    };
  });
  
  return {
    activeProjects: projectList.filter(p => p.status === 'active').length,
    newProjects: projectList.filter(p => {
      return dayjs(p.createdAt).isBetween(monthStart, monthEnd, null, '[]');
    }).length,
    completedProjects: projectList.filter(p => p.status === 'completed').length,
    delayedProjects: projectList.filter(p => p.delayDays > 0).length,
    projectList
  };
}
```

#### 6.3.5 计算月度任务统计

```typescript
function calculateMonthlyTaskStats(tasks: any[], monthRange: any) {
  const { monthStart, monthEnd } = monthRange;
  
  // 第一步：筛选出所有叶子任务
  const leafTasks = getLeafTasks(tasks);
  
  const monthTasks = leafTasks.filter(t => {
    const taskDate = dayjs(t.completedAt || t.createdAt);
    return taskDate.isBetween(monthStart, monthEnd, null, '[]');
  });
  
  // 计算每周任务统计
  const weeklyTaskCount: WeeklyTaskCount[] = [];
  for (let week = 1; week <= 4; week++) {
    const weekStart = dayjs(monthStart).add(week - 1, 'week').format('YYYY-MM-DD');
    const weekEnd = dayjs(monthStart).add(week, 'week').subtract(1, 'day').format('YYYY-MM-DD');
    
    const weekTasks = monthTasks.filter(t => {
      const taskDate = dayjs(t.completedAt);
      return taskDate.isBetween(weekStart, weekEnd, null, '[]');
    });
    
    weeklyTaskCount.push({
      weekNumber: week,
      weekStart,
      weekEnd,
      completedCount: weekTasks.length
    });
  }
  
  return {
    totalTasks: monthTasks.length,
    completedTasks: monthTasks.filter(t => t.status === 'done').length,
    inProgressTasks: monthTasks.filter(t => t.status === 'in-progress').length,
    pendingTasks: monthTasks.filter(t => t.status === 'pending').length,
    taskList: monthTasks.map(t => ({
      id: t.id,
      name: t.name,
      projectName: projects.find(p => p.id === t.projectId)?.name || '',
      assignee: users.find(u => u.id === t.assigneeId)?.name || '',
      priority: t.priority,
      completedAt: t.completedAt,
      status: t.status
    })),
    weeklyTaskCount
  };
}
```

## 七、组件结构设计

### 7.1 组件层次结构

```
Reports.vue (主组件)
├── TabHeader.vue (Tab页切换组件)
├── ReportsStatistics.vue (Tab 1: 报表统计)
│   ├── StatsCards.vue (统计卡片)
│   ├── ChartsSection.vue (图表区域)
│   └── ExportSection.vue (导出区域)
├── WeeklyProgressReport.vue (Tab 2: 本周项目进度报告)
│   ├── ReportHeader.vue (报告头部)
│   ├── ProjectSummary.vue (项目汇总)
│   │   ├── SummaryCards.vue (统计卡片)
│   │   └── ProjectList.vue (项目列表)
│   ├── TaskDetails.vue (任务详情)
│   │   ├── TaskCharts.vue (任务图表)
│   │   └── TaskList.vue (任务列表)
│   ├── TeamPerformance.vue (团队绩效)
│   │   ├── PerformanceCards.vue (绩效卡片)
│   │   ├── MemberRanking.vue (成员排行)
│   │   └── PerformanceTrend.vue (绩效趋势)
│   └── NextWeekPlan.vue (下周计划)
│       ├── PlanOverview.vue (计划概览)
│       ├── PlannedTaskList.vue (计划任务列表)
│       └── Milestones.vue (里程碑)
└── MonthlyProgressReport.vue (Tab 3: 月报项目进度报告)
    ├── MonthlyReportHeader.vue (报告头部，含月份选择)
    ├── MonthlyOverview.vue (月度概况)
    │   ├── MonthlySummaryCards.vue (统计卡片)
    │   └── MonthlyTrendChart.vue (月度趋势图)
    ├── ProjectProgress.vue (项目进展)
    │   ├── ProjectProgressTable.vue (项目进度表格)
    │   └── ProjectProgressBar.vue (项目进度条)
    ├── TaskStatistics.vue (任务统计)
    │   ├── MonthlyTaskCharts.vue (任务图表)
    │   └── MonthlyTaskList.vue (任务列表)
    ├── MonthlyTeamPerformance.vue (团队绩效)
    │   ├── MonthlyPerformanceCards.vue (绩效卡片)
    │   ├── MonthlyMemberRanking.vue (成员排行)
    │   └── WeeklyPerformanceTrend.vue (每周绩效趋势)
    └── NextMonthPlan.vue (下月计划)
        ├── MonthlyPlanOverview.vue (计划概览)
        ├── NextMonthTaskList.vue (下月任务列表)
        └── MonthlyMilestones.vue (里程碑)
```

### 7.2 主要组件说明

#### TabHeader.vue

* 功能：Tab页切换

* Props: activeTab (string)

* Events: tab-change

#### WeeklyProgressReport.vue

* 功能：本周项目进度报告主容器

* 包含四个子模块

* 数据计算和状态管理

#### MonthlyProgressReport.vue

* 功能：月报项目进度报告主容器

* 包含五个子模块

* 数据计算和状态管理

* 支持月份选择和历史数据查看

## 八、技术实现方案

### 8.1 Tab页切换实现

使用Vue 3的ref和computed状态管理：

```typescript
const activeTab = ref('statistics');

const tabs = [
  { id: 'statistics', label: 'reports.tabs.statistics' },
  { id: 'weekly-report', label: 'reports.tabs.weeklyReport' },
  { id: 'monthly-report', label: 'reports.tabs.monthlyReport' }
];

const currentTab = computed(() => {
  return tabs.find(t => t.id === activeTab.value);
});
```

### 8.2 日期处理

使用dayjs处理日期计算：

* 安装依赖：`npm install dayjs`

* 配置插件：weekday, weekOfYear

### 8.3 图表渲染

使用ECharts渲染以下图表：

**周报图表**：

* 任务状态分布（饼图）

* 任务优先级分布（饼图）

* 每日完成任务趋势（折线图）

**月报图表**：

* 月度任务完成趋势（折线图）

* 任务状态分布（饼图）

* 任务优先级分布（饼图）

* 每周完成任务数（柱状图）

* 每周绩效趋势（折线图）

### 8.4 响应式设计

使用Tailwind CSS实现：

* 移动端：单列布局

* 平板：双列布局

* 桌面：多列布局

## 九、实现步骤

### Phase 1: 基础框架搭建

1. 创建TabHeader组件
2. 重构Reports.vue，添加Tab切换逻辑
3. 将现有内容迁移到ReportsStatistics子组件

### Phase 2: 数据准备

1. 安装dayjs依赖
2. 实现日期范围计算函数（周和月）
3. 实现数据聚合计算函数

### Phase 3: 本周报告模块开发

1. 创建WeeklyProgressReport主组件
2. 实现项目汇总模块
3. 实现任务详情模块
4. 实现团队绩效模块
5. 实现下周计划模块

### Phase 4: 月报模块开发

1. 创建MonthlyProgressReport主组件
2. 实现月度概况模块
3. 实现项目进展模块
4. 实现任务统计模块
5. 实现团队绩效模块
6. 实现下月计划模块
7. 实现月份选择和历史数据查看功能

### Phase 5: 图表集成

1. 集成ECharts
2. 实现周报图表（任务状态、优先级、每日趋势）
3. 实现月报图表（月度趋势、每周统计、每周绩效）

### Phase 6: 国际化

1. 添加中文翻译
2. 添加韩文翻译

### Phase 7: 测试和优化

1. 功能测试
2. 性能优化
3. 响应式测试

## 十、国际化文案

### 中文 (zh.ts)

```typescript
tabs: {
  statistics: '报表统计',
  weeklyReport: '本周项目进度报告',
  monthlyReport: '月报项目进度报告'
},
weeklyReport: {
  title: '本周项目进度报告',
  reportPeriod: '报告周期',
  generatedAt: '生成时间',
  exportReport: '导出周报',
  refreshData: '刷新数据',
  
  projectSummary: {
    title: '项目汇总',
    activeProjects: '活跃项目',
    newProjects: '新增项目',
    completedProjects: '完成项目',
    delayedProjects: '延期项目'
  },
  taskDetails: {
    title: '任务详情',
    taskStatus: '任务状态',
    taskPriority: '任务优先级'
  },
  teamPerformance: {
    title: '团队绩效',
    completedTasks: '本周完成任务',
    avgEfficiency: '平均效率',
    overtimeHours: '加班时长',
    memberRanking: '成员绩效排行榜',
    dailyTrend: '本周每日完成任务趋势'
  },
  nextWeekPlan: {
    title: '下周计划',
    plannedTasks: '计划任务',
    keyProjects: '重点项目',
    keyMilestones: '关键里程碑'
  }
},
monthlyReport: {
  title: '月报项目进度报告',
  reportPeriod: '报告周期',
  generatedAt: '生成时间',
  monthSelector: '月份选择',
  exportReport: '导出月报',
  refreshData: '刷新数据',
  previousMonth: '上个月',
  nextMonth: '下个月',
  
  monthlyOverview: {
    title: '月度概况',
    activeProjects: '活跃项目',
    newProjects: '新增项目',
    completedProjects: '完成项目',
    delayedProjects: '延期项目',
    totalTasks: '本月任务',
    completedTasks: '已完成',
    inProgressTasks: '进行中',
    pendingTasks: '未开始',
    monthlyTrend: '月度趋势分析'
  },
  projectProgress: {
    title: '项目进展',
    progress: '进度',
    status: '状态',
    progressChange: '本月进度变化',
    projectProgressDetail: '各项目本月进度详情'
  },
  taskStatistics: {
    title: '任务统计',
    taskStatus: '任务状态',
    taskPriority: '任务优先级',
    weeklyTaskCount: '每周完成任务数'
  },
  teamPerformance: {
    title: '团队绩效',
    completedTasks: '本月完成任务',
    avgEfficiency: '平均效率',
    overtimeHours: '加班时长',
    memberRanking: '本月成员绩效排行榜',
    weeklyTrend: '本月每周绩效趋势'
  },
  nextMonthPlan: {
    title: '下月计划',
    plannedTasks: '计划任务',
    keyProjects: '重点项目',
    keyMilestones: '关键里程碑',
    keyMilestonesList: '下月关键里程碑'
  }
}
```

### 韩文 (ko.ts)

```typescript
tabs: {
  statistics: '보고서 통계',
  weeklyReport: '이번 주 프로젝트 진행 보고서',
  monthlyReport: '월간 프로젝트 진행 보고서'
},
weeklyReport: {
  title: '이번 주 프로젝트 진행 보고서',
  reportPeriod: '보고서 기간',
  generatedAt: '생성 시간',
  exportReport: '주간 보고서 내보내기',
  refreshData: '데이터 새로고침',
  
  projectSummary: {
    title: '프로젝트 요약',
    activeProjects: '활성 프로젝트',
    newProjects: '신규 프로젝트',
    completedProjects: '완료 프로젝트',
    delayedProjects: '지연 프로젝트'
  },
  taskDetails: {
    title: '작업 상세',
    taskStatus: '작업 상태',
    taskPriority: '작업 우선순위'
  },
  teamPerformance: {
    title: '팀 성과',
    completedTasks: '이번 주 완료 작업',
    avgEfficiency: '평균 효율',
    overtimeHours: '연장 근무 시간',
    memberRanking: '팀원 성과 순위',
    dailyTrend: '이번 주 일일 작업 완료 추이'
  },
  nextWeekPlan: {
    title: '다음 주 계획',
    plannedTasks: '계획 작업',
    keyProjects: '주요 프로젝트',
    keyMilestones: '주요 마일스톤'
  }
},
monthlyReport: {
  title: '월간 프로젝트 진행 보고서',
  reportPeriod: '보고서 기간',
  generatedAt: '생성 시간',
  monthSelector: '월 선택',
  exportReport: '월간 보고서 내보내기',
  refreshData: '데이터 새로고침',
  previousMonth: '지난달',
  nextMonth: '다음달',
  
  monthlyOverview: {
    title: '월간 개요',
    activeProjects: '활성 프로젝트',
    newProjects: '신규 프로젝트',
    completedProjects: '완료 프로젝트',
    delayedProjects: '지연 프로젝트',
    totalTasks: '이번 달 작업',
    completedTasks: '완료',
    inProgressTasks: '진행 중',
    pendingTasks: '시작 전',
    monthlyTrend: '월간 추세 분석'
  },
  projectProgress: {
    title: '프로젝트 진행',
    progress: '진행률',
    status: '상태',
    progressChange: '이번 달 진행률 변화',
    projectProgressDetail: '프로젝트별 이번 달 진행 상세'
  },
  taskStatistics: {
    title: '작업 통계',
    taskStatus: '작업 상태',
    taskPriority: '작업 우선순위',
    weeklyTaskCount: '주간 완료 작업 수'
  },
  teamPerformance: {
    title: '팀 성과',
    completedTasks: '이번 달 완료 작업',
    avgEfficiency: '평균 효율',
    overtimeHours: '연장 근무 시간',
    memberRanking: '이번 달 팀원 성과 순위',
    weeklyTrend: '이번 달 주간 성과 추이'
  },
  nextMonthPlan: {
    title: '다음 달 계획',
    plannedTasks: '계획 작업',
    keyProjects: '주요 프로젝트',
    keyMilestones: '주요 마일스톤',
    keyMilestonesList: '다음 달 주요 마일스톤'
  }
}
```

## 十一、后续扩展建议

### 11.1 功能扩展

**周报功能扩展**：

1. 支持历史周报查看
2. 支持自定义周报导出格式
3. 支持周报邮件发送功能
4. 支持周报数据对比分析

**月报功能扩展**：
5\. 支持历史月报查看
6\. 支持自定义月报导出格式
7\. 支持月报邮件发送功能
8\. 支持月度数据对比分析
9\. 支持季度报表和年度报表
10\. 支持数据可视化仪表板

### 11.2 性能优化

1. 数据懒加载
2. 图表虚拟滚动
3. 缓存优化
4. 月度数据分页加载
5. 历史数据缓存机制

### 11.3 用户体验

1. 支持拖拽排序
2. 支持自定义视图布局
3. 支持数据筛选和搜索

