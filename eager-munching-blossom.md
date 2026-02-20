# 项目延期处理功能细化实施方案

## Context

WBS 项目管理系统当前具有基础的延期视觉提示（TaskCard.vue 中的颜色标识），但缺乏完整的延期管理功能。用户需要：
1. 系统化地识别和统计延期任务
2. 在任务编辑时记录延期原因
3. 在报表中查看延期数据分析
4. 在甘特图中视觉化延期状态

本方案基于原计划文档，结合当前代码库实际情况，提供可执行的详细实施步骤。

---

## 实施方案

### 阶段一：核心延期功能（高优先级）

#### 1. 数据库字段扩展

**文件**: 创建 SQL 迁移脚本 `backend/src/main/resources/db/migration/V1__add_delay_fields.sql`

```sql
-- 添加延期相关字段到 sys_task 表
ALTER TABLE sys_task ADD COLUMN original_end_date DATE DEFAULT NULL COMMENT '原始结束日期（首次设定）';
ALTER TABLE sys_task ADD COLUMN delayed_days INT DEFAULT 0 COMMENT '累计延期天数';
ALTER TABLE sys_task ADD COLUMN delay_reason TEXT DEFAULT NULL COMMENT '最新延期原因';
ALTER TABLE sys_task ADD COLUMN delay_count INT DEFAULT 0 COMMENT '延期次数';
ALTER TABLE sys_task ADD COLUMN last_delay_date DATE DEFAULT NULL COMMENT '最后延期日期';
ALTER TABLE sys_task ADD COLUMN is_delayed BOOLEAN DEFAULT FALSE COMMENT '是否已延期（运行时计算）';

-- 添加索引优化查询
CREATE INDEX idx_end_date ON sys_task(end_date);
CREATE INDEX idx_delayed_days ON sys_task(delayed_days);
CREATE INDEX idx_is_delayed ON sys_task(is_delayed);
```

#### 2. 后端实体类扩展

**文件**: `backend/src/main/java/com/wbs/project/entity/Task.java`

在现有字段后添加：

```java
// 延期相关字段
private LocalDate originalEndDate;  // 原始结束日期
private Integer delayedDays;        // 累计延期天数
private String delayReason;         // 延期原因
private Integer delayCount;         // 延期次数
private LocalDate lastDelayDate;    // 最后延期日期
private Boolean isDelayed;          // 是否已延期
```

#### 3. 后端 Mapper 扩展

**文件**: `backend/src/main/resources/mapper/TaskMapper.xml`

修改 `BaseResultMap` 添加新字段映射，修改 `Base_Column_List` 包含新字段。

#### 4. 后端 Service 延期计算逻辑

**文件**: `backend/src/main/java/com/wbs/project/service/TaskService.java`

添加以下方法：

```java
/**
 * 更新任务延期状态（运行时计算）
 */
public void updateDelayedStatus(List<Task> tasks) {
    LocalDate today = LocalDate.now();
    for (Task task : tasks) {
        boolean isDelayed = task.getEndDate().isBefore(today)
            && !"done".equals(task.getStatus());
        task.setIsDelayed(isDelayed);
    }
}

/**
 * 获取延期任务列表
 */
public List<Task> getDelayedTasks(String projectId, Boolean includeCompleted) {
    List<Task> tasks = projectId != null
        ? taskMapper.selectByProjectId(projectId)
        : taskMapper.selectAll();
    updateDelayedStatus(tasks);
    return tasks.stream()
        .filter(t -> t.getIsDelayed() || t.getDelayedDays() > 0)
        .filter(t -> includeCompleted || !"done".equals(t.getStatus()))
        .collect(Collectors.toList());
}

/**
 * 获取项目延期统计
 */
public DelayStats getProjectDelayStats(String projectId) {
    List<Task> tasks = taskMapper.selectByProjectId(projectId);
    updateDelayedStatus(tasks);

    DelayStats stats = new DelayStats();
    stats.setTotalTasks(tasks.size());
    stats.setDelayedTasks((int) tasks.stream().filter(t -> t.getIsDelayed()).count());
    stats.setTotalDelayedDays(tasks.stream().mapToInt(t -> t.getDelayedDays() != null ? t.getDelayedDays() : 0).sum());
    stats.setCriticalDelayedTasks((int) tasks.stream().filter(t -> t.getDelayedDays() != null && t.getDelayedDays() >= 7).count());
    stats.setDelayRate(stats.getTotalTasks() > 0 ? (double) stats.getDelayedTasks() / stats.getTotalTasks() * 100 : 0);

    return stats;
}
```

#### 5. 后端 Controller API 扩展

**文件**: `backend/src/main/java/com/wbs/project/controller/TaskController.java`

添加端点：

```java
@GetMapping("/delayed")
public Result<List<Task>> getDelayedTasks(
    @RequestParam(required = false) String projectId,
    @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted) {
    return Result.success(taskService.getDelayedTasks(projectId, includeCompleted));
}

@GetMapping("/project/{projectId}/delay-stats")
public Result<DelayStats> getProjectDelayStats(@PathVariable String projectId) {
    return Result.success(taskService.getProjectDelayStats(projectId));
}
```

#### 6. 前端类型定义扩展

**文件**: `frontend/src/types/index.ts`

在 Task 接口中添加：

```typescript
export interface Task {
  // ... 现有字段

  // 延期相关字段
  originalEndDate?: string;      // 原始结束日期
  delayedDays?: number;          // 累计延期天数
  delayReason?: string;          // 延期原因
  delayCount?: number;           // 延期次数
  lastDelayDate?: string;        // 最后延期日期
  isDelayed?: boolean;           // 是否已延期
}

// 新增接口
export interface DelayStats {
  totalTasks: number;
  delayedTasks: number;
  delayRate: number;
  totalDelayedDays: number;
  criticalDelayedTasks: number;
}
```

#### 7. 前端 API 服务扩展

**文件**: `frontend/src/services/api.ts`

添加方法：

```typescript
export const getDelayedTasks = async (projectId?: string, includeCompleted = false): Promise<Task[]> => {
  const params = new URLSearchParams();
  if (projectId) params.append('projectId', projectId);
  params.append('includeCompleted', String(includeCompleted));

  const response = await fetch(`${API_BASE_URL}/tasks/delayed?${params}`);
  const result = await response.json();
  handleResponse(result);
  return result.data;
};

export const getProjectDelayStats = async (projectId: string): Promise<DelayStats> => {
  const response = await fetch(`${API_BASE_URL}/tasks/project/${projectId}/delay-stats`);
  const result = await response.json();
  handleResponse(result);
  return result.data;
};
```

#### 8. TaskCard 组件延期标识

**文件**: `frontend/src/components/task/TaskCard.vue`

修改要点：
- 在现有 `dueDateClass` 计算属性基础上，添加延期徽章显示
- 添加 `delayBorderClass` 计算属性，为延期任务添加右侧边框
- 在模板中添加延期标识区域（显示在 footer 之前）

```vue
<!-- 新增：延期标识 -->
<div v-if="task.isDelayed || (task.delayedDays || 0) > 0" class="mt-2 flex items-center gap-2">
  <Badge :variant="delaySeverity" size="sm">{{ delayLabel }}</Badge>
  <span v-if="task.delayedDays > 0" class="text-xs" :class="delayTextClass">
    延期 {{ task.delayedDays }} 天
  </span>
</div>
```

添加计算属性：

```typescript
const delaySeverity = computed(() => {
  const days = props.task.delayedDays || 0;
  if (days >= 7) return 'danger';
  if (days >= 3) return 'warning';
  return 'info';
});

const delayLabel = computed(() => {
  const days = props.task.delayedDays || 0;
  if (days >= 7) return '严重延期';
  if (days >= 3) return '已延期';
  return '延期';
});
```

#### 9. TaskBoard 延期筛选器

**文件**: `frontend/src/views/TaskBoard.vue`

在页面顶部（现有 header 之后）添加筛选器区域：

```vue
<div class="flex items-center gap-4 rounded-lg bg-secondary-50 p-4">
  <span class="text-sm font-medium text-secondary-700">延期筛选：</span>
  <label class="flex items-center gap-2 cursor-pointer">
    <input type="checkbox" v-model="showDelayedOnly" class="rounded border-secondary-300 text-primary-600 focus:ring-primary-500" />
    <span class="text-sm text-secondary-600">仅显示延期任务</span>
  </label>
  <Select v-model="delayFilter" class="w-40">
    <option value="all">全部</option>
    <option value="delayed">已延期</option>
    <option value="critical">严重延期（≥7天）</option>
    <option value="warning">中度延期（3-6天）</option>
  </Select>
</div>
```

添加响应式变量和筛选逻辑：

```typescript
const showDelayedOnly = ref(false);
const delayFilter = ref('all');

const filteredTasks = computed(() => {
  let tasks = projectTasks.value;

  if (showDelayedOnly.value || delayFilter.value !== 'all') {
    tasks = tasks.filter(task => {
      const isDelayed = task.isDelayed || (task.delayedDays || 0) > 0;
      if (!isDelayed) return false;

      const days = task.delayedDays || 0;
      switch (delayFilter.value) {
        case 'critical': return days >= 7;
        case 'warning': return days >= 3 && days < 7;
        default: return true;
      }
    });
  }

  return tasks;
});
```

#### 10. Reports 延期统计卡片

**文件**: `frontend/src/views/Reports.vue`

在现有统计卡片区域后添加新卡片（使用 grid 布局）：

```vue
<div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
  <!-- 现有卡片保持不变 -->

  <!-- 新增：延期任务数 -->
  <Card>
    <div class="flex items-center">
      <div class="rounded-lg bg-danger-100 p-3">
        <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <div class="ml-4">
        <p class="text-sm font-medium text-secondary-600">延期任务数</p>
        <p class="text-2xl font-semibold text-danger-600">{{ delayStats.delayedTasks }}</p>
      </div>
    </div>
  </Card>

  <!-- 新增：累计延期天数 -->
  <Card>
    <div class="flex items-center">
      <div class="rounded-lg bg-warning-100 p-3">
        <svg class="h-6 w-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
        </svg>
      </div>
      <div class="ml-4">
        <p class="text-sm font-medium text-secondary-600">累计延期天数</p>
        <p class="text-2xl font-semibold text-warning-600">{{ delayStats.totalDelayedDays }}</p>
      </div>
    </div>
  </Card>

  <!-- 新增：延期率 -->
  <Card>
    <div class="flex items-center">
      <div class="rounded-lg bg-info-100 p-3">
        <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      </div>
      <div class="ml-4">
        <p class="text-sm font-medium text-secondary-600">延期率</p>
        <p class="text-2xl font-semibold text-info-600">{{ delayStats.delayRate.toFixed(1) }}%</p>
      </div>
    </div>
  </Card>

  <!-- 新增：严重延期任务 -->
  <Card>
    <div class="flex items-center">
      <div class="rounded-lg bg-accent-100 p-3">
        <svg class="h-6 w-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <div class="ml-4">
        <p class="text-sm font-medium text-secondary-600">严重延期（≥7天）</p>
        <p class="text-2xl font-semibold text-accent-600">{{ delayStats.criticalDelayedTasks }}</p>
      </div>
    </div>
  </Card>
</div>
```

在 script 部分添加：

```typescript
import { getProjectDelayStats } from '@/services/api';

const delayStats = ref({
  delayedTasks: 0,
  totalDelayedDays: 0,
  delayRate: 0,
  criticalDelayedTasks: 0
});

onMounted(async () => {
  // 现有数据加载...

  // 加载延期统计
  if (projectStore.projects.length > 0) {
    const stats = await getProjectDelayStats(projectStore.projects[0].id);
    delayStats.value = stats;
  }
});
```

#### 11. 国际化文本

**文件**: `frontend/src/i18n/locales/zh.ts`

添加翻译：

```typescript
export default {
  // ... 现有内容

  taskBoard: {
    // ... 现有字段
    delayFilter: '延期筛选',
    showDelayedOnly: '仅显示延期任务',
    delayOptions: {
      all: '全部',
      delayed: '已延期',
      critical: '严重延期（≥7天）',
      warning: '中度延期（3-6天）'
    }
  },

  reports: {
    // ... 现有字段
    stats: {
      // ... 现有字段
      delayedTasks: '延期任务数',
      totalDelayedDays: '累计延期天数',
      delayRate: '延期率',
      criticalDelays: '严重延期（≥7天）'
    }
  }
};
```

**文件**: `frontend/src/i18n/locales/ko.ts` 对应韩文翻译

---

### 阶段二：延期管理功能（中优先级）

#### 12. TaskModal 延期原因录入

**文件**: `frontend/src/components/task/TaskModal.vue`

在结束日期输入框后添加：

```vue
<!-- 延期原因输入（仅当检测到日期延后时显示） -->
<div v-if="showDelayReasonInput">
  <label class="mb-1 block text-sm font-medium text-secondary-700">
    延期原因
    <span class="text-warning-500">*</span>
  </label>
  <textarea
    v-model="formData.delayReason"
    rows="3"
    class="w-full rounded-lg border border-secondary-200 px-4 py-2 focus:border-warning-500 focus:ring-2 focus:ring-warning-500/20"
    placeholder="请说明延期原因..."
  ></textarea>
  <div class="mt-1 rounded-md bg-warning-50 px-3 py-2 text-xs text-warning-700">
    检测到结束日期延后，请说明延期原因
  </div>
</div>
```

添加逻辑：

```typescript
const originalEndDate = ref('');

const showDelayReasonInput = computed(() => {
  if (!isEditing.value || !formData.endDate || !originalEndDate.value) {
    return false;
  }
  return new Date(formData.endDate) > new Date(originalEndDate.value);
});

const loadTaskData = () => {
  if (props.task) {
    // ... 现有字段加载
    formData.endDate = props.task.endDate;
    originalEndDate.value = props.task.endDate;
    formData.delayReason = props.task.delayReason || '';
  }
};
```

#### 13. 后端延期记录 API

**文件**: `backend/src/main/java/com/wbs/project/controller/TaskController.java`

添加：

```java
@PostMapping("/{id}/delay")
public Result<Task> recordTaskDelay(
    @PathVariable String id,
    @RequestBody DelayRequest request) {
    Task updatedTask = taskService.recordTaskDelay(id, request);
    return Result.success("延期记录成功", updatedTask);
}
```

**文件**: `backend/src/main/java/com/wbs/project/service/TaskService.java`

添加方法：

```java
@Transactional
public Task recordTaskDelay(String taskId, DelayRequest request) {
    Task task = taskMapper.selectById(taskId);
    if (task == null) throw new RuntimeException("任务不存在");

    LocalDate oldEndDate = task.getEndDate();
    LocalDate newEndDate = LocalDate.parse(request.getNewEndDate());

    if (newEndDate.isBefore(oldEndDate)) {
        throw new RuntimeException("新的结束日期不能早于原结束日期");
    }

    int delayDays = (int) ChronoUnit.DAYS.between(oldEndDate, newEndDate);

    // 首次延期记录原始日期
    if (task.getOriginalEndDate() == null) {
        task.setOriginalEndDate(oldEndDate);
    }

    // 更新延期信息
    task.setEndDate(newEndDate);
    task.setDelayedDays((task.getDelayedDays() != null ? task.getDelayedDays() : 0) + delayDays);
    task.setDelayCount((task.getDelayCount() != null ? task.getDelayCount() : 0) + 1);
    task.setDelayReason(request.getDelayReason());
    task.setLastDelayDate(LocalDate.now());

    taskMapper.update(task);
    return task;
}
```

#### 14. GanttChart 延期高亮

**文件**: `frontend/src/components/gantt/GanttChart.vue`

修改任务条样式计算：

```typescript
const getTaskColor = (task: Task): string => {
  // 优先显示延期状态
  if (task.isDelayed || (task.delayedDays || 0) > 0) {
    const days = task.delayedDays || 0;
    if (days >= 7) return '#ef4444';    // 红色
    if (days >= 3) return '#f59e0b';    // 橙色
    return '#3b82f6';                    // 蓝色
  }

  // 按状态设置颜色
  const colors: Record<string, string> = {
    'todo': '#95a5a6',
    'in-progress': '#3498db',
    'done': '#27ae60'
  };
  return colors[task.status] || colors['todo'];
};
```

添加延期任务样式类：

```typescript
gantt.templates.task_class = (start, end, task) => {
  const ganttTask = task as any;
  let classes = ['gantt_task'];

  if (ganttTask.status) {
    classes.push(`gantt_task_${ganttTask.status}`);
  }

  if (ganttTask.isDelayed || (ganttTask.delayedDays || 0) > 0) {
    const days = ganttTask.delayedDays || 0;
    if (days >= 7) classes.push('gantt_task_delayed_critical');
    else if (days >= 3) classes.push('gantt_task_delayed_warning');
    else classes.push('gantt_task_delayed');
  }

  return classes.join(' ');
};
```

添加样式：

```css
.gantt_task_delayed {
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);
}

.gantt_task_delayed_warning {
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.5);
}

.gantt_task_delayed_critical {
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.6);
  animation: pulse-red 2s infinite;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}
```

---

### 阶段三：高级分析功能（低优先级）

#### 15. 延期历史表

创建 `sys_task_delay_history` 表（参见原计划文档）

#### 16. 延期趋势图表

在 Reports.vue 中添加延期趋势分析图表（折线图显示按月的延期变化）

---

## 关键文件清单

### 后端文件
- `backend/src/main/resources/db/migration/V1__add_delay_fields.sql`（新建）
- `backend/src/main/java/com/wbs/project/entity/Task.java`
- `backend/src/main/resources/mapper/TaskMapper.xml`
- `backend/src/main/java/com/wbs/project/service/TaskService.java`
- `backend/src/main/java/com/wbs/project/controller/TaskController.java`

### 前端文件
- `frontend/src/types/index.ts`
- `frontend/src/services/api.ts`
- `frontend/src/components/task/TaskCard.vue`
- `frontend/src/views/TaskBoard.vue`
- `frontend/src/views/Reports.vue`
- `frontend/src/components/task/TaskModal.vue`
- `frontend/src/components/gantt/GanttChart.vue`
- `frontend/src/i18n/locales/zh.ts`
- `frontend/src/i18n/locales/ko.ts`

---

## 验证步骤

1. 运行后端，执行数据库迁移脚本
2. 启动前端，测试延期任务筛选功能
3. 创建一个任务，修改结束日期为延后日期，验证延期原因录入
4. 在任务看板中查看延期任务的视觉标识
5. 在报表页面查看延期统计数据
6. 在甘特图中查看延期任务的高亮显示

---

## 注意事项

1. **数据兼容性**: 新增字段使用默认值，确保现有数据兼容
2. **性能优化**: 延期统计可考虑缓存（如 Redis）
3. **国际化**: 所有新增文本需要添加中韩双语支持
4. **测试**: 建议先在测试环境验证完整流程
