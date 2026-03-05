import ExcelJS from 'exceljs';
import type { Project, Task, User } from '@/types/index';
import { saveAs } from 'file-saver';

// ==================== Type Definitions ====================

export interface ExportColumn {
  key: string;
  label: string;
  width?: number;
}

export interface StatisticsData {
  stats: {
    totalProjects: number;
    totalTasks: number;
    completedTasks: number;
    inProgressTasks: number;
  };
  projects: Project[];
  tasks: Task[];
  users: User[];
}

export interface ComprehensiveData {
  projects: Project[];
  tasks: Task[];
  users: User[];
  stats: {
    totalProjects: number;
    totalTasks: number;
    completedTasks: number;
    inProgressTasks: number;
  };
}

// ==================== Excel 样式配置 ====================

// 表头样式
const headerStyle = {
  font: { bold: true, color: { argb: 'FFFFFFFF' }, size: 12 },
  fill: {
    type: 'pattern' as const,
    pattern: 'solid' as const,
    fgColor: { argb: 'FF4472C4' }
  },
  alignment: { vertical: 'middle' as const, horizontal: 'center' as const },
  border: {
    top: { style: 'thin' as const },
    left: { style: 'thin' as const },
    bottom: { style: 'thin' as const },
    right: { style: 'thin' as const }
  }
};

// 数据单元格样式
const cellStyle = {
  font: { size: 11 },
  alignment: { vertical: 'middle' as const, horizontal: 'left' as const, wrapText: true },
  border: {
    top: { style: 'thin' as const },
    left: { style: 'thin' as const },
    bottom: { style: 'thin' as const },
    right: { style: 'thin' as const }
  }
};

// 数字单元格样式（右对齐）
const numberCellStyle = {
  font: { size: 11 },
  alignment: { vertical: 'middle' as const, horizontal: 'right' as const },
  border: {
    top: { style: 'thin' as const },
    left: { style: 'thin' as const },
    bottom: { style: 'thin' as const },
    right: { style: 'thin' as const }
  }
};

// ==================== Translation Functions ====================

function translateProjectStatus(status: string): string {
  const statusMap: Record<string, string> = {
    'planning': '规划中',
    'active': '进行中',
    'completed': '已完成',
    'on-hold': '暂停',
    'cancelled': '已取消'
  };
  return statusMap[status] || status;
}

function translateTaskStatus(status: string): string {
  const statusMap: Record<string, string> = {
    'todo': '待办',
    'in-progress': '进行中',
    'done': '已完成'
  };
  return statusMap[status] || status;
}

function translatePriority(priority: string): string {
  const priorityMap: Record<string, string> = {
    'low': '低',
    'medium': '中',
    'high': '高',
    'urgent': '紧急',
    'critical': '严重'
  };
  return priorityMap[priority] || priority;
}

function translateRole(role: string): string {
  const roleMap: Record<string, string> = {
    'admin': '管理员',
    'project-manager': '项目经理',
    'member': '成员',
    'viewer': '访客'
  };
  return roleMap[role] || role;
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getUserName(userId: string | undefined, users: User[]): string {
  if (!userId) return '';
  const user = users.find(u => u.id === userId);
  return user?.name || userId;
}

function formatSkills(skills: string[] | undefined): string {
  if (!skills || skills.length === 0) return '';
  return skills.join(', ');
}

// ==================== Helper Functions ====================

/**
 * 获取叶子任务（没有子任务的任务）
 */
function getLeafTasks(tasks: Task[]): Task[] {
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId!));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  return tasks.filter(t => leafTaskIds.has(t.id));
}

/**
 * 计算延期天数
 */
function calculateDelayedDays(task: Task): number {
  if (!task.isDelayed || !task.delayedDays) return 0;
  return task.delayedDays;
}

/**
 * 应用样式到工作表
 */
function applyWorksheetStyles(worksheet: ExcelJS.Worksheet, rowCount: number, columnCount: number): void {
  // 应用表头样式（第一行）
  worksheet.getRow(1).eachCell((cell) => {
    cell.style = headerStyle;
  });

  // 应用数据行样式
  for (let row = 2; row <= rowCount; row++) {
    worksheet.getRow(row).eachCell((cell, colNumber) => {
      // 判断是否为数字列（简单判断）
      const value = cell.value;
      if (typeof value === 'number') {
        cell.style = numberCellStyle;
      } else {
        cell.style = cellStyle;
      }
    });
  }

  // 冻结首行
  worksheet.views = [
    { state: 'frozen', xSplit: 0, ySplit: 1 }
  ];
}

/**
 * 创建工作表
 */
async function createWorksheet(
  workbook: ExcelJS.Workbook,
  sheetName: string,
  data: any[][],
  columns: ExportColumn[]
): Promise<ExcelJS.Worksheet> {
  const worksheet = workbook.addWorksheet(sheetName);

  // 添加数据
  data.forEach((row, rowIndex) => {
    const rowNumber = rowIndex + 1;
    worksheet.addRow(row);

    // 设置行高
    if (rowIndex === 0) {
      worksheet.getRow(rowNumber).height = 25; // 表头行高
    } else {
      worksheet.getRow(rowNumber).height = 20; // 数据行高
    }
  });

  // 设置列宽
  columns.forEach((col, index) => {
    worksheet.getColumn(index + 1).width = col.width || 15;
  });

  // 应用样式
  applyWorksheetStyles(worksheet, data.length, columns.length);

  return worksheet;
}

/**
 * 导出多个工作表到单个Excel文件
 */
async function exportMultipleSheets(
  sheets: Array<{ name: string; data: any[][]; columns: ExportColumn[] }>,
  filename: string
): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'WBS Project Management System';
  workbook.created = new Date();

  // 创建所有工作表
  for (const sheet of sheets) {
    await createWorksheet(workbook, sheet.name, sheet.data, sheet.columns);
  }

  // 生成文件
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  saveAs(blob, filename);
}

// ==================== Export Methods ====================

/**
 * 导出项目列表（未使用，保留以备后用）
 */
async function exportProjects(projects: Project[], filename: string): Promise<void> {
  const columns: ExportColumn[] = [
    { key: 'name', label: '项目名称', width: 25 },
    { key: 'description', label: '描述', width: 40 },
    { key: 'status', label: '状态', width: 12 },
    { key: 'priority', label: '优先级', width: 12 },
    { key: 'startDate', label: '开始日期', width: 15 },
    { key: 'endDate', label: '结束日期', width: 15 },
    { key: 'progress', label: '进度(%)', width: 10 },
    { key: 'delayedTasks', label: '延期任务数', width: 12 },
    { key: 'totalDelayedDays', label: '总延期天数', width: 12 }
  ];

  const data: any[][] = [];

  // 表头
  data.push(columns.map(col => col.label));

  // 数据行
  projects.forEach(project => {
    data.push([
      project.name,
      project.description,
      translateProjectStatus(project.status),
      translatePriority(project.priority),
      formatDate(project.startDate),
      formatDate(project.endDate),
      project.progress,
      project.delayedTasks || 0,
      project.totalDelayedDays || 0
    ]);
  });

  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'WBS Project Management System';
  await createWorksheet(workbook, '项目列表', data, columns);

  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  saveAs(blob, filename);
}

/**
 * 导出统计数据（未使用，保留以备后用）
 */
async function exportStatistics(data: StatisticsData, filename: string): Promise<void> {
  const sheets: Array<{ name: string; data: any[][]; columns: ExportColumn[] }> = [];

  // 工作表1: 总体统计
  sheets.push({
    name: '总体统计',
    data: [
      ['统计项', '数值'],
      ['总项目数', data.stats.totalProjects],
      ['总任务数', data.stats.totalTasks],
      ['完成任务数', data.stats.completedTasks],
      ['进行中任务数', data.stats.inProgressTasks],
      ['任务完成率', data.stats.totalTasks > 0
        ? `${Math.round((data.stats.completedTasks / data.stats.totalTasks) * 100)}%`
        : '0%']
    ],
    columns: [{ key: 'name', label: '统计项', width: 20 }, { key: 'value', label: '数值', width: 15 }]
  });

  // 工作表2: 项目状态分布
  const statusCounts = {
    'planning': data.projects.filter(p => p.status === 'planning').length,
    'active': data.projects.filter(p => p.status === 'active').length,
    'completed': data.projects.filter(p => p.status === 'completed').length,
    'on-hold': data.projects.filter(p => p.status === 'on-hold').length,
    'cancelled': data.projects.filter(p => p.status === 'cancelled').length
  };

  const statusData: any[][] = [['状态', '数量', '百分比']];
  Object.entries(statusCounts).forEach(([status, count]) => {
    const percentage = data.stats.totalProjects > 0
      ? `${Math.round((count / data.stats.totalProjects) * 100)}%`
      : '0%';
    statusData.push([translateProjectStatus(status), count, percentage]);
  });
  sheets.push({
    name: '项目状态分布',
    data: statusData,
    columns: [
      { key: 'status', label: '状态', width: 12 },
      { key: 'count', label: '数量', width: 10 },
      { key: 'percentage', label: '百分比', width: 12 }
    ]
  });

  // 工作表3: 任务优先级分布
  const leafTasks = getLeafTasks(data.tasks);
  const priorityCounts = {
    'low': leafTasks.filter(t => t.priority === 'low').length,
    'medium': leafTasks.filter(t => t.priority === 'medium').length,
    'high': leafTasks.filter(t => t.priority === 'high').length,
    'urgent': leafTasks.filter(t => t.priority === 'urgent').length
  };

  const priorityData: any[][] = [['优先级', '数量', '百分比']];
  Object.entries(priorityCounts).forEach(([priority, count]) => {
    const percentage = leafTasks.length > 0
      ? `${Math.round((count / leafTasks.length) * 100)}%`
      : '0%';
    priorityData.push([translatePriority(priority), count, percentage]);
  });
  sheets.push({
    name: '任务优先级分布',
    data: priorityData,
    columns: [
      { key: 'priority', label: '优先级', width: 12 },
      { key: 'count', label: '数量', width: 10 },
      { key: 'percentage', label: '百分比', width: 12 }
    ]
  });

  // 工作表4: 成员绩效
  const memberData: any[][] = [['成员姓名', '完成任务', '进行中任务', '总任务数']];
  data.users.forEach(user => {
    const completedTasks = leafTasks.filter(t => t.assigneeId === user.id && t.status === 'done').length;
    const inProgressTasks = leafTasks.filter(t => t.assigneeId === user.id && t.status === 'in-progress').length;
    const totalTasks = completedTasks + inProgressTasks;
    memberData.push([user.name, completedTasks, inProgressTasks, totalTasks]);
  });
  sheets.push({
    name: '成员绩效',
    data: memberData,
    columns: [
      { key: 'name', label: '成员姓名', width: 15 },
      { key: 'completed', label: '完成任务', width: 12 },
      { key: 'inProgress', label: '进行中任务', width: 15 },
      { key: 'total', label: '总任务数', width: 12 }
    ]
  });

  await exportMultipleSheets(sheets, filename);
}

/**
 * 导出综合报表
 */
async function exportComprehensive(data: ComprehensiveData, filename: string): Promise<void> {
  const sheets: Array<{ name: string; data: any[][]; columns: ExportColumn[] }> = [];

  // 工作表1: 项目总览
  const projectColumns: ExportColumn[] = [
    { key: 'name', label: '项目名称', width: 25 },
    { key: 'status', label: '状态', width: 12 },
    { key: 'priority', label: '优先级', width: 12 },
    { key: 'startDate', label: '开始日期', width: 15 },
    { key: 'endDate', label: '结束日期', width: 15 },
    { key: 'progress', label: '进度', width: 10 },
    { key: 'owner', label: '负责人', width: 15 },
    { key: 'members', label: '成员数', width: 10 },
    { key: 'tasks', label: '任务数', width: 10 },
    { key: 'delayedTasks', label: '延期任务', width: 12 }
  ];

  const projectData: any[][] = [projectColumns.map(col => col.label)];
  data.projects.forEach(project => {
    const memberCount = project.memberIds?.length || 0;
    const taskCount = data.tasks.filter(t => t.projectId === project.id).length;
    projectData.push([
      project.name,
      translateProjectStatus(project.status),
      translatePriority(project.priority),
      formatDate(project.startDate),
      formatDate(project.endDate),
      project.progress,
      getUserName(project.ownerId, data.users),
      memberCount,
      taskCount,
      project.delayedTasks || 0
    ]);
  });
  sheets.push({ name: '项目总览', data: projectData, columns: projectColumns });

  // 工作表2: 任务明细
  const leafTasks = getLeafTasks(data.tasks);
  const taskColumns: ExportColumn[] = [
    { key: 'title', label: '任务标题', width: 30 },
    { key: 'project', label: '所属项目', width: 20 },
    { key: 'status', label: '状态', width: 12 },
    { key: 'priority', label: '优先级', width: 12 },
    { key: 'assignee', label: '负责人', width: 15 },
    { key: 'startDate', label: '开始日期', width: 15 },
    { key: 'endDate', label: '结束日期', width: 15 },
    { key: 'progress', label: '进度', width: 10 },
    { key: 'hours', label: '预估工时', width: 12 },
    { key: 'delayedDays', label: '延期天数', width: 12 }
  ];

  const taskData: any[][] = [taskColumns.map(col => col.label)];
  leafTasks.forEach(task => {
    const project = data.projects.find(p => p.id === task.projectId);
    taskData.push([
      task.title,
      project?.name || '',
      translateTaskStatus(task.status),
      translatePriority(task.priority),
      getUserName(task.assigneeId, data.users),
      formatDate(task.startDate),
      formatDate(task.endDate),
      task.progress,
      task.estimatedHours || '-',
      calculateDelayedDays(task)
    ]);
  });
  sheets.push({ name: '任务明细', data: taskData, columns: taskColumns });

  // 工作表3: 成员列表
  const userColumns: ExportColumn[] = [
    { key: 'name', label: '姓名', width: 15 },
    { key: 'email', label: '邮箱', width: 25 },
    { key: 'role', label: '角色', width: 15 },
    { key: 'department', label: '部门', width: 20 },
    { key: 'skills', label: '技能', width: 30 },
    { key: 'completedTasks', label: '完成任务数', width: 12 },
    { key: 'inProgressTasks', label: '进行中任务数', width: 15 }
  ];

  const userData: any[][] = [userColumns.map(col => col.label)];
  data.users.forEach(user => {
    const completedTasks = leafTasks.filter(t => t.assigneeId === user.id && t.status === 'done').length;
    const inProgressTasks = leafTasks.filter(t => t.assigneeId === user.id && t.status === 'in-progress').length;
    userData.push([
      user.name,
      user.email,
      translateRole(user.role),
      user.department,
      formatSkills(user.skills),
      completedTasks,
      inProgressTasks
    ]);
  });
  sheets.push({ name: '成员列表', data: userData, columns: userColumns });

  // 工作表4: 统计汇总
  const statsData: any[][] = [
    ['统计项', '数值'],
    ['总项目数', data.stats.totalProjects],
    ['总任务数', data.stats.totalTasks],
    ['完成任务数', data.stats.completedTasks],
    ['进行中任务数', data.stats.inProgressTasks],
    ['总成员数', data.users.length],
    ['任务完成率', data.stats.totalTasks > 0
      ? `${Math.round((data.stats.completedTasks / data.stats.totalTasks) * 100)}%`
      : '0%']
  ];
  sheets.push({
    name: '统计汇总',
    data: statsData,
    columns: [
      { key: 'name', label: '统计项', width: 20 },
      { key: 'value', label: '数值', width: 15 }
    ]
  });

  await exportMultipleSheets(sheets, filename);
}

// ==================== Export Namespace (New API) ====================

/**
 * 导出命名空间对象
 * 支持新的调用方式：
 * - exportToExcelNamespace.projects(projects, filename)
 * - exportToExcelNamespace.statistics(data, filename)
 * - exportToExcelNamespace.comprehensive(data, filename)
 */
export const exportToExcelNamespace = {
  projects: exportProjects,
  statistics: exportStatistics,
  comprehensive: exportComprehensive
};

// 默认导出命名空间
export default exportToExcelNamespace;

// ==================== Legacy Export Function (Backward Compatible) ====================

/**
 * 通用导出函数（保留以保持向后兼容）
 * 支持旧的调用方式：exportToExcel(data, columns, filename)
 *
 * @example
 * exportToExcel(data, columns, 'filename')
 */
export async function exportToExcel<T>(
  data: T[],
  columns: ExportColumn[],
  filename: string
): Promise<void> {
  const worksheetData: any[][] = [];

  worksheetData.push(columns.map(col => col.label));

  data.forEach(row => {
    const rowData: any[] = [];
    columns.forEach(col => {
      rowData.push((row as any)[col.key] || '');
    });
    worksheetData.push(rowData);
  });

  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'WBS Project Management System';
  await createWorksheet(workbook, 'Sheet1', worksheetData, columns);

  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  saveAs(blob, filename);
}
