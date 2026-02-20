export interface User {
  id: string;
  name: string;
  email: string;
  avatar: string;
  role: 'admin' | 'project-manager' | 'member' | 'viewer';
  department: string;
  skills: string[];
  password?: string;
  joinedAt: string;
}

export interface Comment {
  id: string;
  taskId: string;
  userId: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface Attachment {
  id: string;
  taskId: string;
  name: string;
  url: string;
  size: number;
  type: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface SubtaskSummary {
  total: number;           // 总子任务数
  completed: number;       // 已完成
  inProgress: number;      // 进行中
  todo: number;           // 待办
  aggregatedProgress: number; // 聚合进度
  totalDescendants: number;   // 所有子孙任务数（包括子任务的子任务）
}

export interface Task {
  id: string;
  projectId: string;
  parentTaskId?: string;
  title: string;
  description: string;
  status: 'todo' | 'in-progress' | 'done';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  assigneeId?: string;
  startDate: string;
  endDate: string;
  estimatedHours?: number;
  actualHours?: number;
  progress: number;
  dependencies?: string[];  // 可选
  tags?: string[];  // 可选
  attachments?: Attachment[];  // 可选
  comments?: Comment[];  // 可选
  createdAt: string;
  updatedAt: string;
  // 新增字段：层级显示支持
  depth?: number;          // 任务深度（0=顶级任务）
  subtaskSummary?: SubtaskSummary;
  isExpanded?: boolean;
  // 延期相关字段
  originalEndDate?: string;      // 原始结束日期
  delayedDays?: number;          // 累计延期天数
  delayReason?: string;          // 延期原因
  delayCount?: number;           // 延期次数
  lastDelayDate?: string;        // 最后延期日期
  isDelayed?: boolean;           // 是否已延期
}

export interface Project {
  id: string;
  name: string;
  description: string;
  status: 'planning' | 'active' | 'completed' | 'on-hold' | 'cancelled';
  priority: 'low' | 'medium' | 'high' | 'critical';
  startDate: string;
  endDate: string;
  progress: number;
  ownerId: string;
  memberIds?: string[];  // 可选，后端可能不返回
  estimatedHours?: number;  // 预估工时（可选）
  createdAt: string;
  updatedAt: string;
  color?: string;  // 可选，后端可能不返回
  tags?: string[];  // 可选，后端可能不返回
  // 延期相关字段
  delayedTasks?: number;          // 延期任务数
  totalDelayedDays?: number;      // 总延期天数
  isDelayed?: boolean;            // 是否有延期任务
}

export interface Statistics {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  totalMembers: number;
}

export interface DelayStats {
  totalTasks: number;
  delayedTasks: number;
  delayRate: number;
  totalDelayedDays: number;
  criticalDelayedTasks: number;
}
