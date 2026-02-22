# WBS系统权限管理功能 - 实施计划

## ⚠️ 数据约束说明

- **外键约束**：数据库表之间不建立外键约束，改为在 Java 程序中通过业务逻辑实现数据完整性
- **级联操作**：删除、更新等级联操作在 Java 程序中实现，确保数据一致性

---

## 一、现状分析

### 1.1 当前用户角色

| 角色 | 数据库值 | 当前功能 |
|------|---------|---------|
| 管理员 | `admin` | 无特殊权限控制 |
| 项目经理 | `project-manager` | 无特殊权限控制 |
| 成员 | `member` | 无特殊权限控制 |

### 1.2 当前项目相关权限

- 项目有 `owner_id` 字段（项目负责人）
- 项目负责人可审批该项目的加班申请
- 有 `sys_project_member` 表存储项目成员关系

### 1.3 问题

- 没有系统性的权限控制
- 角色只是标签，没有实际权限逻辑
- 前端没有权限校验，后端也没有接口权限控制

---

## 二、权限模型设计

### 2.1 采用 RBAC + 项目级权限 混合模型

```
系统级权限（RBAC）
    └── 角色权限
        ├── admin: 全部权限
        ├── project-manager: 项目管理权限
        └── member: 基本操作权限

项目级权限
    └── 项目负责人（owner）
        ├── 项目设置
        ├── 成员管理
        ├── 加班审批
        └── 任务分配
```

### 2.2 权限点定义

#### 系统级权限

| 权限代码 | 描述 | admin | project-manager | member |
|---------|------|-------|-----------------|--------|
| `user:create` | 创建用户 | ✓ | ✗ | ✗ |
| `user:edit` | 编辑用户 | ✓ | ✗ | ✗ |
| `user:delete` | 删除用户 | ✓ | ✗ | ✗ |
| `user:view` | 查看用户列表 | ✓ | ✓ | ✗ |
| `project:create` | 创建项目 | ✓ | ✓ | ✓ |
| `project:delete` | 删除项目 | ✓ | 部分 | 部分 |
| `settings:view` | 查看系统设置 | ✓ | ✗ | ✗ |
| `settings:edit` | 修改系统设置 | ✓ | ✗ | ✗ |

#### 项目级权限

| 权限代码 | 描述 | 项目负责人 | 项目成员 | 非成员 |
|---------|------|-----------|---------|--------|
| `project:edit` | 编辑项目 | ✓ | ✗ | ✗ |
| `project:manage_members` | 管理项目成员 | ✓ | ✗ | ✗ |
| `task:create` | 创建任务 | ✓ | ✓ | ✗ |
| `task:edit` | 编辑任务 | ✓ | ✓ | ✗ |
| `task:delete` | 删除任务 | ✓ | 部分 | ✗ |
| `task:assign` | 分配任务 | ✓ | ✗ | ✗ |
| `overtime:approve` | 审批加班 | ✓ | ✗ | ✗ |
| `overtime:create` | 创建加班记录 | ✓ | ✓ | ✗ |

---

## 三、数据库设计

### 3.1 新增权限表（可选，用于细粒度权限）

```sql
-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    type VARCHAR(20) NOT NULL COMMENT '权限类型: system/project',
    description VARCHAR(500) COMMENT '权限描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL COMMENT '角色',
    permission_id VARCHAR(50) NOT NULL COMMENT '权限ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role, permission_id)
);
```

### 3.2 修改现有表

```sql
-- 项目成员表增加角色字段
ALTER TABLE sys_project_member 
ADD COLUMN role VARCHAR(50) DEFAULT 'member' COMMENT '项目内角色: owner/member';

-- 设置现有项目的负责人为项目成员中的owner
INSERT IGNORE INTO sys_project_member (project_id, user_id, role)
SELECT id, owner_id, 'owner' FROM sys_project;
```

---

## 四、后端实现

### 4.1 权限注解

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();  // 权限代码
    String projectIdParam() default "";  // 项目ID参数名
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();  // 允许的角色
}
```

### 4.2 权限拦截器

```java
@Component
public class PermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 检查权限注解
        // 验证用户权限
        // 返回结果
    }
}
```

### 4.3 权限服务

```java
@Service
public class PermissionService {
    // 检查系统权限
    public boolean hasPermission(String userId, String permission);
    
    // 检查项目权限
    public boolean hasProjectPermission(String userId, String projectId, String permission);
    
    // 判断是否是项目负责人
    public boolean isProjectOwner(String userId, String projectId);
    
    // 判断是否是项目成员
    public boolean isProjectMember(String userId, String projectId);
}
```

---

## 五、前端实现

### 5.1 权限 Store

```typescript
// stores/permission.ts
export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore();
  const projectStore = useProjectStore();
  
  // 系统权限检查
  const hasPermission = (permission: string): boolean => { ... };
  
  // 项目权限检查
  const hasProjectPermission = (projectId: string, permission: string): boolean => { ... };
  
  // 是否是项目负责人
  const isProjectOwner = (projectId: string): boolean => { ... };
  
  return { hasPermission, hasProjectPermission, isProjectOwner };
});
```

### 5.2 权限指令

```typescript
// directives/permission.ts
export const vPermission = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const permission = binding.value;
    if (!permissionStore.hasPermission(permission)) {
      el.remove();
    }
  }
};
```

### 5.3 路由守卫

```typescript
// router/guards.ts
router.beforeEach((to, from, next) => {
  const requiredPermission = to.meta.permission;
  if (requiredPermission && !hasPermission(requiredPermission)) {
    next('/forbidden');
  } else {
    next();
  }
});
```

---

## 六、实施任务列表

### [x] Task 1: 数据库权限表设计与创建 ✅ 已完成

- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 创建权限表 `sys_permission`
  - 创建角色权限关联表 `sys_role_permission`
  - 修改项目成员表增加角色字段
  - 初始化权限数据
- **Success Criteria**:
  - 数据库表创建成功
  - 初始权限数据插入成功
- **Test Requirements**:
  - `programmatic` TR-1.1: 表结构正确创建 ✅
  - `programmatic` TR-1.2: 初始权限数据存在 ✅

### [x] Task 2: 后端权限服务实现 ✅ 已完成

- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 创建 PermissionService
  - 实现权限检查方法
  - 创建权限注解
  - 创建 PermissionController
- **Success Criteria**:
  - 权限服务可正常工作
  - 权限检查方法返回正确结果
- **Test Requirements**:
  - `programmatic` TR-2.1: 权限检查方法返回正确结果 ✅

### [x] Task 3: 后端接口权限控制 ✅ 已完成

- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 为用户管理接口添加权限控制
  - 为项目管理接口添加权限控制
  - 为任务管理接口添加权限控制
- **Success Criteria**:
  - 所有敏感接口都有权限控制
- **Test Requirements**:
  - `programmatic` TR-3.1: 无权限用户访问敏感接口返回错误 ✅

### [x] Task 4: 前端权限 Store 实现 ✅ 已完成

- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 创建 permission store
  - 实现权限检查方法
  - 实现项目权限检查方法
- **Success Criteria**:
  - Store 可正常工作
  - 权限检查方法返回正确结果
- **Test Requirements**:
  - `programmatic` TR-4.1: Store 初始化成功 ✅
  - `programmatic` TR-4.2: 权限检查方法返回正确布尔值 ✅

### [x] Task 5: 前端权限指令和组件 ✅ 已完成

- **Priority**: P1
- **Depends On**: Task 4
- **Description**: 
  - 创建 v-permission 指令
  - 创建 v-role 指令
  - 创建 v-project-permission 指令
  - 创建 PermissionGuard 组件
- **Success Criteria**:
  - 指令和组件可正常工作
- **Test Requirements**:
  - `programmatic` TR-5.1: v-permission 指令正确隐藏元素 ✅
  - `programmatic` TR-5.2: PermissionGuard 组件正确显示/隐藏内容 ✅

### [x] Task 6: 前端页面权限控制 ✅ 已完成

- **Priority**: P1
- **Depends On**: Task 5
- **Description**: 
  - 团队管理页面添加权限控制
  - 项目详情页面添加权限控制
  - 任务操作添加权限控制
- **Success Criteria**:
  - 所有页面都有正确的权限控制
- **Test Requirements**:
  - `human-judgement` TR-6.1: 不同角色用户看到正确的UI ✅
  - `human-judgement` TR-6.2: 无权限操作按钮正确隐藏 ✅

### [x] Task 7: 路由权限守卫 ✅ 已完成

- **Priority**: P1
- **Depends On**: Task 4
- **Description**: 
  - 为路由添加权限元数据
  - 实现路由守卫
  - 创建无权限页面
- **Success Criteria**:
  - 无权限用户无法访问受限页面
- **Test Requirements**:
  - `programmatic` TR-7.1: 无权限访问受限页面重定向到403 ✅
  - `human-judgement` TR-7.2: 403页面显示友好提示 ✅

### [x] Task 8: 测试与验证 ✅ 已完成

- **Priority**: P2
- **Depends On**: Task 1-7
- **Description**: 
  - 验证权限功能
  - 确认所有功能正常工作
- **Success Criteria**:
  - 所有功能正常工作
- **Test Requirements**:
  - `programmatic` TR-8.1: 所有组件创建成功 ✅
  - `human-judgement` TR-8.2: 功能测试通过 ✅

---

## 七、权限矩阵总览

### 7.1 系统级权限矩阵

| 功能模块 | 操作 | admin | project-manager | member |
|---------|------|-------|-----------------|--------|
| **用户管理** | 查看 | ✓ | ✓ | 自己 |
| | 创建 | ✓ | ✗ | ✗ |
| | 编辑 | ✓ | ✗ | 自己 |
| | 删除 | ✓ | ✗ | ✗ |
| **项目管理** | 查看 | 全部 | 全部 | 参与的 |
| | 创建 | ✓ | ✓ | ✓ |
| | 编辑 | 全部 | 负责的 | 负责的 |
| | 删除 | 全部 | 负责的 | ✗ |
| **任务管理** | 查看 | 全部 | 全部 | 参与的 |
| | 创建 | 全部 | 参与的 | 参与的 |
| | 编辑 | 全部 | 参与的 | 分配的 |
| | 删除 | 全部 | 参与的 | 自己创建的 |
| **加班管理** | 查看 | 全部 | 全部 | 自己的 |
| | 创建 | 全部 | 参与的 | 参与的 |
| | 审批 | 全部 | 负责的项目 | 负责的项目 |
| **系统设置** | 查看 | ✓ | ✗ | ✗ |
| | 修改 | ✓ | ✗ | ✗ |

### 7.2 项目级权限矩阵

| 操作 | 项目负责人 | 项目成员 | 非成员 |
|------|-----------|---------|--------|
| 查看项目 | ✓ | ✓ | ✗ |
| 编辑项目 | ✓ | ✗ | ✗ |
| 删除项目 | ✓ | ✗ | ✗ |
| 管理成员 | ✓ | ✗ | ✗ |
| 创建任务 | ✓ | ✓ | ✗ |
| 编辑任务 | ✓ | ✓ | ✗ |
| 删除任务 | ✓ | 自己创建的 | ✗ |
| 分配任务 | ✓ | ✗ | ✗ |
| 审批加班 | ✓ | ✗ | ✗ |

---

## 八、注意事项

1. **向后兼容**: 确保现有功能不受影响
2. **性能优化**: 权限检查应该高效，避免多次数据库查询
3. **用户体验**: 无权限时显示友好提示，而不是直接报错
4. **日志记录**: 记录权限相关的操作日志
5. **测试覆盖**: 确保所有权限场景都有测试覆盖
