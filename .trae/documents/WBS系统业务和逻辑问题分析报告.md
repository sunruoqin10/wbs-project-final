# WBS系统业务和逻辑问题分析报告

***

## 🔒 一、权限控制问题

| 问题                        | 描述                                                                                                                                    | 影响                          |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| **1. Controller 层缺少权限校验** | 所有Controller（如`OvertimeController`、`ProjectController`等）没有使用 `@RequireRole` 或 `@RequirePermission` 注解，只是依赖 `AuthInterceptor` 的简单JWT验证 | ❌ 任何登录用户都可以访问所有API，存在严重安全隐患 |
| **2. 加班审批权限过于简单**         | `OvertimeService.validateApprover()` 只验证项目负责人，没有考虑多层级审批（如部门经理、HR）                                                                     | ⚠️ 缺少多级审批流程                 |
| **3. 缺少数据访问权限控制**         | 用户可以查看其他用户的加班记录和任务详情                                                                                                                  | ⚠️ 隐私泄露风险                   |

***

## 📊 二、统计逻辑不一致

| 问题               | 描述                                                                                                                | 影响                | 状态    |
| ---------------- | ----------------------------------------------------------------------------------------------------------------- | ----------------- | ----- |
| **4. 前端和后端重复统计** | 后端 `OvertimeService.getStats()` 提供了统计数据，但前端 `OvertimeManagement.vue` 又自己在本地重新计算了一遍                                | ⚠️ 浪费资源，可能导致数据不一致 | ✅ 已解决 |
| **5. 统计状态筛选不完整** | 后端 `sumHoursGroupByUser`、`sumHoursGroupByDate`、`sumHoursGroupByType` 等SQL查询可能还没有添加 `status = 'approved'` 条件（需要检查） | ⚠️ 可能导致已拒绝的记录参与统计 | ✅ 已解决 |

***

## 📧 三、邮件通知问题

| 问题                | 描述                                       | 影响                | 状态    |
| ----------------- | ---------------------------------------- | ----------------- | ----- |
| **6. 邮件发送缺少重试机制** | `EmailNotificationService` 发送邮件失败时没有重试逻辑 | ⚠️ 网络波动可能导致重要通知丢失 | |
| **7. 邮件发送没有异步处理** | 邮件发送是同步的，会阻塞请求响应                         | ⚠️ 影响用户体验，增加响应时间  | |
| **8. 缺少邮件发送记录**   | 没有数据库表记录邮件发送状态、发送时间等                     | ⚠️ 无法追踪邮件是否成功送达   | ✅ 已解决 |

***

## 🔄 四、任务和项目管理问题

| 问题                 | 描述                                                                      | 影响            |
| ------------------ | ----------------------------------------------------------------------- | ------------- |
| **9. 任务删除缺少软删除**   | `TaskService.deleteTask()` 是物理删除，没有审计日志                                 | ❌ 数据丢失后无法恢复   |
| **10. 项目删除缺少软删除**  | `ProjectService.deleteProject()` 也是物理删除                                 | ❌ 数据丢失后无法恢复   |
| **11. 任务状态转移缺少验证** | 没有验证任务状态转移的合理性（如 `done` 状态不能变回 `todo`）                                  | ⚠️ 可能导致业务逻辑混乱 |
| **12. 进度和状态不一致**   | `TaskService.updateTaskProgress()` 注释说"不再自动更新状态"，但业务逻辑上进度100%时应该自动标记为完成 | ⚠️ 用户体验不好     |

***

## ⏰ 五、延期管理问题

| 问题                  | 描述                                                                        | 影响          |
| ------------------- | ------------------------------------------------------------------------- | ----------- |
| **13. 延期通知可能重复发送**  | `DelayNotificationScheduler` 虽然有 `DelayNotificationRecord` 去重，但边缘情况下可能有问题 | ⚠️ 可能造成邮件轰炸 |
| **14. 延期天数计算可能不准确** | `TaskService.updateTaskDelayedStatus()` 中 `delayedDays` 的累加逻辑在多次延期时可能有问题  | ⚠️ 统计数据不准确  |
| **15. 缺少延期审批流程**    | 任务延期没有审批流程，任何人都可以修改结束日期                                                   | ❌ 缺少管控      |

***

## 💼 六、加班管理问题

| 问题                  | 描述                                                          | 影响        |
| ------------------- | ----------------------------------------------------------- | --------- |
| **16. 加班时长计算不考虑午休** | `OvertimeService.calculateHours()` 简单地计算时间差，没有扣除午休时间        | ⚠️ 统计不准确  |
| **17. 加班记录编辑权限**    | `OvertimeService.updateRecord()` 只允许待审批状态修改，但已拒绝的申请应该允许重新提交 | ⚠️ 用户体验不好 |
| **18. 缺少加班额度管理**    | 没有月度/季度加班额度限制                                               | ⚠️ 缺少管控   |

***

## 🏗️ 七、代码架构问题

| 问题                | 描述                                                     | 影响                | 状态    |
| ----------------- | ------------------------------------------------------ | ----------------- | ----- |
| **19. 缺少统一的异常处理** | 没有 `@ControllerAdvice` 全局异常处理器，各个Controller自己try-catch | ⚠️ 代码重复，错误响应格式不统一 | ✅ 已解决 |
| **20. 缺少参数校验**    | Controller层没有使用 `@Valid` 或 `@Validated` 注解进行参数校验       | ⚠️ 可能导致脏数据进入系统    | |
| **21. SQL注入风险**   | MyBatis mapper中有些SQL可能使用了 `${}` 而非 `#{}`（需要检查）         | ❌ 安全隐患            | |
| **22. 缺少日志规范**    | 有些地方用 `System.out.println`，有些用 `log.info()`，不统一        | ⚠️ 日志管理混乱         | |
| **23. 缺少单元测试**    | 没有看到测试代码                                               | ⚠️ 代码质量无法保证       | |

***

## 🔧 建议修复优先级

| 优先级  | 问题编号 | 问题              |
| ---- | ---- | --------------- |
| 🔴 高 | 1    | Controller层权限控制 |
| 🔴 高 | 9    | 数据软删除           |
| 🔴 高 | 10   | 数据软删除           |
| 🟡 中 | 4    | 前后端统计统一 ✅       |
| 🟡 中 | 7    | 邮件异步发送          |
| 🟡 中 | 8    | 邮件发送记录 ✅        |
| 🟡 中 | 19   | 全局异常处理 ✅        |
| 🟡 中 | 20   | 参数校验            |
| 🟢 低 | 其他   | 优化和改进           |

***

## ✅ 已解决问题

### 问题4和5：统计逻辑不一致（已修复）

**修复内容：**

1. **后端SQL查询状态筛选** - 检查并确认所有统计查询已添加 `status = 'approved'` 条件：

   * `sumHoursGroupByUser` ✅

   * `sumHoursGroupByProject` ✅

   * `sumHoursGroupByDate` ✅

   * `sumHoursGroupByType` ✅

   * `sumHoursGroupByTask` ✅

   * `sumHoursByUserId` ✅

   * `sumHoursByProjectId` ✅

   * `sumHoursByTaskId` ✅

2. **前端使用后端统计数据** - 修改 `OvertimeManagement.vue`，不再本地重复计算，直接使用 `overtimeStore.stats`

3. **数据类型转换** - 在 `apiService.getOvertimeStats()` 中添加数据转换逻辑，正确处理：

   * BigDecimal → number 转换

   * 字段名兼容（hours/totalHours, count/recordCount）

**修改的文件：**

* `backend/src/main/resources/mapper/OvertimeMapper.xml`

* `frontend/src/views/OvertimeManagement.vue`

* `frontend/src/services/api.ts`

***

### 问题8：缺少邮件发送记录（已修复）

**修复内容：**

1. **创建邮件发送记录实体类** `EmailLog.java` - 包含收件人、抄送人、主题、模板名称、发送状态、错误信息、发送时间等字段

2. **创建 EmailLogMapper 接口** - 提供插入、按ID查询、按收件人查询、按状态查询、按日期范围查询、按日期删除等方法

3. **创建 EmailLogMapper.xml 映射文件** - 完整的 SQL 映射配置

4. **创建数据库迁移脚本** `add_email_log_table.sql` - 创建 `email_log` 表，包含必要的索引和注释

5. **修改 EmailService** - 在 `sendEmail()` 和 `sendSimpleEmail()` 方法中添加邮件发送记录逻辑，使用 try-catch-finally 块确保无论发送成功或失败都会记录

**修改的文件：**

* `backend/src/main/java/com/wbs/project/entity/EmailLog.java`

* `backend/src/main/java/com/wbs/project/mapper/EmailLogMapper.java`

* `backend/src/main/resources/mapper/EmailLogMapper.xml`

* `backend/add_email_log_table.sql`

* `backend/src/main/java/com/wbs/project/service/EmailService.java`

***

### 问题19：缺少统一的异常处理（已修复）

**修复内容：**

1. **创建自定义业务异常类** `BusinessException.java` - 支持自定义错误码和错误消息

2. **创建全局异常处理器** `GlobalExceptionHandler.java` - 使用 `@RestControllerAdvice` 注解实现全局异常处理，支持多种异常类型：
   - `BusinessException`：业务异常
   - `MethodArgumentNotValidException`：参数校验异常
   - `BindException`：参数绑定异常
   - `IllegalArgumentException`：非法参数异常
   - `Exception`：系统异常

3. **移除所有 Controller 中重复的 try-catch 块** - 简化代码，统一异常处理

**修改的文件：**

* `backend/src/main/java/com/wbs/project/exception/BusinessException.java`

* `backend/src/main/java/com/wbs/project/exception/GlobalExceptionHandler.java`

* `backend/src/main/java/com/wbs/project/controller/OvertimeController.java`

* `backend/src/main/java/com/wbs/project/controller/ProjectController.java`

* `backend/src/main/java/com/wbs/project/controller/TaskController.java`

* `backend/src/main/java/com/wbs/project/controller/UserController.java`

* `backend/src/main/java/com/wbs/project/controller/DelayNotificationController.java`

***

## 📝 备注

* 本报告基于对代码的静态分析生成

* 部分问题需要结合实际业务场景进一步确认

* 建议优先修复高优先级问题

