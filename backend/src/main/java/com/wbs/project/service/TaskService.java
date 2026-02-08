package com.wbs.project.service;

import com.wbs.project.entity.Task;
import com.wbs.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
}
