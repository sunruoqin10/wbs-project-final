// API Service Layer - Connects to Spring Boot backend

import type { Project, Task, User, DelayStats } from '@/types';
import { useUserStore } from '@/stores/user';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Backend response wrapper
interface Result<T> {
  code: number;
  message: string;
  data: T;
}

// Helper function for API calls
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;

  // 获取token（排除登录接口）
  const userStore = useUserStore();
  const isLoginEndpoint = endpoint === '/users/login';

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>,
  };

  // 如果有token且不是登录接口，添加Authorization头
  if (userStore.token && !isLoginEndpoint) {
    headers['Authorization'] = `Bearer ${userStore.token}`;
  }

  const config: RequestInit = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      // 尝试读取错误响应体
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
      } catch (e) {
        // 如果无法解析 JSON，使用默认错误消息
      }
      throw new Error(errorMessage);
    }

    const result: Result<T> = await response.json();

    if (result.code !== 200) {
      throw new Error(result.message || 'API request failed');
    }

    return result.data;
  } catch (error) {
    console.error(`API request failed: ${endpoint}`, error);
    throw error;
  }
}

class ApiService {
  // Projects API
  async getProjects(): Promise<Project[]> {
    return request<Project[]>('/projects');
  }

  async getProject(id: string | number): Promise<Project> {
    return request<Project>(`/projects/${id}`);
  }

  async createProject(data: Partial<Project>): Promise<Project> {
    return request<Project>('/projects', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateProject(id: string | number, data: Partial<Project>): Promise<Project> {
    return request<Project>(`/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteProject(id: string | number): Promise<void> {
    return request<void>(`/projects/${id}`, {
      method: 'DELETE',
    });
  }

  async getProjectsByStatus(status: string): Promise<Project[]> {
    return request<Project[]>(`/projects/status/${status}`);
  }

  async getProjectsByOwner(ownerId: string | number): Promise<Project[]> {
    return request<Project[]>(`/projects/owner/${ownerId}`);
  }

  async getProjectsByMember(userId: string | number): Promise<Project[]> {
    return request<Project[]>(`/projects/member/${userId}`);
  }

  async getProjectStats(): Promise<any> {
    return request<any>('/projects/stats');
  }

  // Tasks API
  async getTasks(projectId?: string | number): Promise<Task[]> {
    if (projectId) {
      return request<Task[]>(`/tasks/project/${projectId}`);
    }
    return request<Task[]>('/tasks');
  }

  async getTask(id: string | number): Promise<Task> {
    return request<Task>(`/tasks/${id}`);
  }

  async createTask(data: Partial<Task>): Promise<Task> {
    // 打印原始数据，用于调试
    console.log('===== Raw task data received =====');
    console.log('Raw data:', data);
    console.log('Raw data JSON:', JSON.stringify(data, null, 2));
    console.log('Raw data types:', Object.entries(data).map(([k, v]) => `${k}: ${typeof v}`));

    // 明确指定后端支持的字段，避免发送前端扩展字段（tags, dependencies等）
    const supportedFields: (keyof Task)[] = [
      'projectId', 'parentTaskId', 'title', 'description',
      'status', 'priority', 'assigneeId', 'startDate', 'endDate',
      'estimatedHours', 'actualHours', 'progress'
    ];

    // 必填字段列表（不能为空）
    const requiredFields = ['projectId', 'title', 'status', 'priority'];

    // 特殊处理数值字段（允许 0 值）
    const numericFields = ['estimatedHours', 'actualHours', 'progress'];

    const cleanData: any = {};
    console.log('===== Processing fields =====');
    supportedFields.forEach(field => {
      const value = data[field];
      const valueType = typeof value;

      // 对于必填字段，如果值为空字符串，也包含进去（后端会处理）
      const isRequired = requiredFields.includes(field as string);
      const isNumeric = numericFields.includes(field);

      // 只包含有值的字段（跳过 null、undefined、空字符串、对象、数组）
      // 特殊处理数值字段：允许 0 值
      if (value !== null &&
          value !== undefined &&
          (isRequired || value !== '' || isNumeric) &&  // 必填字段或数值字段可以包含空字符串/0
          valueType !== 'object' &&  // 跳过对象和数组
          !Array.isArray(value)) {
        cleanData[field] = value;
        console.log(`✓ Field ${field}: ${valueType} =`, value);
      } else {
        console.log(`✗ Skipped ${field}: ${valueType}`, value);
      }
    });

    // 确保 projectId 存在（必填字段）
    if (!cleanData.projectId) {
      throw new Error('projectId is required for creating a task');
    }

    // 确保 title 存在（必填字段）
    if (!cleanData.title) {
      throw new Error('title is required for creating a task');
    }

    // 确保 status 存在（必填字段），如果不存在则设置默认值
    if (!cleanData.status) {
      cleanData.status = 'todo';
      console.log('⚠ Status is missing, setting default value: todo');
    }

    // 确保 priority 存在（必填字段），如果不存在则设置默认值
    if (!cleanData.priority) {
      cleanData.priority = 'medium';
      console.log('⚠ Priority is missing, setting default value: medium');
    }

    console.log('===== Final cleaned data =====');
    console.log('Cleaned data:', cleanData);
    console.log('Cleaned data JSON:', JSON.stringify(cleanData, null, 2));

    return request<Task>('/tasks', {
      method: 'POST',
      body: JSON.stringify(cleanData),
    });
  }

  async updateTask(id: string | number, data: Partial<Task>): Promise<Task> {
    // 明确指定后端支持的字段
    const supportedFields: (keyof Task)[] = [
      'projectId', 'parentTaskId', 'title', 'description',
      'status', 'priority', 'assigneeId', 'startDate', 'endDate',
      'estimatedHours', 'actualHours', 'progress'
    ];

    // 特殊处理数值字段（允许 0 值）
    const numericFields = ['estimatedHours', 'actualHours', 'progress'];

    const cleanData: any = {};

    supportedFields.forEach(field => {
      const value = data[field];

      // 只包含有值的字段（特殊处理数值字段，允许 0）
      if (value !== null &&
          value !== undefined &&
          (numericFields.includes(field) || value !== '')) {
        cleanData[field] = value;
      }
    });

    console.log('Update task data:', cleanData);

    return request<Task>(`/tasks/${id}`, {
      method: 'PUT',
      body: JSON.stringify(cleanData),
    });
  }

  async updateTaskStatus(id: string | number, status: Task['status']): Promise<Task> {
    return request<Task>(`/tasks/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  }

  async updateTaskProgress(id: string | number, progress: number): Promise<Task> {
    return request<Task>(`/tasks/${id}/progress`, {
      method: 'PATCH',
      body: JSON.stringify({ progress }),
    });
  }

  async deleteTask(id: string | number): Promise<void> {
    console.log(`API: 准备删除任务 ID = ${id}`);
    console.log(`API: 删除请求 URL = /tasks/${id}`);

    const result = request<void>(`/tasks/${id}`, {
      method: 'DELETE',
    });

    console.log(`API: 删除请求已发送`);
    return result;
  }

  async getTasksByStatus(status: string): Promise<Task[]> {
    return request<Task[]>(`/tasks/status/${status}`);
  }

  async getTasksByAssignee(assigneeId: string | number): Promise<Task[]> {
    return request<Task[]>(`/tasks/assignee/${assigneeId}`);
  }

  async getSubTasks(parentTaskId: string | number): Promise<Task[]> {
    return request<Task[]>(`/tasks/parent/${parentTaskId}`);
  }

  async getTaskStats(): Promise<any> {
    return request<any>('/tasks/stats');
  }

  // Delay management API
  async getDelayedTasks(projectId?: string | number, includeCompleted = false): Promise<Task[]> {
    const params = new URLSearchParams();
    if (projectId) params.append('projectId', String(projectId));
    params.append('includeCompleted', String(includeCompleted));

    return request<Task[]>(`/tasks/delayed?${params.toString()}`);
  }

  async getProjectDelayStats(projectId: string | number): Promise<DelayStats> {
    return request<DelayStats>(`/tasks/project/${projectId}/delay-stats`);
  }

  async recordTaskDelay(taskId: string | number, newEndDate: string, delayReason: string): Promise<Task> {
    return request<Task>(`/tasks/${taskId}/delay`, {
      method: 'POST',
      body: JSON.stringify({ newEndDate, delayReason }),
    });
  }

  // Users API
  async login(userId: string, password: string): Promise<{ token: string; user: User }> {
    return request<{ token: string; user: User }>('/users/login', {
      method: 'POST',
      body: JSON.stringify({ userId, password }),
    });
  }

  async getUsers(): Promise<User[]> {
    return request<User[]>('/users');
  }

  async getUser(id: string | number): Promise<User> {
    return request<User>(`/users/${id}`);
  }

  async createUser(data: Partial<User>): Promise<User> {
    // 处理 skills 字段：如果是数组，转换为 JSON 字符串
    const processedData = { ...data };
    if (processedData.skills && Array.isArray(processedData.skills)) {
      processedData.skills = JSON.stringify(processedData.skills) as any;
    }

    return request<User>('/users', {
      method: 'POST',
      body: JSON.stringify(processedData),
    });
  }

  async updateUser(id: string | number, data: Partial<User>): Promise<User> {
    // 处理 skills 字段：如果是数组，转换为 JSON 字符串
    const processedData = { ...data };
    if (processedData.skills && Array.isArray(processedData.skills)) {
      processedData.skills = JSON.stringify(processedData.skills) as any;
    }

    return request<User>(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(processedData),
    });
  }

  async deleteUser(id: string | number): Promise<void> {
    return request<void>(`/users/${id}`, {
      method: 'DELETE',
    });
  }

  async getBatchUsers(ids: (string | number)[]): Promise<User[]> {
    return request<User[]>('/users/batch', {
      method: 'POST',
      body: JSON.stringify(ids),
    });
  }

  async getUserCount(): Promise<number> {
    return request<number>('/users/count');
  }

  // Statistics API
  async getStatistics() {
    const [projectStats, taskStats, userCount] = await Promise.all([
      this.getProjectStats(),
      this.getTaskStats(),
      this.getUserCount(),
    ]);

    return {
      ...projectStats,
      ...taskStats,
      totalMembers: userCount,
    };
  }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;
