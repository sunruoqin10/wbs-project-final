export interface User {
  id: string;
  name: string;
  email: string;
  avatar: string;
  /** 角色管理 v2:5 个枚举 */
  role: 'admin' | 'dept-project-manager' | 'project-manager' | 'member' | 'viewer';
  department: string;
  skills: string[];
  password?: string;
  joinedAt: string;
  // === HR 同步扩展字段（2026-06-10 来自 MDM 中间表，后端在 sync-hr 时回填） ===
  deptCode?: string;
  subOrgCd?: string;
  subOrgNam?: string;
  companyCd?: string;
  // === HR 同步扩展字段(2026-06-16,职级) ===
  jpstnCd?: string;
  jpstnNam?: string;
  chineseNam?: string;
  status?: 'C' | 'H' | 'T'; // C=在职, H=休职, T=离职（后端已过滤 T，前端不展示）
  // === 角色管理 v2 扩展 ===
  managedDeptCodes?: string[];  // 管辖部门编码列表(仅 dept-project-manager 有效)
  managedCompanyCd?: string;    // 管辖公司编码(仅 dept-project-manager 有效)
  managedProjectIds?: string[]; // 管辖项目 ID 列表(2026-06-12 新增,仅 project-manager 有效)
  tokenVersion?: number;        // JWT 版本号(角色变更后旧 token 失效)
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
  actualStartDate?: string;    // 实际开始日期
  actualEndDate?: string;      // 实际结束日期
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
  // 子任务延期累计（运行时计算）
  childrenDelayedCount?: number;      // 子任务中延期的数量
  childrenTotalDelayedDays?: number;  // 子任务累计延期天数
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
  createdBy?: string;     // 创建者(独立于 owner / member)
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
  // === 角色管理 v2 扩展 ===
  deptCode?: string;              // 项目归属部门编码(对应 MDM ORG_CD)
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

export interface OvertimeRecord {
  id: string;
  userId: string;
  projectId: string;
  taskId?: string;
  overtimeDate: string;
  startTime: string;
  endTime: string;
  hours: number;
  overtimeType: 'weekday' | 'weekend' | 'holiday';
  reason: string;
  status: 'pending' | 'approved' | 'rejected';
  approverId?: string;
  approvedAt?: string;
  rejectReason?: string;
  compensationType?: 'pay' | 'timeoff';
  createdAt: string;
  updatedAt: string;
}

/**
 * 加班审批历史日志(2026-06-14 新建)
 * 后端 GET /api/overtime/{id}/approval-logs 返回
 */
export interface OvertimeApprovalLog {
  id: string;
  overtimeId: string;
  approverId: string;
  /** 后端 join sys_user.name 返回 */
  approverName?: string;
  approverRole?: 'admin' | 'project-manager' | 'dept-project-manager' | 'project-owner';
  action: 'approve' | 'reject';
  rejectReason?: string;
  approvedAt: string;
}

export interface OvertimeStats {
  totalRecords: number;
  totalHours: number;
  totalPeople: number;
  pendingApprovals: number;
  thisMonthHours: number;
  thisMonthPeople: number;
  byType: {
    weekday: number;
    weekend: number;
    holiday: number;
  };
  byProject: {
    projectId: string;
    projectName: string;
    hours: number;
    count: number;
    totalHours?: number | string;
    recordCount?: number;
  }[];
  // 后端实际返回的字段
  approvedRecords?: number;
  approvedHours?: number | string;
  pendingRecords?: number;
  pendingHours?: number | string;
  rejectedRecords?: number;
}

export interface TaskOvertimeStats {
  taskId: string;
  taskName: string;
  assigneeId: string;
  assigneeName: string;
  recordCount: number;
  totalHours: number;
}

export interface Permission {
  id: string;
  code: string;
  name: string;
  type: 'system' | 'project';
  description: string;
}

/**
 * 角色管理 v2:5 个角色
 */
export type UserRole = 'admin' | 'dept-project-manager' | 'project-manager' | 'member' | 'viewer';

/**
 * 角色变更请求 DTO
 */
export interface RoleChangeRequest {
  newRole: UserRole;
  managedDeptCodes?: string[];
  managedCompanyCd?: string;
  managedProjectIds?: string[]; // 2026-06-12 新增,仅 newRole=project-manager 有效
  reason?: string;
}

/**
 * 角色变更历史记录
 */
export interface RoleChangeLog {
  id: number;
  userId: string;
  oldRole: string;
  newRole: string;
  oldManagedDeptCodes?: string;
  newManagedDeptCodes?: string;
  oldManagedCompanyCd?: string;
  newManagedCompanyCd?: string;
  changedBy: string;
  changedAt: string;
  reason?: string;
}

export interface PermissionCheckResult {
  hasPermission: boolean;
}

export interface ProjectPermissionCheckResult {
  isOwner: boolean;
  isMember: boolean;
}

export type ReportStatus = 'draft' | 'submitted' | 'approved' | 'rejected';

export interface WeeklyReport {
  id: string;
  userId: string;
  projectId?: string;
  weekStart: string;
  weekEnd: string;
  completedWork: string;
  nextWeekPlan: string;
  problems?: string;
  status: ReportStatus;
  submitTime?: string;
  approveTime?: string;
  approverId?: string;
  approveComment?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WeeklyReportComment {
  id: string;
  reportId: string;
  userId: string;
  content: string;
  createdAt: string;
}

/**
 * 周报审批历史日志(2026-06-14 新建)
 * 后端 GET /api/weekly-reports/{id}/approval-logs 返回
 */
export interface WeeklyReportApprovalLog {
  id: number;                    // 日志表自增 PK
  reportId: string;              // 对齐 WeeklyReport.id (String)
  approverId: string;
  approverName: string;
  approverAvatar?: string;
  approverRole: 'admin' | 'dept-project-manager' | 'project-manager' | 'project-owner';
  action: 'approve' | 'reject';
  comment?: string;
  createdAt: string;
}

export interface Document {
  id: string;
  projectId?: string;
  taskId?: string;
  name: string;
  category: string;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  fileExtension: string;
  version: number;
  parentId?: string;
  reportId?: string;
  description?: string;
  uploadedBy: string;
  status: string;
  downloadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentAccessLog {
  id: string;
  documentId: string;
  userId: string;
  action: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

export type DocumentCategory = 'requirements' | 'design' | 'development' | 'testing' | 'deployment' | 'documentation' | 'other';

export type DocumentStatus = 'active' | 'deleted';

export type DocumentAction = 'upload' | 'download' | 'update' | 'delete' | 'preview';

export interface OrgNode {
  /** 组织 code（来自 mdm_if_or_a.ORG_CD） */
  code: string;
  /** 组织名（来自 mdm_if_or_a.ORG_NAM） */
  name: string;
  /** 所属公司 code（来自 mdm_if_or_a.COMPANY_CD，2700/8400） */
  companyCd: string;
  /** 父组织 code（来自 mdm_if_or_a.PRNT_ORG_CD，根为 null） */
  parentCode: string | null;
  /** 组织层级数字（来自 mdm_if_or_a.ORG_LVL_NUM） */
  level: number;
  /** 子组织 */
  children: OrgNode[];
}
