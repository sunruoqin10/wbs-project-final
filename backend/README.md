# WBS 项目管理系统 - 后端

基于 Spring Boot + MyBatis + MySQL 的项目管理系统后端服务。

## 技术栈

- **Spring Boot 3.2.0** - 基础框架
- **MyBatis 3.0.3** - ORM框架
- **MySQL 8.2.0** - 关系型数据库
- **Maven** - 项目构建工具
- **Lombok** - 简化代码

## 项目结构

```
backend/
├── src/main/java/com/wbs/project/
│   ├── ProjectApplication.java          # 主应用类
│   ├── common/                           # 通用类
│   │   └── Result.java                   # 统一响应结果类
│   ├── config/                           # 配置类
│   │   └── CorsConfig.java               # 跨域配置
│   ├── controller/                       # 控制器层
│   │   ├── UserController.java           # 用户控制器
│   │   ├── ProjectController.java        # 项目控制器
│   │   └── TaskController.java           # 任务控制器
│   ├── service/                          # 服务层
│   │   ├── UserService.java              # 用户服务
│   │   ├── ProjectService.java           # 项目服务
│   │   └── TaskService.java              # 任务服务
│   ├── mapper/                           # 数据访问层
│   │   ├── UserMapper.java               # 用户Mapper
│   │   ├── ProjectMapper.java            # 项目Mapper
│   │   └── TaskMapper.java               # 任务Mapper
│   ├── entity/                           # 实体类
│   │   ├── User.java                     # 用户实体
│   │   ├── Project.java                  # 项目实体
│   │   ├── Task.java                     # 任务实体
│   │   ├── Comment.java                  # 评论实体
│   │   └── Attachment.java               # 附件实体
│   └── enums/                            # 枚举类
│       ├── UserRole.java                 # 用户角色枚举
│       ├── ProjectStatus.java            # 项目状态枚举
│       ├── TaskStatus.java               # 任务状态枚举
│       └── Priority.java                 # 优先级枚举
├── src/main/resources/
│   ├── application.yml                   # 应用配置文件
│   └── mapper/                           # MyBatis映射文件
│       ├── UserMapper.xml
│       ├── ProjectMapper.xml
│       └── TaskMapper.xml
└── pom.xml                               # Maven配置文件
```

## 数据库设计

### 数据表清单

1. **sys_user** - 用户表
2. **sys_project** - 项目表
3. **sys_task** - 任务表
4. **sys_comment** - 评论表
5. **sys_attachment** - 附件表
6. **sys_project_member** - 项目成员关联表
7. **sys_project_tag** - 项目标签表
8. **sys_task_tag** - 任务标签表
9. **sys_task_dependency** - 任务依赖关系表

### 关键设计

- ✅ 外键和级联操作在Java程序中处理，不使用数据库外键约束
- ✅ 使用JSON字段存储列表数据（如技能列表）
- ✅ 使用关联表处理多对多关系
- ✅ 支持软删除（通过状态字段）

## API 接口文档

### 基础路径
- 服务器地址: `http://localhost:8080`
- API前缀: `/api`

### 用户管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 根据ID获取用户 |
| POST | `/api/users/batch` | 批量获取用户 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/users/count` | 获取用户总数 |

### 项目管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects` | 获取所有项目 |
| GET | `/api/projects/{id}` | 根据ID获取项目 |
| GET | `/api/projects/status/{status}` | 根据状态获取项目 |
| GET | `/api/projects/owner/{ownerId}` | 根据负责人获取项目 |
| GET | `/api/projects/member/{userId}` | 获取用户参与的项目 |
| POST | `/api/projects` | 创建项目 |
| PUT | `/api/projects/{id}` | 更新项目 |
| DELETE | `/api/projects/{id}` | 删除项目 |
| GET | `/api/projects/stats` | 获取项目统计信息 |

### 任务管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tasks` | 获取所有任务 |
| GET | `/api/tasks/{id}` | 根据ID获取任务 |
| GET | `/api/tasks/project/{projectId}` | 根据项目ID获取任务 |
| GET | `/api/tasks/parent/{parentTaskId}` | 获取子任务 |
| GET | `/api/tasks/status/{status}` | 根据状态获取任务 |
| GET | `/api/tasks/assignee/{assigneeId}` | 根据分配人获取任务 |
| POST | `/api/tasks` | 创建任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态 |
| PATCH | `/api/tasks/{id}/progress` | 更新任务进度 |
| GET | `/api/tasks/stats` | 获取任务统计信息 |

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_webwbs
    username: root
    password: your_password
```

### 3. 创建数据库

```sql
CREATE DATABASE db_webwbs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 运行项目

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 5. 访问测试

启动成功后，访问：`http://localhost:8080/api`

## 响应格式

所有接口返回统一的JSON格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

## 数据字典

### 用户角色 (UserRole)
- `admin` - 管理员
- `project-manager` - 项目经理
- `member` - 成员

### 项目状态 (ProjectStatus)
- `planning` - 计划中
- `active` - 进行中
- `completed` - 已完成
- `on-hold` - 已暂停

### 任务状态 (TaskStatus)
- `todo` - 待办
- `in-progress` - 进行中
- `done` - 已完成

### 优先级 (Priority)
- `low` - 低
- `medium` - 中
- `high` - 高
- `urgent` - 紧急
- `critical` - 严重

## 注意事项

1. ⚠️ 外键和级联操作在Java代码中处理，不在数据库层面定义外键
2. ⚠️ 删除操作需要手动处理关联数据的删除
3. ⚠️ 建议生产环境使用连接池（如Druid、HikariCP）
4. ⚠️ 建议生产环境添加Spring Security进行权限控制
5. ⚠️ 建议生产环境配置完善的日志系统

## 后续开发计划

- [ ] 添加评论管理功能
- [ ] 添加附件管理功能
- [ ] 添加标签管理功能
- [ ] 添加任务依赖管理
- [ ] 添加项目成员管理
- [ ] 添加统计分析功能
- [ ] 集成Spring Security
- [ ] 添加Redis缓存
- [ ] 添加定时任务支持
- [ ] 添加邮件通知功能

## 许可证

MIT License
