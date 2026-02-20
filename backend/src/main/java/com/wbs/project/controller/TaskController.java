package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Task;
import com.wbs.project.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务Controller
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 获取所有任务
     * GET /api/tasks
     */
    @GetMapping
    public Result<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        taskService.updateDelayedStatus(tasks); // 计算延期状态
        return Result.success(tasks);
    }

    /**
     * 根据ID获取任务
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public Result<Task> getTaskById(@PathVariable String id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskService.updateTaskDelayedStatus(task); // 计算延期状态
        return Result.success(task);
    }

    /**
     * 根据项目ID获取任务列表
     * GET /api/tasks/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public Result<List<Task>> getTasksByProjectId(@PathVariable String projectId) {
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        taskService.updateDelayedStatus(tasks); // 计算延期状态
        return Result.success(tasks);
    }

    /**
     * 根据父任务ID获取子任务列表
     * GET /api/tasks/parent/{parentTaskId}
     */
    @GetMapping("/parent/{parentTaskId}")
    public Result<List<Task>> getSubTasks(@PathVariable String parentTaskId) {
        List<Task> tasks = taskService.getSubTasks(parentTaskId);
        taskService.updateDelayedStatus(tasks); // 计算延期状态
        return Result.success(tasks);
    }

    /**
     * 根据状态获取任务列表
     * GET /api/tasks/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<Task>> getTasksByStatus(@PathVariable String status) {
        List<Task> tasks = taskService.getTasksByStatus(status);
        taskService.updateDelayedStatus(tasks); // 计算延期状态
        return Result.success(tasks);
    }

    /**
     * 根据分配人获取任务列表
     * GET /api/tasks/assignee/{assigneeId}
     */
    @GetMapping("/assignee/{assigneeId}")
    public Result<List<Task>> getTasksByAssigneeId(@PathVariable String assigneeId) {
        List<Task> tasks = taskService.getTasksByAssigneeId(assigneeId);
        taskService.updateDelayedStatus(tasks); // 计算延期状态
        return Result.success(tasks);
    }

    /**
     * 创建任务
     * POST /api/tasks
     */
    @PostMapping
    public Result<Task> createTask(@RequestBody Task task) {
        try {
            Task createdTask = taskService.createTask(task);
            return Result.success("任务创建成功", createdTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新任务
     * PUT /api/tasks/{id}
     */
    @PutMapping("/{id}")
    public Result<Task> updateTask(@PathVariable String id, @RequestBody Task task) {
        try {
            Task updatedTask = taskService.updateTask(id, task);
            return Result.success("任务更新成功", updatedTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除任务
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable String id) {
        try {
            taskService.deleteTask(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新任务状态
     * PATCH /api/tasks/{id}/status
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateTaskStatus(@PathVariable String id, @RequestBody StatusRequest request) {
        try {
            taskService.updateTaskStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新任务进度
     * PATCH /api/tasks/{id}/progress
     */
    @PatchMapping("/{id}/progress")
    public Result<Void> updateTaskProgress(@PathVariable String id, @RequestBody ProgressRequest request) {
        try {
            taskService.updateTaskProgress(id, request.getProgress());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取任务统计信息
     * GET /api/tasks/stats
     */
    @GetMapping("/stats")
    public Result<TaskStats> getTaskStats() {
        TaskStats stats = new TaskStats();
        stats.setTotalTasks(taskService.getTotalTasks());
        stats.setTodoTasks(taskService.getTasksCountByStatus("todo"));
        stats.setInProgressTasks(taskService.getTasksCountByStatus("in-progress"));
        stats.setDoneTasks(taskService.getTasksCountByStatus("done"));
        return Result.success(stats);
    }

    // ==================== 延期管理 API ====================

    /**
     * 获取延期任务列表
     * GET /api/tasks/delayed?projectId=xxx&includeCompleted=false
     */
    @GetMapping("/delayed")
    public Result<List<Task>> getDelayedTasks(
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted) {
        List<Task> delayedTasks = taskService.getDelayedTasks(projectId, includeCompleted);
        return Result.success(delayedTasks);
    }

    /**
     * 获取项目延期统计
     * GET /api/tasks/project/{projectId}/delay-stats
     */
    @GetMapping("/project/{projectId}/delay-stats")
    public Result<TaskService.DelayStats> getProjectDelayStats(@PathVariable String projectId) {
        TaskService.DelayStats stats = taskService.getProjectDelayStats(projectId);
        return Result.success(stats);
    }

    /**
     * 调试任务延期计算
     * GET /api/tasks/{id}/debug-delay
     */
    @GetMapping("/{id}/debug-delay")
    public Result<String> debugTaskDelay(@PathVariable String id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("任务: ").append(task.getTitle()).append("\n");
        debugInfo.append("自身延期: ").append(task.getDelayedDays()).append(" 天\n");
        debugInfo.append("子任务延期数: ").append(task.getChildrenDelayedCount()).append("\n");
        debugInfo.append("子任务累计延期: ").append(task.getChildrenTotalDelayedDays()).append(" 天\n");

        // 获取子任务列表
        List<Task> subtasks = taskService.getSubTasks(id);
        debugInfo.append("\n直接子任务列表:\n");
        for (Task subtask : subtasks) {
            debugInfo.append("  - ").append(subtask.getTitle())
                    .append(": 延期=").append(subtask.getDelayedDays())
                    .append(", 子任务累计=").append(subtask.getChildrenTotalDelayedDays())
                    .append("\n");
        }

        return Result.success(debugInfo.toString());
    }

    /**
     * 记录任务延期
     * POST /api/tasks/{id}/delay
     */
    @PostMapping("/{id}/delay")
    public Result<Task> recordTaskDelay(
        @PathVariable String id,
        @RequestBody DelayRequest request) {
        try {
            Task updatedTask = taskService.recordTaskDelay(id, request.getNewEndDate(), request.getDelayReason());
            return Result.success("延期记录成功", updatedTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 状态请求类
     */
    public static class StatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 进度请求类
     */
    public static class ProgressRequest {
        private Integer progress;

        public Integer getProgress() {
            return progress;
        }

        public void setProgress(Integer progress) {
            this.progress = progress;
        }
    }

    /**
     * 任务统计信息类
     */
    public static class TaskStats {
        private Integer totalTasks;
        private Integer todoTasks;
        private Integer inProgressTasks;
        private Integer doneTasks;

        public Integer getTotalTasks() {
            return totalTasks;
        }

        public void setTotalTasks(Integer totalTasks) {
            this.totalTasks = totalTasks;
        }

        public Integer getTodoTasks() {
            return todoTasks;
        }

        public void setTodoTasks(Integer todoTasks) {
            this.todoTasks = todoTasks;
        }

        public Integer getInProgressTasks() {
            return inProgressTasks;
        }

        public void setInProgressTasks(Integer inProgressTasks) {
            this.inProgressTasks = inProgressTasks;
        }

        public Integer getDoneTasks() {
            return doneTasks;
        }

        public void setDoneTasks(Integer doneTasks) {
            this.doneTasks = doneTasks;
        }
    }

    /**
     * 延期请求类
     */
    public static class DelayRequest {
        private String newEndDate;
        private String delayReason;

        public String getNewEndDate() {
            return newEndDate;
        }

        public void setNewEndDate(String newEndDate) {
            this.newEndDate = newEndDate;
        }

        public String getDelayReason() {
            return delayReason;
        }

        public void setDelayReason(String delayReason) {
            this.delayReason = delayReason;
        }
    }
}
