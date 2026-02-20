import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Task, SubtaskSummary } from '@/types';
import apiService from '@/services/api';
import { tasks as mockTasks } from '@/data/tasks';

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([]);
  const currentTask = ref<Task | null>(null);
  const loading = ref(false);
  const loaded = ref(false);

  // 加载所有任务
  const loadTasks = async (projectId?: string) => {
    try {
      loading.value = true;
      const loadedTasks = await apiService.getTasks(projectId);
      // 调试：打印任务数据
      console.log('=== 加载的任务数据 ===');
      loadedTasks.forEach(t => {
        if (t.title.includes('HESS') || t.title.includes('后端API')) { // 只打印相关任务
          console.log(`任务: ${t.title}`, {
            isDelayed: t.isDelayed,
            delayedDays: t.delayedDays,
            childrenDelayedCount: t.childrenDelayedCount,
            childrenTotalDelayedDays: t.childrenTotalDelayedDays
          });
        }
      });
      // 过滤掉可能的 null 值
      tasks.value = loadedTasks.filter(t => t !== null && t !== undefined);
      // 强制将所有待办状态的任务进度设置为 0
      enforceTodoProgressZero();
      // 强制将所有已完成状态的任务进度设置为 100
      enforceDoneProgressHundred();
      // 重新计算所有父任务的状态和进度
      await recalculateAllParentTasks();
      // 更新所有父任务的时间范围
      await updateAllParentTaskDates();
      loaded.value = true;
    } catch (error) {
      // API 调用失败时使用 mock 数据（开发阶段）
      console.warn('API 未连接，使用 mock 数据:', error);
      tasks.value = mockTasks;
      // 强制将所有待办状态的任务进度设置为 0
      enforceTodoProgressZero();
      // 强制将所有已完成状态的任务进度设置为 100
      enforceDoneProgressHundred();
      // 重新计算所有父任务的状态和进度
      await recalculateAllParentTasks();
      // 更新所有父任务的时间范围
      await updateAllParentTaskDates();
      loaded.value = true;
    } finally {
      loading.value = false;
    }
  };

  // 重新计算所有父任务的状态和进度
  const recalculateAllParentTasks = async () => {
    // 获取所有有子任务的任务（即父任务）
    const parentTaskIds = new Set<string>();
    tasks.value.forEach(task => {
      if (task && task.parentTaskId) {
        parentTaskIds.add(task.parentTaskId);
      }
    });

    // 对每个父任务重新计算状态和进度
    for (const parentTaskId of parentTaskIds) {
      await updateParentTaskProgress(parentTaskId);
      await updateParentTaskStatus(parentTaskId);
    }
  };

  // 强制将所有待办状态的任务进度设置为 0
  const enforceTodoProgressZero = () => {
    tasks.value = tasks.value.map(task => {
      if (task && task.status === 'todo' && task.progress !== 0) {
        return { ...task, progress: 0 };
      }
      return task;
    });
  };

  // 强制将所有已完成状态的任务进度设置为 100
  const enforceDoneProgressHundred = () => {
    tasks.value = tasks.value.map(task => {
      if (task && task.status === 'done' && task.progress !== 100) {
        return { ...task, progress: 100 };
      }
      return task;
    });
  };

  // 更新父任务的时间范围（基于子任务自动计算）
  const updateParentTaskDates = async (parentTaskId: string) => {
    const subtasks = getSubtasks(parentTaskId);

    if (subtasks.length === 0) {
      return;
    }

    // 找出最早的开始时间和最晚的结束时间
    const startDates = subtasks.map(t => new Date(t.startDate).getTime());
    const endDates = subtasks.map(t => new Date(t.endDate).getTime());

    const earliestStart = new Date(Math.min(...startDates));
    const latestEnd = new Date(Math.max(...endDates));

    const startDate = earliestStart.toISOString().split('T')[0];
    const endDate = latestEnd.toISOString().split('T')[0];

    // 查找父任务
    const parentIndex = tasks.value.findIndex(t => t && t.id === parentTaskId);
    if (parentIndex === -1) {
      return;
    }

    const parentTask = tasks.value[parentIndex];
    if (!parentTask) {
      return;
    }

    // 检查是否需要更新
    if (parentTask.startDate === startDate && parentTask.endDate === endDate) {
      return;
    }

    try {
      // 更新父任务的时间
      const updatedTask = { ...parentTask, startDate, endDate };
      tasks.value[parentIndex] = updatedTask;

      if (currentTask.value?.id === parentTaskId) {
        currentTask.value = updatedTask;
      }

      // 调用 API 保存
      await apiService.updateTask(parentTaskId, {
        startDate,
        endDate
      });

      // 递归更新上级父任务
      if (parentTask.parentTaskId) {
        await updateParentTaskDates(parentTask.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update parent task dates:', error);
    }
  };

  // 更新父任务的工时（基于子任务工时之和）
  const updateParentTaskHours = async (parentTaskId: string) => {
    const subtasks = getSubtasks(parentTaskId);

    if (subtasks.length === 0) {
      return;
    }

    // 计算直接子任务的预估工时和实际工时之和
    const totalEstimatedHours = subtasks.reduce((sum, task) => sum + (task.estimatedHours || 0), 0);
    const totalActualHours = subtasks.reduce((sum, task) => sum + (task.actualHours || 0), 0);

    // 查找父任务
    const parentIndex = tasks.value.findIndex(t => t && t.id === parentTaskId);
    if (parentIndex === -1) {
      return;
    }

    const parentTask = tasks.value[parentIndex];
    if (!parentTask) {
      return;
    }

    // 检查是否需要更新
    if (parentTask.estimatedHours === totalEstimatedHours && parentTask.actualHours === totalActualHours) {
      return;
    }

    try {
      // 更新父任务的工时
      const updatedTask = { ...parentTask, estimatedHours: totalEstimatedHours, actualHours: totalActualHours };
      tasks.value[parentIndex] = updatedTask;

      if (currentTask.value?.id === parentTaskId) {
        currentTask.value = updatedTask;
      }

      // 调用 API 保存
      await apiService.updateTask(parentTaskId, {
        estimatedHours: totalEstimatedHours,
        actualHours: totalActualHours
      });

      // 递归更新上级父任务
      if (parentTask.parentTaskId) {
        await updateParentTaskHours(parentTask.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update parent task hours:', error);
    }
  };

  // 更新所有父任务的时间范围
  const updateAllParentTaskDates = async () => {
    // 获取所有有子任务的任务（即父任务）
    const parentTaskIds = new Set<string>();
    tasks.value.forEach(task => {
      if (task && task.parentTaskId) {
        parentTaskIds.add(task.parentTaskId);
      }
    });

    // 对每个父任务更新时间范围
    for (const parentTaskId of parentTaskIds) {
      await updateParentTaskDates(parentTaskId);
    }
  };

  // Getters
  const tasksByProject = (projectId: string) => {
    return tasks.value.filter(t => t && t.projectId === projectId);
  };

  const tasksByStatus = (status: string) => {
    return tasks.value.filter(t => t && t.status === status);
  };

  const tasksByAssignee = (assigneeId: string) => {
    return tasks.value.filter(t => t && t.assigneeId === assigneeId);
  };

  const getTaskById = (id: string) => {
    return tasks.value.find(t => t && t.id === id);
  };

  const getSubtasks = (parentTaskId: string) => {
    return tasks.value.filter(t => t && t.parentTaskId === parentTaskId);
  };

  // 获取任务深度
  const getTaskDepth = (taskId: string, depth = 0): number => {
    const task = getTaskById(taskId);
    if (!task || !task.parentTaskId) return depth;
    return getTaskDepth(task.parentTaskId, depth + 1);
  };

  // 获取所有子孙任务（递归）
  const getAllDescendants = (taskId: string): Task[] => {
    const directSubtasks = getSubtasks(taskId);
    let allDescendants = [...directSubtasks];

    directSubtasks.forEach(subtask => {
      const subDescendants = getAllDescendants(subtask.id);
      allDescendants = [...allDescendants, ...subDescendants];
    });

    return allDescendants;
  };

  // 计算聚合进度（基于所有子孙任务）
  const computeAggregatedProgress = (taskId: string): number => {
    const descendants = getAllDescendants(taskId);
    if (descendants.length === 0) {
      const task = getTaskById(taskId);
      return task?.progress || 0;
    }

    const totalProgress = descendants.reduce((sum, t) => sum + t.progress, 0);
    return Math.round(totalProgress / descendants.length);
  };

  // 获取子任务摘要
  const getSubtaskSummary = (taskId: string): SubtaskSummary => {
    const directSubtasks = getSubtasks(taskId);
    const allDescendants = getAllDescendants(taskId);

    const completed = allDescendants.filter(t => t.status === 'done').length;
    const inProgress = allDescendants.filter(t => t.status === 'in-progress').length;
    const todo = allDescendants.filter(t => t.status === 'todo').length;

    return {
      total: directSubtasks.length,
      completed,
      inProgress,
      todo,
      aggregatedProgress: computeAggregatedProgress(taskId),
      totalDescendants: allDescendants.length
    };
  };

  // Actions
  const createTask = async (data: Partial<Task>) => {
    try {
      const newTask = await apiService.createTask(data);
      if (newTask) {
        tasks.value.unshift(newTask);

        // 如果新任务有父任务，更新父任务的进度、状态、日期和工时
        if (newTask.parentTaskId) {
          await updateParentTaskProgress(newTask.parentTaskId);
          await updateParentTaskStatus(newTask.parentTaskId);
          await updateParentTaskDates(newTask.parentTaskId);

          // 如果新任务有工时，需要更新父任务的工时
          if (newTask.estimatedHours || newTask.actualHours) {
            await updateParentTaskHours(newTask.parentTaskId);
          }
        }
      } else {
        console.warn('Created task is null, not adding to list');
      }
      return newTask;
    } catch (error) {
      console.error('Failed to create task:', error);
      throw error;
    }
  };

  const updateTask = async (id: string, data: Partial<Task>) => {
    try {
      // 先获取旧任务数据，用于后续判断是否需要更新父任务
      const oldTask = getTaskById(id);
      const oldParentTaskId = oldTask?.parentTaskId;
      const oldEstimatedHours = oldTask?.estimatedHours;
      const oldActualHours = oldTask?.actualHours;

      const updatedTask = await apiService.updateTask(id, data);
      if (updatedTask) {
        const index = tasks.value.findIndex(t => t && t.id === id);
        if (index !== -1) {
          tasks.value[index] = updatedTask;
        }
        if (currentTask.value?.id === id) {
          currentTask.value = updatedTask;
        }

        // 如果更新的任务有父任务，需要更新父任务的进度、状态、日期和工时
        const parentTaskId = updatedTask.parentTaskId || oldParentTaskId;
        if (parentTaskId) {
          // 无论是状态、进度还是其他字段变化，都需要更新父任务
          await updateParentTaskProgress(parentTaskId);
          await updateParentTaskStatus(parentTaskId);
          await updateParentTaskDates(parentTaskId);

          // 如果工时发生了变化，需要更新父任务的工时
          if (data.estimatedHours !== undefined || data.actualHours !== undefined) {
            await updateParentTaskHours(parentTaskId);
          }
        }
      }
      return updatedTask;
    } catch (error) {
      console.error('Failed to update task:', error);
      throw error;
    }
  };

  const deleteTask = async (id: string) => {
    try {
      console.log('===== 开始删除任务 =====');
      console.log('删除任务 ID:', id);
      console.log('删除前的任务列表:', tasks.value.map(t => t ? `${t.id}: ${t.title}` : 'null'));

      // 先获取要删除的任务，以便稍后更新父任务进度
      const index = tasks.value.findIndex(t => t && t.id === id);
      const taskToDelete = index !== -1 ? tasks.value[index] : null;
      const parentTaskId = taskToDelete?.parentTaskId;

      await apiService.deleteTask(id);

      console.log('API 删除成功');

      if (index !== -1) {
        const deletedTask = tasks.value[index];
        tasks.value.splice(index, 1);
        console.log('从列表中移除任务:', deletedTask);
      } else {
        console.warn('任务在列表中未找到');
      }

      if (currentTask.value?.id === id) {
        currentTask.value = null;
      }

      console.log('删除后的任务列表:', tasks.value.map(t => t ? `${t.id}: ${t.title}` : 'null'));
      console.log('===== 删除任务完成 =====');

      // 如果删除的任务有父任务，更新父任务的进度、状态、日期和工时
      if (parentTaskId) {
        await updateParentTaskProgress(parentTaskId);
        await updateParentTaskStatus(parentTaskId);
        await updateParentTaskDates(parentTaskId);

        // 如果删除的任务有工时，需要更新父任务的工时
        if (taskToDelete?.estimatedHours || taskToDelete?.actualHours) {
          await updateParentTaskHours(parentTaskId);
        }
      }
    } catch (error) {
      console.error('Failed to delete task:', error);
      throw error;
    }
  };

  const updateTaskStatus = async (id: string, status: Task['status']) => {
    // 先查找任务
    const index = tasks.value.findIndex(t => t && t.id === id);

    if (index === -1) {
      console.warn(`Task with id ${id} not found`);
      return;
    }

    const task = tasks.value[index];
    if (!task) {
      console.warn(`Task at index ${index} is null`);
      return;
    }

    // 如果状态没有变化，直接返回
    if (task.status === status) {
      return;
    }

    try {
      // 乐观更新：先更新本地状态（立即响应）
      // 如果状态变为 todo，强制将进度设置为 0
      // 如果状态变为 done，强制将进度设置为 100
      const updatedData: Partial<Task> = { status };
      if (status === 'todo') {
        updatedData.progress = 0;
      } else if (status === 'done') {
        updatedData.progress = 100;
      }

      tasks.value[index] = { ...task, ...updatedData };

      // 如果是当前任务，也更新它
      if (currentTask.value?.id === id) {
        currentTask.value = { ...currentTask.value, ...updatedData };
      }

      // 然后调用 API（在后台执行）
      const updatedTask = await apiService.updateTaskStatus(id, status);

      // 如果状态变为 todo，还需要调用 API 更新进度为 0
      if (status === 'todo') {
        await apiService.updateTaskProgress(id, 0);
      }

      // 如果状态变为 done，还需要调用 API 更新进度为 100
      if (status === 'done') {
        await apiService.updateTaskProgress(id, 100);
      }

      // API 成功后，用服务器返回的数据再次更新
      let finalTask = updatedTask;
      if (updatedTask) {
        if (updatedTask.status === 'todo') {
          finalTask = { ...updatedTask, progress: 0 };
        } else if (updatedTask.status === 'done') {
          finalTask = { ...updatedTask, progress: 100 };
        }
      }

      const finalIndex = tasks.value.findIndex(t => t && t.id === id);
      if (finalIndex !== -1 && finalTask) {
        tasks.value[finalIndex] = finalTask;
      }
      if (currentTask.value?.id === id && finalTask) {
        currentTask.value = finalTask;
      }

      // 如果状态变化且有父任务，需要更新父任务的进度和状态
      if (task.parentTaskId) {
        await updateParentTaskProgress(task.parentTaskId);
        await updateParentTaskStatus(task.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update task status:', error);
      // API 调用失败时，本地状态已经被乐观更新了
      // 如果需要，可以在这里添加回滚逻辑
    }
  };

  const updateTaskProgress = async (id: string, progress: number) => {
    // 先查找任务
    const index = tasks.value.findIndex(t => t && t.id === id);

    if (index === -1) {
      console.warn(`Task with id ${id} not found`);
      return;
    }

    const task = tasks.value[index];
    if (!task) {
      console.warn(`Task at index ${index} is null`);
      return;
    }

    // 如果进度没有变化，直接返回
    if (task.progress === progress) {
      return;
    }

    try {
      // 乐观更新：先更新本地状态（立即响应）
      tasks.value[index] = { ...task, progress };

      // 如果是当前任务，也更新它
      if (currentTask.value?.id === id) {
        currentTask.value = { ...currentTask.value, progress };
      }

      // 然后调用 API（在后台执行）
      const updatedTask = await apiService.updateTaskProgress(id, progress);

      // API 成功后，用服务器返回的数据再次更新
      const finalIndex = tasks.value.findIndex(t => t && t.id === id);
      if (finalIndex !== -1 && updatedTask) {
        tasks.value[finalIndex] = updatedTask;
      }
      if (currentTask.value?.id === id && updatedTask) {
        currentTask.value = updatedTask;
      }

      // 如果此任务有父任务，更新父任务的进度和状态
      if (task.parentTaskId) {
        await updateParentTaskProgress(task.parentTaskId);
        await updateParentTaskStatus(task.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update task progress:', error);
      // API 调用失败时，本地状态已经被乐观更新了
    }
  };

  // 更新父任务的进度（基于子任务自动计算）
  const updateParentTaskProgress = async (parentTaskId: string) => {
    const subtasks = getSubtasks(parentTaskId);

    if (subtasks.length === 0) {
      return;
    }

    // 使用子任务的实际进度值来计算平均值（而不是基于状态估算）
    const totalProgress = subtasks.reduce((sum, t) => sum + (t.progress || 0), 0);
    const progress = Math.round(totalProgress / subtasks.length);

    // 查找父任务
    const parentIndex = tasks.value.findIndex(t => t && t.id === parentTaskId);
    if (parentIndex === -1) {
      return;
    }

    const parentTask = tasks.value[parentIndex];
    if (!parentTask || parentTask.progress === progress) {
      return;
    }

    try {
      // 更新父任务进度
      tasks.value[parentIndex] = { ...parentTask, progress };

      if (currentTask.value?.id === parentTaskId) {
        currentTask.value = { ...currentTask.value, progress };
      }

      // 调用 API 保存
      await apiService.updateTaskProgress(parentTaskId, progress);

      // 递归更新上级父任务
      if (parentTask.parentTaskId) {
        await updateParentTaskProgress(parentTask.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update parent task progress:', error);
    }
  };

  // 根据子任务状态计算父任务状态
  const computeParentTaskStatus = (parentTaskId: string): Task['status'] => {
    const subtasks = getSubtasks(parentTaskId);

    if (subtasks.length === 0) {
      return 'todo';
    }

    // 检查所有子任务的状态
    const allSubtaskStatuses = subtasks.map(t => t.status);

    // 如果所有子任务都是 done，父任务为 done
    if (allSubtaskStatuses.every(status => status === 'done')) {
      return 'done';
    }

    // 如果任意子任务是 in-progress，父任务为 in-progress
    if (allSubtaskStatuses.some(status => status === 'in-progress')) {
      return 'in-progress';
    }

    // 如果同时存在 todo 和 done 状态，父任务为 in-progress
    const hasTodo = allSubtaskStatuses.some(status => status === 'todo');
    const hasDone = allSubtaskStatuses.some(status => status === 'done');
    if (hasTodo && hasDone) {
      return 'in-progress';
    }

    // 否则（所有子任务都是 todo），父任务为 todo
    return 'todo';
  };

  // 更新父任务的状态（基于子任务自动计算）
  const updateParentTaskStatus = async (parentTaskId: string) => {
    const subtasks = getSubtasks(parentTaskId);

    if (subtasks.length === 0) {
      return;
    }

    // 计算父任务应该的状态
    const newStatus = computeParentTaskStatus(parentTaskId);

    // 查找父任务
    const parentIndex = tasks.value.findIndex(t => t && t.id === parentTaskId);
    if (parentIndex === -1) {
      return;
    }

    const parentTask = tasks.value[parentIndex];
    if (!parentTask || parentTask.status === newStatus) {
      return;
    }

    try {
      // 更新父任务状态
      tasks.value[parentIndex] = { ...parentTask, status: newStatus };

      if (currentTask.value?.id === parentTaskId) {
        currentTask.value = { ...currentTask.value, status: newStatus };
      }

      // 调用 API 保存
      await apiService.updateTaskStatus(parentTaskId, newStatus);

      // 递归更新上级父任务
      if (parentTask.parentTaskId) {
        await updateParentTaskStatus(parentTask.parentTaskId);
      }
    } catch (error) {
      console.error('Failed to update parent task status:', error);
    }
  };

  const setCurrentTask = (task: Task | null) => {
    currentTask.value = task;
  };

  return {
    tasks,
    currentTask,
    loading,
    loaded,
    loadTasks,
    tasksByProject,
    tasksByStatus,
    tasksByAssignee,
    getTaskById,
    getSubtasks,
    getTaskDepth,
    getAllDescendants,
    computeAggregatedProgress,
    getSubtaskSummary,
    createTask,
    updateTask,
    deleteTask,
    updateTaskStatus,
    updateTaskProgress,
    updateParentTaskProgress,
    updateParentTaskStatus,
    updateParentTaskDates,
    updateParentTaskHours,
    computeParentTaskStatus,
    setCurrentTask
  };
});
