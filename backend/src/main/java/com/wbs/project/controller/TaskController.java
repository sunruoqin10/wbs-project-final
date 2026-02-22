package com.wbs.project.controller;

import com.wbs.project.annotation.RequirePermission;
import com.wbs.project.common.Result;
import com.wbs.project.entity.Task;
import com.wbs.project.service.PermissionService;
import com.wbs.project.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        taskService.updateDelayedStatus(tasks);
        return Result.success(tasks);
    }

    @GetMapping("/{id}")
    public Result<Task> getTaskById(@PathVariable String id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskService.updateTaskDelayedStatus(task);
        return Result.success(task);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<Task>> getTasksByProjectId(@PathVariable String projectId) {
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        taskService.updateDelayedStatus(tasks);
        return Result.success(tasks);
    }

    @GetMapping("/parent/{parentTaskId}")
    public Result<List<Task>> getSubTasks(@PathVariable String parentTaskId) {
        List<Task> tasks = taskService.getSubTasks(parentTaskId);
        taskService.updateDelayedStatus(tasks);
        return Result.success(tasks);
    }

    @GetMapping("/status/{status}")
    public Result<List<Task>> getTasksByStatus(@PathVariable String status) {
        List<Task> tasks = taskService.getTasksByStatus(status);
        taskService.updateDelayedStatus(tasks);
        return Result.success(tasks);
    }

    @GetMapping("/assignee/{assigneeId}")
    public Result<List<Task>> getTasksByAssigneeId(@PathVariable String assigneeId) {
        List<Task> tasks = taskService.getTasksByAssigneeId(assigneeId);
        taskService.updateDelayedStatus(tasks);
        return Result.success(tasks);
    }

    @PostMapping
    @RequirePermission("task:create")
    public Result<Task> createTask(
            @RequestBody Task task,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId != null && !permissionService.isProjectMember(userId, task.getProjectId())) {
                return Result.error("无权限在此项目中创建任务");
            }
            Task createdTask = taskService.createTask(task);
            return Result.success("任务创建成功", createdTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Task> updateTask(
            @PathVariable String id, 
            @RequestBody Task task,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Task existingTask = taskService.getTaskById(id);
            if (existingTask == null) {
                return Result.error("任务不存在");
            }
            if (userId != null && !permissionService.isProjectMember(userId, existingTask.getProjectId())) {
                return Result.error("无权限编辑此任务");
            }
            Task updatedTask = taskService.updateTask(id, task);
            return Result.success("任务更新成功", updatedTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        try {
            Task existingTask = taskService.getTaskById(id);
            if (existingTask == null) {
                return Result.error("任务不存在");
            }
            if ("admin".equals(userRole)) {
                taskService.deleteTask(id);
                return Result.success();
            }
            if (userId != null && permissionService.isProjectOwner(userId, existingTask.getProjectId())) {
                taskService.deleteTask(id);
                return Result.success();
            }
            if (userId != null && userId.equals(existingTask.getAssigneeId())) {
                taskService.deleteTask(id);
                return Result.success();
            }
            return Result.error("无权限删除此任务");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateTaskStatus(
            @PathVariable String id, 
            @RequestBody StatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Task existingTask = taskService.getTaskById(id);
            if (existingTask == null) {
                return Result.error("任务不存在");
            }
            if (userId != null && !permissionService.isProjectMember(userId, existingTask.getProjectId())) {
                return Result.error("无权限更新此任务状态");
            }
            taskService.updateTaskStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PatchMapping("/{id}/progress")
    public Result<Void> updateTaskProgress(
            @PathVariable String id, 
            @RequestBody ProgressRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Task existingTask = taskService.getTaskById(id);
            if (existingTask == null) {
                return Result.error("任务不存在");
            }
            if (userId != null && !permissionService.isProjectMember(userId, existingTask.getProjectId())) {
                return Result.error("无权限更新此任务进度");
            }
            taskService.updateTaskProgress(id, request.getProgress());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<TaskStats> getTaskStats() {
        TaskStats stats = new TaskStats();
        stats.setTotalTasks(taskService.getTotalTasks());
        stats.setTodoTasks(taskService.getTasksCountByStatus("todo"));
        stats.setInProgressTasks(taskService.getTasksCountByStatus("in-progress"));
        stats.setDoneTasks(taskService.getTasksCountByStatus("done"));
        return Result.success(stats);
    }

    @GetMapping("/delayed")
    public Result<List<Task>> getDelayedTasks(
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted) {
        List<Task> delayedTasks = taskService.getDelayedTasks(projectId, includeCompleted);
        return Result.success(delayedTasks);
    }

    @GetMapping("/project/{projectId}/delay-stats")
    public Result<TaskService.DelayStats> getProjectDelayStats(@PathVariable String projectId) {
        TaskService.DelayStats stats = taskService.getProjectDelayStats(projectId);
        return Result.success(stats);
    }

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

    @PostMapping("/{id}/delay")
    public Result<Task> recordTaskDelay(
        @PathVariable String id,
        @RequestBody DelayRequest request,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Task existingTask = taskService.getTaskById(id);
            if (existingTask == null) {
                return Result.error("任务不存在");
            }
            if (userId != null && !permissionService.isProjectMember(userId, existingTask.getProjectId())) {
                return Result.error("无权限记录此任务延期");
            }
            Task updatedTask = taskService.recordTaskDelay(id, request.getNewEndDate(), request.getDelayReason());
            return Result.success("延期记录成功", updatedTask);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    public static class StatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ProgressRequest {
        private Integer progress;

        public Integer getProgress() {
            return progress;
        }

        public void setProgress(Integer progress) {
            this.progress = progress;
        }
    }

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
