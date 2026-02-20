package com.wbs.project.service;

import com.wbs.project.entity.Task;
import com.wbs.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务Service
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    /**
     * 查询所有任务
     */
    public List<Task> getAllTasks() {
        return taskMapper.selectAll();
    }

    /**
     * 根据ID查询任务
     */
    public Task getTaskById(String id) {
        return taskMapper.selectById(id);
    }

    /**
     * 根据项目ID查询任务列表
     */
    public List<Task> getTasksByProjectId(String projectId) {
        return taskMapper.selectByProjectId(projectId);
    }

    /**
     * 根据父任务ID查询子任务列表
     */
    public List<Task> getSubTasks(String parentTaskId) {
        return taskMapper.selectByParentTaskId(parentTaskId);
    }

    /**
     * 根据状态查询任务列表
     */
    public List<Task> getTasksByStatus(String status) {
        return taskMapper.selectByStatus(status);
    }

    /**
     * 根据分配人ID查询任务列表
     */
    public List<Task> getTasksByAssigneeId(String assigneeId) {
        return taskMapper.selectByAssigneeId(assigneeId);
    }

    /**
     * 创建任务
     */
    @Transactional
    public Task createTask(Task task) {
        task.setId("t" + UUID.randomUUID().toString().substring(0, 8));
        // 只有当进度为null时，才设置默认值0
        if (task.getProgress() == null) {
            task.setProgress(0);
        }
        task.setActualHours(java.math.BigDecimal.ZERO);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.insert(task);
        return task;
    }

    /**
     * 更新任务
     */
    @Transactional
    public Task updateTask(String id, Task task) {
        Task existingTask = taskMapper.selectById(id);
        if (existingTask == null) {
            throw new RuntimeException("任务不存在");
        }

        task.setId(id);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.update(task);
        return taskMapper.selectById(id);
    }

    /**
     * 删除任务
     * 注意：需要级联删除相关数据（在Java代码中处理）
     */
    @Transactional
    public void deleteTask(String id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 处理级联删除逻辑
        // 1. 递归删除所有子任务
        deleteSubtasksRecursive(id);

        // 2. 删除任务依赖关系（如果有相关的mapper方法，在这里调用）
        // 3. 删除任务的评论（如果有相关的mapper方法，在这里调用）
        // 4. 删除任务的附件（如果有相关的mapper方法，在这里调用）
        // 5. 删除任务的标签（如果有相关的mapper方法，在这里调用）

        // 最后删除任务本身
        taskMapper.deleteById(id);
    }

    /**
     * 递归删除子任务
     */
    private void deleteSubtasksRecursive(String parentTaskId) {
        // 查询所有子任务
        List<Task> subtasks = taskMapper.selectByParentTaskId(parentTaskId);

        // 递归删除每个子任务
        for (Task subtask : subtasks) {
            // 先删除该子任务的子任务
            deleteSubtasksRecursive(subtask.getId());

            // 删除该子任务
            taskMapper.deleteById(subtask.getId());
        }
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public void updateTaskStatus(String id, String status) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.update(task);

        // 如果任务完成，更新进度为100%
        if ("done".equals(status)) {
            task.setProgress(100);
            taskMapper.update(task);
        }
    }

    /**
     * 更新任务进度
     */
    @Transactional
    public void updateTaskProgress(String id, Integer progress) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        task.setProgress(progress);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.update(task);

        // 不再自动更新状态，状态应该由用户手动控制
        // 移除了以下逻辑：
        // - 进度为100%时自动设置为"done"
        // - 进度大于0且状态为"todo"时自动设置为"in-progress"
    }

    /**
     * 获取任务总数
     */
    public int getTotalTasks() {
        return taskMapper.countTotal();
    }

    /**
     * 获取指定项目的任务数
     */
    public int getTasksCountByProject(String projectId) {
        return taskMapper.countByProjectId(projectId);
    }

    /**
     * 获取指定状态的任务数
     */
    public int getTasksCountByStatus(String status) {
        return taskMapper.countByStatus(status);
    }

    // ==================== 延期管理方法 ====================

    /**
     * 更新任务延期状态（运行时计算）
     */
    public void updateDelayedStatus(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        for (Task task : tasks) {
            updateTaskDelayedStatus(task, today);
        }
    }

    /**
     * 更新单个任务的延期状态
     */
    public void updateTaskDelayedStatus(Task task) {
        updateTaskDelayedStatus(task, LocalDate.now());
    }

    /**
     * 更新单个任务的延期状态（内部方法）
     */
    private void updateTaskDelayedStatus(Task task, LocalDate today) {
        if (task == null || task.getEndDate() == null) {
            return;
        }

        boolean isDelayed = task.getEndDate().isBefore(today)
            && !"done".equals(task.getStatus());
        task.setIsDelayed(isDelayed);

        // 计算延期天数
        if (isDelayed) {
            // 如果有原始结束日期，使用原始结束日期计算
            if (task.getOriginalEndDate() != null) {
                long days = ChronoUnit.DAYS.between(task.getOriginalEndDate(), today);
                task.setDelayedDays((int) days);
            } else {
                // 否则使用当前结束日期计算
                long days = ChronoUnit.DAYS.between(task.getEndDate(), today);
                task.setDelayedDays((int) days);
            }
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
            .filter(t -> t.getIsDelayed() || (t.getDelayedDays() != null && t.getDelayedDays() > 0))
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

    /**
     * 记录任务延期
     */
    @Transactional
    public Task recordTaskDelay(String taskId, String newEndDateStr, String delayReason) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        LocalDate oldEndDate = task.getEndDate();
        LocalDate newEndDate = LocalDate.parse(newEndDateStr);

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
        task.setDelayReason(delayReason);
        task.setLastDelayDate(LocalDate.now());
        task.setUpdatedAt(LocalDateTime.now());

        // 更新延期状态
        updateTaskDelayedStatus(task);

        taskMapper.update(task);
        return task;
    }

    /**
     * 延期统计信息类
     */
    public static class DelayStats {
        private Integer totalTasks;
        private Integer delayedTasks;
        private Double delayRate;
        private Integer totalDelayedDays;
        private Integer criticalDelayedTasks;

        public Integer getTotalTasks() {
            return totalTasks;
        }

        public void setTotalTasks(Integer totalTasks) {
            this.totalTasks = totalTasks;
        }

        public Integer getDelayedTasks() {
            return delayedTasks;
        }

        public void setDelayedTasks(Integer delayedTasks) {
            this.delayedTasks = delayedTasks;
        }

        public Double getDelayRate() {
            return delayRate;
        }

        public void setDelayRate(Double delayRate) {
            this.delayRate = delayRate;
        }

        public Integer getTotalDelayedDays() {
            return totalDelayedDays;
        }

        public void setTotalDelayedDays(Integer totalDelayedDays) {
            this.totalDelayedDays = totalDelayedDays;
        }

        public Integer getCriticalDelayedTasks() {
            return criticalDelayedTasks;
        }

        public void setCriticalDelayedTasks(Integer criticalDelayedTasks) {
            this.criticalDelayedTasks = criticalDelayedTasks;
        }
    }
}
