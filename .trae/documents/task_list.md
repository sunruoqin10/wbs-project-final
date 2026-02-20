# 项目加班管理功能 - 任务列表

## 一、前端开发任务

### 1.1 类型定义
- [x] 在 `types/index.ts` 中添加 `OvertimeRecord` 接口
- [x] 在 `types/index.ts` 中添加 `OvertimeStats` 接口

### 1.2 状态管理
- [x] 创建 `stores/overtime.ts` 文件
- [x] 实现 `loadOvertimeRecords` 方法（加载加班记录列表）
- [x] 实现 `loadOvertimeStats` 方法（加载统计数据）
- [x] 实现 `createOvertimeRecord` 方法（创建加班记录）
- [x] 实现 `updateOvertimeRecord` 方法（更新加班记录）
- [x] 实现 `deleteOvertimeRecord` 方法（删除加班记录）
- [x] 实现 `approveOvertimeRecord` 方法（审批加班记录）

### 1.3 页面组件
- [x] 创建 `views/OvertimeManagement.vue` 页面
- [x] 实现页面头部（标题和描述）
- [x] 实现统计卡片区域（5个统计卡片）
- [x] 实现加班趋势图表（折线图）
- [x] 实现项目加班分布图表（柱状图）
- [x] 实现人员加班排名图表（横向柱状图）
- [x] 实现加班记录列表表格
- [x] 实现筛选功能（项目、人员、状态、类型、日期范围）

### 1.4 弹窗组件
- [x] 创建 `components/overtime/OvertimeModal.vue` 组件
- [x] 实现新增加班表单
- [x] 实现编辑加班表单
- [x] 实现表单验证
- [x] 创建 `components/overtime/ApprovalModal.vue` 组件
- [x] 实现审批通过/拒绝功能

### 1.5 路由配置
- [x] 在 `router/index.ts` 中添加加班管理路由
- [x] 在 `Sidebar.vue` 中添加加班管理菜单项

---

## 二、后端开发任务

### 2.1 数据库
- [x] 创建 `t_overtime_record` 数据库表
- [x] 添加必要的索引

### 2.2 实体类
- [x] 创建 `entity/OvertimeRecord.java` 实体类
- [x] 创建 `dto/OvertimeDTO.java` 数据传输对象

### 2.3 数据访问层
- [x] 创建 `mapper/OvertimeMapper.java` 接口
- [x] 实现基本的 CRUD 方法
- [x] 实现按条件查询方法
- [x] 实现统计查询方法

### 2.4 业务逻辑层
- [x] 创建 `service/OvertimeService.java` 服务类
- [x] 实现创建加班记录逻辑
- [x] 实现更新加班记录逻辑
- [x] 实现删除加班记录逻辑
- [x] 实现审批逻辑
- [x] 实现统计计算逻辑

### 2.5 控制器层
- [x] 创建 `controller/OvertimeController.java` 控制器
- [x] 实现 GET /api/overtime 接口
- [x] 实现 GET /api/overtime/{id} 接口
- [x] 实现 POST /api/overtime 接口
- [x] 实现 PUT /api/overtime/{id} 接口
- [x] 实现 DELETE /api/overtime/{id} 接口
- [x] 实现 PUT /api/overtime/{id}/approve 接口
- [x] 实现 GET /api/overtime/stats 接口

---

## 三、测试任务

### 3.1 前端测试
- [ ] 测试加班记录列表加载
- [ ] 测试筛选功能
- [ ] 测试新增加班记录
- [ ] 测试编辑加班记录
- [ ] 测试删除加班记录
- [ ] 测试审批功能
- [ ] 测试图表渲染

### 3.2 后端测试
- [ ] 测试 API 接口响应
- [ ] 测试数据验证
- [ ] 测试权限控制

---

## 四、任务依赖关系

```
前端类型定义 → 前端状态管理 → 前端页面组件 → 前端弹窗组件
                                    ↓
                              前端路由配置
                                    
后端数据库 → 后端实体类 → 后端数据访问层 → 后端业务逻辑层 → 后端控制器层
```

---

## 五、预计工作量

| 模块 | 预计时间 |
|-----|---------|
| 前端类型定义 | 0.5h |
| 前端状态管理 | 1h |
| 前端页面组件 | 3h |
| 前端弹窗组件 | 2h |
| 前端路由配置 | 0.5h |
| 后端数据库 | 0.5h |
| 后端实体类 | 0.5h |
| 后端数据访问层 | 1h |
| 后端业务逻辑层 | 2h |
| 后端控制器层 | 1h |
| 测试 | 2h |
| **总计** | **14h** |
