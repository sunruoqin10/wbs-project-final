# 项目加班管理功能规格说明

## 一、功能概述

项目加班管理页面用于记录和管理项目成员的加班情况，提供加班申请、审批、统计和分析功能，帮助管理者了解团队加班状况，合理安排工作。

### 1.1 业务背景

- 项目开发过程中，经常需要加班赶进度
- 需要记录加班情况以便进行工时统计和补偿
- 管理者需要了解团队加班状况，避免过度加班

### 1.2 功能目标

- 提供加班记录的增删改查功能
- 支持加班申请与审批流程
- 提供加班统计分析和可视化图表
- 支持加班与任务的关联

---

## 二、数据结构设计

### 2.1 加班记录类型 (OvertimeRecord)

```typescript
interface OvertimeRecord {
  id: string;                                    // 记录ID
  userId: string;                                // 加班人员ID
  projectId: string;                             // 所属项目ID
  taskId?: string;                               // 关联任务ID（可选）
  overtimeDate: string;                          // 加班日期 (YYYY-MM-DD)
  startTime: string;                             // 开始时间 (HH:mm)
  endTime: string;                               // 结束时间 (HH:mm)
  hours: number;                                 // 加班时长（小时，精确到0.5小时）
  overtimeType: 'weekday' | 'weekend' | 'holiday';  // 加班类型
  reason: string;                                // 加班原因
  status: 'pending' | 'approved' | 'rejected';   // 审批状态
  approverId?: string;                           // 审批人ID
  approvedAt?: string;                           // 审批时间
  rejectReason?: string;                         // 拒绝原因
  compensationType?: 'pay' | 'timeoff';          // 补偿方式：加班费/调休
  createdAt: string;                             // 创建时间
  updatedAt: string;                             // 更新时间
}
```

### 2.2 加班统计类型 (OvertimeStats)

```typescript
interface OvertimeStats {
  totalRecords: number;                          // 总记录数
  totalHours: number;                            // 总加班时长
  totalPeople: number;                           // 加班人数
  pendingApprovals: number;                      // 待审批数量
  thisMonthHours: number;                        // 本月加班时长
  thisMonthPeople: number;                       // 本月加班人数
  byType: {                                      // 按类型统计
    weekday: number;
    weekend: number;
    holiday: number;
  };
  byProject: {                                   // 按项目统计
    projectId: string;
    projectName: string;
    hours: number;
    count: number;
  }[];
}
```

---

## 三、页面功能设计

### 3.1 页面布局

```
┌─────────────────────────────────────────────────────────────────┐
│ 项目加班管理                                                      │
│ 记录和管理项目成员的加班情况                                        │
├─────────────────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │本月加班   │ │加班人数   │ │待审批    │ │调休累计   │ │加班费累计 ││
│ │  48h     │ │  12人    │ │  3条     │ │  24h     │ │  ¥4800   ││
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘│
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────┐ ┌─────────────────────┐                 │
│ │   加班趋势图         │ │   项目加班分布       │                 │
│ │   (折线图)          │ │   (柱状图)          │                 │
│ └─────────────────────┘ └─────────────────────┘                 │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 加班记录列表                                    [+ 新增加班]  │ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │ 筛选: [项目▼] [人员▼] [状态▼] [日期范围] [查询] [重置]  │ │ │
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │ 人员 │ 项目 │ 日期 │ 时长 │ 类型 │ 状态 │ 操作          │ │ │
│ │ ├─────────────────────────────────────────────────────────┤ │ │
│ │ │ 张三 │ WBS  │ 2/18 │ 4h  │ 周末 │ 待审批│ [审批][编辑]  │ │ │
│ │ │ 李四 │ SIP  │ 2/17 │ 3h  │ 工作日│ 已通过│ [查看][编辑]  │ │ │
│ │ │ ...  │ ...  │ ...  │ ... │ ...  │ ...  │ ...           │ │ │
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 统计卡片区域

| 卡片名称 | 数据内容 | 图标颜色 |
|---------|---------|---------|
| 本月加班时长 | 本月所有已审批加班的总时长 | 蓝色 (primary) |
| 加班人数 | 本月有加班记录的人数 | 绿色 (success) |
| 待审批数量 | 状态为 pending 的记录数 | 橙色 (warning) |
| 调休累计 | 选择调休补偿的总时长 | 紫色 (accent) |
| 加班费累计 | 选择加班费补偿的总金额估算 | 红色 (danger) |

### 3.3 图表区域

#### 3.3.1 加班趋势图（折线图）
- X轴：日期（最近30天或按月）
- Y轴：加班时长（小时）
- 可切换显示：按天/按周/按月

#### 3.3.2 项目加班分布（柱状图）
- X轴：项目名称
- Y轴：加班时长
- 显示各项目的加班时长对比

#### 3.3.3 人员加班排名（横向柱状图）
- 显示加班时长最多的前10名人员
- 可按时间段筛选

### 3.4 加班记录列表

#### 3.4.1 筛选条件
| 筛选项 | 类型 | 说明 |
|-------|------|------|
| 所属项目 | 下拉选择 | 全部/指定项目 |
| 加班人员 | 下拉选择 | 全部/指定人员 |
| 审批状态 | 下拉选择 | 全部/待审批/已通过/已拒绝 |
| 加班类型 | 下拉选择 | 全部/工作日/周末/节假日 |
| 日期范围 | 日期选择器 | 开始日期 - 结束日期 |

#### 3.4.2 表格列
| 列名 | 字段 | 说明 |
|-----|------|------|
| 加班人员 | userId | 显示人员姓名 |
| 所属项目 | projectId | 显示项目名称 |
| 关联任务 | taskId | 显示任务标题（可选） |
| 加班日期 | overtimeDate | YYYY-MM-DD |
| 时间段 | startTime, endTime | HH:mm - HH:mm |
| 时长 | hours | X小时 |
| 类型 | overtimeType | 工作日/周末/节假日 |
| 补偿方式 | compensationType | 加班费/调休 |
| 状态 | status | 待审批/已通过/已拒绝 |
| 操作 | - | 审批/编辑/删除 |

### 3.5 加班申请/编辑表单

```
┌─────────────────────────────────────────┐
│ 新增加班记录                              │
├─────────────────────────────────────────┤
│ 所属项目 *    [请选择项目 ▼]             │
│ 关联任务      [请选择任务 ▼]             │
│ 加班人员 *    [请选择人员 ▼]             │
│ 加班日期 *    [📅 选择日期]              │
│ 时间段 *      [开始时间] - [结束时间]     │
│ 加班类型 *    ○ 工作日 ○ 周末 ○ 节假日   │
│ 补偿方式 *    ○ 加班费 ○ 调休            │
│ 加班原因 *    [________________]         │
│                                         │
│         [取消]  [确定]                   │
└─────────────────────────────────────────┘
```

### 3.6 审批功能

#### 3.6.1 审批弹窗
```
┌─────────────────────────────────────────┐
│ 审批加班申请                              │
├─────────────────────────────────────────┤
│ 申请人：张三                              │
│ 项目：WBS项目管理系统                     │
│ 加班日期：2024-02-18                     │
│ 加班时长：4小时                          │
│ 加班原因：赶项目进度，完成核心功能开发     │
│                                         │
│ 审批结果：○ 通过  ○ 拒绝                 │
│ 拒绝原因：[________________]             │
│                                         │
│         [取消]  [确定]                   │
└─────────────────────────────────────────┘
```

---

## 四、权限设计

### 4.1 基本权限

| 角色 | 新增 | 编辑 | 删除 | 查看范围 |
|-----|------|------|------|---------|
| admin | ✓ | ✓ | ✓ | 全部 |
| project-manager | ✓ | ✓(自己的) | ✓(自己的待审批) | 全部 |
| member | ✓ | ✓(自己的) | ✓(自己的待审批) | 仅自己 |
| viewer | ✗ | ✗ | ✗ | 全部 |

### 4.2 审批权限规则

**审批人：该项目的负责人（ownerId）**

| 审批场景 | 审批人 | 说明 |
|---------|--------|------|
| 成员A提交项目X的加班申请 | 项目X的ownerId对应的用户 | 只有项目负责人可以审批 |
| 项目负责人提交自己项目的加班申请 | 自己审批 | 项目负责人可以审批自己的加班 |
| admin提交加班申请 | admin | admin可以自行审批 |

**审批权限判断逻辑：**
```typescript
// 判断当前用户是否有审批权限
function canApprove(record: OvertimeRecord, currentUser: User): boolean {
  // 1. admin 拥有所有审批权限
  if (currentUser.role === 'admin') return true;
  
  // 2. 项目负责人可以审批该项目的加班申请（包括自己的）
  const project = getProjectById(record.projectId);
  if (project.ownerId === currentUser.id) return true;
  
  return false;
}
```

### 4.3 审批流程

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│  成员提交    │ ──▶ │  项目负责人审批   │ ──▶ │  审批完成    │
│  加班申请    │     │                 │     │             │
└─────────────┘     └─────────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ 通过 / 拒绝  │
                    └─────────────┘
```

---

## 五、技术实现方案

### 5.1 前端文件结构

```
frontend/src/
├── views/
│   └── OvertimeManagement.vue    # 加班管理页面
├── components/
│   └── overtime/
│       ├── OvertimeModal.vue     # 加班申请/编辑弹窗
│       ├── ApprovalModal.vue     # 审批弹窗
│       └── OvertimeStats.vue     # 统计卡片组件
├── stores/
│   └── overtime.ts               # 加班数据状态管理
├── types/
│   └── index.ts                  # 添加 OvertimeRecord 类型
└── router/
    └── index.ts                  # 添加路由
```

### 5.2 后端文件结构

```
backend/src/main/java/com/wbs/project/
├── controller/
│   └── OvertimeController.java   # 加班管理API
├── service/
│   └── OvertimeService.java      # 加班业务逻辑
├── entity/
│   └── OvertimeRecord.java       # 加班记录实体
├── mapper/
│   └── OvertimeMapper.java       # 数据访问层
└── dto/
    └── OvertimeDTO.java          # 数据传输对象
```

### 5.3 API 接口设计

| 方法 | 路径 | 说明 |
|-----|------|------|
| GET | /api/overtime | 获取加班记录列表 |
| GET | /api/overtime/{id} | 获取单条加班记录 |
| POST | /api/overtime | 新增加班记录 |
| PUT | /api/overtime/{id} | 更新加班记录 |
| DELETE | /api/overtime/{id} | 删除加班记录 |
| PUT | /api/overtime/{id}/approve | 审批加班申请 |
| GET | /api/overtime/stats | 获取加班统计数据 |

### 5.4 数据库表设计

#### 5.4.1 项目负责人说明

**项目负责人定义：** `sys_project` 表中的 `owner_id` 字段对应的 `sys_user` 表中的 `id` 的人员。

```
┌─────────────────┐         ┌─────────────────┐
│   sys_project   │         │    sys_user     │
├─────────────────┤         ├─────────────────┤
│ id              │         │ id              │
│ name            │         │ name            │
│ owner_id ───────┼────────▶│ (项目负责人)    │
│ ...             │         │ ...             │
└─────────────────┘         └─────────────────┘
```

**审批权限判断：** 当加班记录的 `project_id` 对应的项目中，`owner_id` 等于当前用户 `id` 时，该用户即为项目负责人，拥有该项目的加班审批权限。

#### 5.4.2 加班记录表

```sql
CREATE TABLE t_overtime_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36),
    overtime_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    hours DECIMAL(4,1) NOT NULL,
    overtime_type VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    approver_id VARCHAR(36),
    approved_at DATETIME,
    reject_reason TEXT,
    compensation_type VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

**注意：** 外键约束在Java程序中进行验证，不在数据库层面设置外键约束。

#### 5.4.3 Java层外键约束验证

在 `OvertimeService.java` 中进行以下验证：

```java
// 创建/更新加班记录时的验证
public void validateOvertimeRecord(OvertimeRecord record) {
    // 1. 验证用户是否存在
    User user = userRepository.findById(record.getUserId())
        .orElseThrow(() -> new BusinessException("用户不存在"));
    
    // 2. 验证项目是否存在
    Project project = projectRepository.findById(record.getProjectId())
        .orElseThrow(() -> new BusinessException("项目不存在"));
    
    // 3. 验证任务是否存在（如果关联了任务）
    if (record.getTaskId() != null) {
        Task task = taskRepository.findById(record.getTaskId())
            .orElseThrow(() -> new BusinessException("任务不存在"));
    }
    
    // 4. 验证审批人是否存在（如果已审批）
    if (record.getApproverId() != null) {
        User approver = userRepository.findById(record.getApproverId())
            .orElseThrow(() -> new BusinessException("审批人不存在"));
    }
}

---

## 六、UI 设计规范

### 6.1 颜色方案

| 状态 | 颜色 | 用途 |
|-----|------|------|
| 待审批 | 橙色 (warning) | pending 状态 |
| 已通过 | 绿色 (success) | approved 状态 |
| 已拒绝 | 红色 (danger) | rejected 状态 |
| 工作日加班 | 蓝色 | weekday 类型 |
| 周末加班 | 紫色 | weekend 类型 |
| 节假日加班 | 红色 | holiday 类型 |

### 6.2 图标

- 菜单图标：时钟图标（与延期统计区分）
- 加班时长：⏰ 或 🕐
- 审批：✓ 或 ✗

---

## 七、实现优先级

### Phase 1 - 基础功能（必须）
1. 加班记录的增删改查
2. 统计卡片展示
3. 加班记录列表（带筛选）

### Phase 2 - 审批功能（重要）
1. 审批流程
2. 审批状态流转
3. 审批通知

### Phase 3 - 高级功能（可选）
1. 图表可视化
2. 数据导出
3. 加班补偿计算
4. 与任务关联显示
