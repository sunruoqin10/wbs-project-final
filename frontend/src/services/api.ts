// API Service Layer - Connects to Spring Boot backend

import type { Project, Task, User, DelayStats, OvertimeRecord, OvertimeStats, OvertimeApprovalLog, Permission, TaskOvertimeStats, WeeklyReport, WeeklyReportComment, WeeklyReportApprovalLog, Document, OrgNode, RoleChangeLog, RoleChangeRequest } from '@/types';
import type { SchedulerConfig } from '@/types/scheduler';
import { useUserStore } from '@/stores/user';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Backend response wrapper
interface Result<T> {
  code: number;
  message: string;
  data: T;
}

// API 错误：携带 HTTP 状态码和后端 data，便于上层做精细化错误映射
export class ApiError extends Error {
  status: number;
  data: unknown;

  constructor(message: string, status: number, data: unknown = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

// request 的扩展选项：silent=true 时不打 console.error，由调用方自己处理
// (典型场景:已知可能 403 的写操作,在 store 层 catch 后做精细化提示)
interface RequestOptions extends RequestInit {
  silent?: boolean;
}

// Helper function for API calls
async function request<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { silent, ...fetchOptions } = options;
  const url = `${API_BASE_URL}${endpoint}`;

  // 获取token和用户信息（排除登录接口）
  const userStore = useUserStore();
  const isLoginEndpoint = endpoint === '/users/login';

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers as Record<string, string>,
  };

  // 如果有token且不是登录接口，添加Authorization头
  if (userStore.token && !isLoginEndpoint) {
    headers['Authorization'] = `Bearer ${userStore.token}`;
  }

  // 添加用户ID和角色请求头（用于权限验证）
  if (!isLoginEndpoint) {
    if (userStore.currentUserId) {
      headers['X-User-Id'] = userStore.currentUserId;
    }
    if (userStore.currentUser?.role) {
      headers['X-User-Role'] = userStore.currentUser.role;
    }
  }

  const config: RequestInit = {
    ...fetchOptions,
    headers,
  };

  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      let errorData: unknown = null;
      try {
        const text = await response.text();
        try {
          const parsed = JSON.parse(text);
          errorData = parsed;
          errorMessage = parsed?.message || parsed?.error || errorMessage;
        } catch (_) {
          console.warn('API error response (non-JSON):', text.substring(0, 200));
        }
      } catch (_) {}
      throw new ApiError(errorMessage, response.status, errorData);
    }

    const result: Result<T> = await response.json();

    if (result.code !== 200) {
      throw new ApiError(result.message || 'API request failed', response.status, result.data);
    }

    return result.data;
  } catch (error) {
    if (!silent) {
      console.error(`API request failed: ${endpoint}`, error);
    }
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

  async updateProject(
    id: string | number,
    data: Partial<Project>,
    options: { silent?: boolean } = {}
  ): Promise<Project> {
    return request<Project>(`/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      silent: options.silent,
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

  /**
   * 当前登录用户作为负责人的任务(跨项目聚合,2026-06-12 "我的任务" 页用)
   */
  async getMyTasks(): Promise<Task[]> {
    return request<Task[]>('/tasks/mine');
  }

  async getMyTaskTree(): Promise<Task[]> {
    return request<Task[]>('/tasks/mine/tree');
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
      'estimatedHours', 'actualHours', 'progress',
      'originalEndDate', 'delayedDays', 'delayReason', 'delayCount', 'lastDelayDate'
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
      'estimatedHours', 'actualHours', 'progress',
      'originalEndDate', 'delayedDays', 'delayReason', 'delayCount', 'lastDelayDate'
    ];

    // 特殊处理数值字段（允许 0 值）
    const numericFields = ['estimatedHours', 'actualHours', 'progress', 'delayedDays', 'delayCount'];

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

  async searchUsers(keyword: string, page: number = 1, pageSize: number = 20): Promise<{ records: User[]; total: number }> {
    const params = new URLSearchParams();
    if (keyword) params.set('keyword', keyword);
    params.set('page', String(page));
    params.set('pageSize', String(pageSize));
    return request<{ records: User[]; total: number }>(`/users?${params.toString()}`);
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

  async changePassword(id: string, currentPassword: string, newPassword: string): Promise<void> {
    return request<void>(`/users/${id}/password`, {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
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

  // ============ 角色管理 v2 ============

  /**
   * 角色变更
   * - admin 可改任何用户
   * - dept-pm 可改本部门内非 admin 用户(2026-06-12 放开)
   * 触发 tokenVersion + 1,目标用户旧 token 失效
   */
  async changeUserRole(id: string, req: RoleChangeRequest): Promise<User> {
    return request<User>(`/users/${id}/role`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
  }

  /**
   * 仅更新 PM 的 managed_project_ids(2026-06-12 新增)
   * 供 dept-pm 单独管理 PM 的项目列表
   * 触发 tokenVersion + 1
   */
  async updateManagedProjects(id: string, managedProjectIds: string[]): Promise<User> {
    return request<User>(`/users/${id}/managed-projects`, {
      method: 'PUT',
      body: JSON.stringify({ managedProjectIds }),
    });
  }

  /**
   * 查询某用户角色变更历史
   */
  async getRoleChangeHistory(id: string): Promise<RoleChangeLog[]> {
    return request<RoleChangeLog[]>(`/users/${id}/role-history`);
  }

  // HR Sync API
  async syncHrUsers(): Promise<{ inserted: number; updated: number; resigned: number; inferred: number }> {
    return request<{ inserted: number; updated: number; resigned: number; inferred: number }>('/users/sync-hr', {
      method: 'POST',
    });
  }

  // Org tree API
  async getOrgTree(): Promise<OrgNode> {
    return request<OrgNode>('/orgs/tree');
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

  // Overtime API
  async getOvertimeRecords(params?: {
    userId?: string;
    projectId?: string;
    status?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<OvertimeRecord[]> {
    const searchParams = new URLSearchParams();
    if (params?.userId) searchParams.append('userId', params.userId);
    if (params?.projectId) searchParams.append('projectId', params.projectId);
    if (params?.status) searchParams.append('status', params.status);
    if (params?.startDate) searchParams.append('startDate', params.startDate);
    if (params?.endDate) searchParams.append('endDate', params.endDate);

    const queryString = searchParams.toString();
    return request<OvertimeRecord[]>(`/overtime${queryString ? `?${queryString}` : ''}`);
  }

  async getOvertimeRecord(id: string | number): Promise<OvertimeRecord> {
    return request<OvertimeRecord>(`/overtime/${id}`);
  }

  async createOvertimeRecord(data: Partial<OvertimeRecord>): Promise<OvertimeRecord> {
    return request<OvertimeRecord>('/overtime', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateOvertimeRecord(id: string | number, data: Partial<OvertimeRecord>): Promise<OvertimeRecord> {
    return request<OvertimeRecord>(`/overtime/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteOvertimeRecord(id: string | number): Promise<void> {
    return request<void>(`/overtime/${id}`, {
      method: 'DELETE',
    });
  }

  async approveOvertimeRecord(id: string | number, approverId: string): Promise<OvertimeRecord> {
    return request<OvertimeRecord>(`/overtime/${id}/approve`, {
      method: 'PUT',
      body: JSON.stringify({ approverId, approved: true }),
    });
  }

  async rejectOvertimeRecord(id: string | number, approverId: string, rejectReason: string): Promise<OvertimeRecord> {
    return request<OvertimeRecord>(`/overtime/${id}/approve`, {
      method: 'PUT',
      body: JSON.stringify({ approverId, approved: false, rejectReason }),
    });
  }

  // 2026-06-14: 获取加班审批历史日志(多角色都可审批,审计追溯)
  async getOvertimeApprovalLogs(id: string | number): Promise<OvertimeApprovalLog[]> {
    return request<OvertimeApprovalLog[]>(`/overtime/${id}/approval-logs`);
  }

  async getOvertimeStats(projectId?: string): Promise<OvertimeStats> {
    const params = projectId ? `?projectId=${projectId}` : '';
    const data = await request<any>(`/overtime/stats${params}`);
    
    // 转换后端 BigDecimal 为前端 number
    const convertBigDecimal = (value: any): number => {
      if (value == null) return 0;
      return Number(value);
    };
    
    // 转换项目统计数据
    const convertByProject = (projects: any[]): any[] => {
      if (!projects) return [];
      return projects.map(p => ({
        ...p,
        hours: convertBigDecimal(p.totalHours || p.hours),
        count: Number(p.recordCount || p.count || 0),
        totalHours: convertBigDecimal(p.totalHours),
        recordCount: Number(p.recordCount || 0)
      }));
    };
    
    return {
      totalRecords: Number(data.totalRecords || 0),
      totalHours: convertBigDecimal(data.totalHours),
      totalPeople: Number(data.totalPeople || 0),
      pendingApprovals: Number(data.pendingApprovals || data.pendingRecords || 0),
      thisMonthHours: convertBigDecimal(data.thisMonthHours),
      thisMonthPeople: Number(data.thisMonthPeople || 0),
      byType: {
        weekday: Number(data.byType?.weekday || 0),
        weekend: Number(data.byType?.weekend || 0),
        holiday: Number(data.byType?.holiday || 0)
      },
      byProject: convertByProject(data.byProject),
      approvedRecords: Number(data.approvedRecords || 0),
      approvedHours: convertBigDecimal(data.approvedHours),
      pendingRecords: Number(data.pendingRecords || 0),
      pendingHours: convertBigDecimal(data.pendingHours),
      rejectedRecords: Number(data.rejectedRecords || 0)
    };
  }

  async getOvertimeRecordsByTaskId(taskId: string | number): Promise<OvertimeRecord[]> {
    return request<OvertimeRecord[]>(`/overtime/task/${taskId}`);
  }

  async getTaskOvertimeHours(taskId: string | number): Promise<number> {
    return request<number>(`/overtime/total-hours/task/${taskId}`);
  }

  async getTaskOvertimeStats(projectId?: string, startDate?: string, endDate?: string): Promise<TaskOvertimeStats[]> {
    const searchParams = new URLSearchParams();
    if (projectId) searchParams.append('projectId', projectId);
    if (startDate) searchParams.append('startDate', startDate);
    if (endDate) searchParams.append('endDate', endDate);

    const queryString = searchParams.toString();
    return request<TaskOvertimeStats[]>(`/overtime/stats/tasks${queryString ? `?${queryString}` : ''}`);
  }

  // Permission APIs
  async getPermissions(): Promise<Permission[]> {
    return request<Permission[]>('/permission');
  }

  async getPermissionsByRole(role: string): Promise<Permission[]> {
    return request<Permission[]>(`/permission/role/${role}`);
  }

  async checkPermission(role: string, permission: string): Promise<{ hasPermission: boolean }> {
    return request<{ hasPermission: boolean }>(`/permission/check?role=${role}&permission=${permission}`);
  }

  async checkProjectPermission(userId: string, projectId: string, permission: string): Promise<{ hasPermission: boolean }> {
    return request<{ hasPermission: boolean }>(`/permission/check-project?userId=${userId}&projectId=${projectId}&permission=${permission}`);
  }

  async checkIsOwner(userId: string, projectId: string): Promise<{ isOwner: boolean }> {
    return request<{ isOwner: boolean }>(`/permission/is-owner?userId=${userId}&projectId=${projectId}`);
  }

  async checkIsMember(userId: string, projectId: string): Promise<{ isMember: boolean }> {
    return request<{ isMember: boolean }>(`/permission/is-member?userId=${userId}&projectId=${projectId}`);
  }

  // Weekly Reports API
  async getWeeklyReports(params?: {
    userId?: string;
    projectId?: string;
    status?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<WeeklyReport[]> {
    const searchParams = new URLSearchParams();
    if (params?.userId) searchParams.append('userId', params.userId);
    if (params?.projectId) searchParams.append('projectId', params.projectId);
    if (params?.status) searchParams.append('status', params.status);
    if (params?.startDate) searchParams.append('startDate', params.startDate);
    if (params?.endDate) searchParams.append('endDate', params.endDate);

    const queryString = searchParams.toString();
    return request<WeeklyReport[]>(`/weekly-reports${queryString ? `?${queryString}` : ''}`);
  }

  async getWeeklyReportById(id: string | number): Promise<WeeklyReport> {
    return request<WeeklyReport>(`/weekly-reports/${id}`);
  }

  async createWeeklyReport(data: Partial<WeeklyReport>): Promise<WeeklyReport> {
    return request<WeeklyReport>('/weekly-reports', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateWeeklyReport(id: string | number, data: Partial<WeeklyReport>): Promise<WeeklyReport> {
    return request<WeeklyReport>(`/weekly-reports/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteWeeklyReport(id: string | number): Promise<void> {
    return request<void>(`/weekly-reports/${id}`, {
      method: 'DELETE',
    });
  }

  async submitWeeklyReport(id: string | number): Promise<WeeklyReport> {
    return request<WeeklyReport>(`/weekly-reports/${id}/submit`, {
      method: 'POST',
      body: JSON.stringify({}),
    });
  }

  async approveWeeklyReport(id: string | number, approved: boolean, approveComment?: string): Promise<WeeklyReport> {
    return request<WeeklyReport>(`/weekly-reports/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ approved, approveComment }),
    });
  }

  async getMyWeeklyReports(userId: string): Promise<WeeklyReport[]> {
    return request<WeeklyReport[]>(`/weekly-reports/my?userId=${userId}`);
  }

  async getProjectWeeklyReports(projectId: string): Promise<WeeklyReport[]> {
    return request<WeeklyReport[]>(`/weekly-reports/project/${projectId}`);
  }

  async getCurrentWeekReport(userId: string): Promise<WeeklyReport> {
    return request<WeeklyReport>(`/weekly-reports/current-week?userId=${userId}`);
  }

  async getWeeklyReportComments(reportId: string): Promise<WeeklyReportComment[]> {
    return request<WeeklyReportComment[]>(`/weekly-reports/${reportId}/comments`);
  }

  async addWeeklyReportComment(reportId: string, data: { content: string }): Promise<WeeklyReportComment> {
    return request<WeeklyReportComment>(`/weekly-reports/${reportId}/comments`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async deleteWeeklyReportComment(commentId: string | number): Promise<void> {
    return request<void>(`/weekly-reports/comments/${commentId}`, {
      method: 'DELETE',
    });
  }

  // 2026-06-14: 获取周报审批历史日志(GET /weekly-reports/{id}/approval-logs)
  async getWeeklyReportApprovalLogs(reportId: string | number): Promise<WeeklyReportApprovalLog[]> {
    return request<WeeklyReportApprovalLog[]>(`/weekly-reports/${reportId}/approval-logs`);
  }

  // Documents API
  async getAllDocuments(): Promise<Document[]> {
    return request<Document[]>('/documents');
  }

  async getDocument(id: string | number): Promise<Document> {
    return request<Document>(`/documents/${id}`);
  }

  async getProjectDocuments(projectId: string | number): Promise<Document[]> {
    return request<Document[]>(`/documents/project/${projectId}`);
  }

  async getTaskDocuments(taskId: string | number): Promise<Document[]> {
    return request<Document[]>(`/documents/task/${taskId}`);
  }

  async getDocumentsByCategory(category: string): Promise<Document[]> {
    return request<Document[]>(`/documents/category/${category}`);
  }

  async getReportDocuments(reportId: string | number): Promise<Document[]> {
    return request<Document[]>(`/documents/report/${reportId}`);
  }

  async uploadDocument(formData: FormData): Promise<Document> {
    console.log('上传文档表单数据:', Object.fromEntries(formData.entries()));
    const url = `${API_BASE_URL}/documents/upload`;

    const userStore = useUserStore();
    const headers: Record<string, string> = {};

    if (userStore.currentUserId) {
      headers['X-User-Id'] = userStore.currentUserId;
    }

    if (userStore.token) {
      headers['Authorization'] = `Bearer ${userStore.token}`;
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        body: formData,
        headers: headers,
      });

      if (!response.ok) {
        let errorMessage = `HTTP error! status: ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch (e) {
        }
        console.error('上传失败:', errorMessage);
        throw new Error(errorMessage);
      }

      const result: Result<Document> = await response.json();
      if (result.code !== 200) {
        throw new Error(result.message || 'API request failed');
      }

      return result.data;
    } catch (error) {
      console.error('上传文档异常:', error);
      throw error;
    }
  }

  async downloadDocument(id: string | number): Promise<Blob> {
    const userStore = useUserStore();
    const url = `${API_BASE_URL}/documents/${id}/download`;
    const headers: Record<string, string> = {};

    if (userStore.currentUserId) {
      headers['X-User-Id'] = userStore.currentUserId;
    }

    if (userStore.token) {
      headers['Authorization'] = `Bearer ${userStore.token}`;
    }

    const response = await fetch(url, {
      headers: headers,
    });

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (e) {
        console.error('Failed to parse error response:', e);
      }
      throw new Error(errorMessage);
    }

    return response.blob();
  }

  async previewDocument(id: string | number): Promise<Blob> {
    const userStore = useUserStore();
    const url = `${API_BASE_URL}/documents/${id}/preview`;
    const headers: Record<string, string> = {};

    if (userStore.currentUserId) {
      headers['X-User-Id'] = userStore.currentUserId;
    }

    if (userStore.token) {
      headers['Authorization'] = `Bearer ${userStore.token}`;
    }

    const response = await fetch(url, {
      headers: headers,
    });

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (e) {
        console.error('Failed to parse error response:', e);
      }
      throw new Error(errorMessage);
    }

    return response.blob();
  }

  async updateDocument(id: string | number, data: Partial<Document>): Promise<Document> {
    return request<Document>(`/documents/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteDocument(id: string | number): Promise<void> {
    return request<void>(`/documents/${id}`, {
      method: 'DELETE',
    });
  }

  async getDocumentCountByProject(projectId: string | number): Promise<number> {
    return request<number>(`/documents/project/${projectId}/count`);
  }

  async getSchedulerConfigs(): Promise<SchedulerConfig[]> {
    return request<SchedulerConfig[]>('/scheduler/configs');
  }

  async getSchedulerConfig(id: string): Promise<SchedulerConfig> {
    return request<SchedulerConfig>(`/scheduler/configs/${id}`);
  }

  async updateSchedulerConfig(id: string, data: Partial<SchedulerConfig>): Promise<SchedulerConfig> {
    return request<SchedulerConfig>(`/scheduler/configs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async startScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/start`, {
      method: 'POST',
    });
  }

  async stopScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/stop`, {
      method: 'POST',
    });
  }

  async triggerScheduler(id: string): Promise<string> {
    return request<string>(`/scheduler/${id}/trigger`, {
      method: 'POST',
    });
  }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;
