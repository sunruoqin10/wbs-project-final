// Export utility for PDF and Excel

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';
import type { Project, Task, User, Statistics } from '@/types';

// Export to PDF
export const exportToPDF = {
  // Export projects list to PDF
  projects: (projects: Project[], filename = 'projects.pdf') => {
    const doc = new jsPDF();

    // Title
    doc.setFontSize(18);
    doc.text('项目列表', 14, 22);
    doc.setFontSize(11);
    doc.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, 14, 30);

    // Table data
    const tableData = projects.map(p => [
      p.name,
      p.description?.substring(0, 30) || '',
      getStatusLabel(p.status),
      getPriorityLabel(p.priority),
      `${p.progress}%`,
      p.startDate,
      p.endDate,
      p.memberIds?.length || 0
    ]);

    autoTable(doc, {
      head: [['项目名称', '描述', '状态', '优先级', '进度', '开始日期', '结束日期', '成员数']],
      body: tableData,
      startY: 40,
      styles: {
        fontSize: 9,
        cellPadding: 3
      },
      headStyles: {
        fillColor: [59, 130, 246],
        textColor: 255,
        fontStyle: 'bold'
      },
      alternateRowStyles: {
        fillColor: [248, 250, 252]
      }
    });

    doc.save(filename);
  },

  // Export tasks to PDF
  tasks: (tasks: Task[], projectName: string, filename = 'tasks.pdf') => {
    const doc = new jsPDF();

    // Title
    doc.setFontSize(18);
    doc.text(`${projectName} - 任务列表`, 14, 22);
    doc.setFontSize(11);
    doc.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, 14, 30);

    // Table data
    const tableData = tasks.map(t => [
      t.title,
      t.description?.substring(0, 30) || '',
      getStatusLabel(t.status),
      getPriorityLabel(t.priority),
      t.assigneeId || '未分配',
      t.startDate,
      t.endDate,
      `${t.progress}%`
    ]);

    autoTable(doc, {
      head: [['任务名称', '描述', '状态', '优先级', '负责人', '开始日期', '结束日期', '进度']],
      body: tableData,
      startY: 40,
      styles: {
        fontSize: 9,
        cellPadding: 3
      },
      headStyles: {
        fillColor: [16, 185, 129],
        textColor: 255,
        fontStyle: 'bold'
      },
      alternateRowStyles: {
        fillColor: [248, 250, 252]
      }
    });

    doc.save(filename);
  },

  // Export statistics report to PDF
  statistics: (stats: any, filename = 'statistics.pdf') => {
    const doc = new jsPDF();

    // Title
    doc.setFontSize(18);
    doc.text('项目统计报告', 14, 22);
    doc.setFontSize(11);
    doc.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, 14, 30);

    // Summary data - 兼容不同的数据结构
    const summaryData = [
      ['总项目数', (stats.totalProjects || 0).toString()],
      ['进行中项目', (stats.activeProjects || projectStatusCount(stats, 'active')).toString()],
      ['已完成项目', (stats.completedProjects || projectStatusCount(stats, 'completed')).toString()],
      ['总任务数', (stats.totalTasks || 0).toString()],
      ['已完成任务', (stats.completedTasks || 0).toString()],
      ['进行中任务', (stats.inProgressTasks || 0).toString()],
      ['团队成员', (stats.totalMembers || 0).toString()],
      ['完成率', `${stats.completionRate || 0}%`]
    ];

    autoTable(doc, {
      head: [['指标', '数值']],
      body: summaryData,
      startY: 40,
      styles: {
        fontSize: 11,
        cellPadding: 4
      },
      headStyles: {
        fillColor: [139, 92, 246],
        textColor: 255,
        fontStyle: 'bold'
      },
      columnStyles: {
        0: { cellWidth: 60 },
        1: { cellWidth: 40 }
      }
    });

    doc.save(filename);
  },

  // Export comprehensive report to PDF
  comprehensive: (data: {
    projects: Project[],
    tasks: Task[],
    users: User[],
    stats: any
  }, filename = 'comprehensive-report.pdf') => {
    const doc = new jsPDF();

    // Title
    doc.setFontSize(18);
    doc.text('WBS 项目管理系统 - 综合报表', 14, 22);
    doc.setFontSize(11);
    doc.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, 14, 30);

    let yPos = 40;

    // Summary section
    doc.setFontSize(14);
    doc.text('一、数据概览', 14, yPos);
    yPos += 10;

    const summaryData = [
      ['总项目数', `${data.projects.length}个`],
      ['总任务数', `${data.tasks.length}个`],
      ['团队成员', `${data.users.length}人`],
      ['完成任务', `${data.tasks.filter(t => t.status === 'done').length}个`],
      ['完成率', `${data.stats.completionRate || 0}%`]
    ];

    autoTable(doc, {
      head: [['指标', '数值']],
      body: summaryData,
      startY: yPos,
      styles: { fontSize: 10, cellPadding: 3 },
      headStyles: { fillColor: [59, 130, 246], textColor: 255 },
      columnStyles: { 0: { cellWidth: 50 }, 1: { cellWidth: 40 } }
    });

    yPos = (doc as any).lastAutoTable.finalY + 15;

    // Projects section
    doc.addPage();
    yPos = 20;
    doc.setFontSize(14);
    doc.text('二、项目列表', 14, yPos);
    yPos += 10;

    const projectData = data.projects.slice(0, 10).map(p => [
      p.name,
      getStatusLabel(p.status),
      `${p.progress}%`,
      p.startDate,
      p.endDate
    ]);

    autoTable(doc, {
      head: [['项目名称', '状态', '进度', '开始日期', '结束日期']],
      body: projectData,
      startY: yPos,
      styles: { fontSize: 9, cellPadding: 3 },
      headStyles: { fillColor: [16, 185, 129], textColor: 255 }
    });

    // Tasks section
    doc.addPage();
    yPos = 20;
    doc.setFontSize(14);
    doc.text('三、任务统计', 14, yPos);
    yPos += 10;

    const taskSummary = [
      ['待办', data.tasks.filter(t => t.status === 'todo').length.toString()],
      ['进行中', data.tasks.filter(t => t.status === 'in-progress').length.toString()],
      ['已完成', data.tasks.filter(t => t.status === 'done').length.toString()]
    ];

    autoTable(doc, {
      head: [['状态', '数量']],
      body: taskSummary,
      startY: yPos,
      styles: { fontSize: 10, cellPadding: 3 },
      headStyles: { fillColor: [245, 158, 11], textColor: 255 },
      columnStyles: { 0: { cellWidth: 50 }, 1: { cellWidth: 30 } }
    });

    doc.save(filename);
  }
};

// Helper function to count projects by status
function projectStatusCount(stats: any, status: string): number {
  return stats.projectsByStatus?.[status] || 0;
}

// Export to Excel
export const exportToExcel = {
  // Export projects to Excel
  projects: (projects: Project[], filename = 'projects.xlsx') => {
    const data = projects.map(p => ({
      '项目名称': p.name,
      '描述': p.description || '',
      '状态': getStatusLabel(p.status),
      '优先级': getPriorityLabel(p.priority),
      '进度': `${p.progress}%`,
      '开始日期': p.startDate,
      '结束日期': p.endDate,
      '负责人ID': p.ownerId,
      '成员数': p.memberIds?.length || 0,
      '标签': p.tags?.join(', ') || '',
      '颜色': p.color || ''
    }));

    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '项目列表');

    // Set column widths
    ws['!cols'] = [
      { wch: 20 }, { wch: 30 }, { wch: 10 }, { wch: 10 }, { wch: 8 }, { wch: 12 }, { wch: 12 }, { wch: 15 }, { wch: 8 }, { wch: 20 }, { wch: 8 }
    ];

    XLSX.writeFile(wb, filename);
  },

  // Export tasks to Excel
  tasks: (tasks: Task[], projectName: string, filename = 'tasks.xlsx') => {
    const data = tasks.map(t => ({
      '任务名称': t.title,
      '描述': t.description || '',
      '状态': getStatusLabel(t.status),
      '优先级': getPriorityLabel(t.priority),
      '负责人ID': t.assigneeId || '未分配',
      '开始日期': t.startDate,
      '结束日期': t.endDate,
      '预估工时': t.estimatedHours || '-',
      '实际工时': t.actualHours || '-',
      '进度': `${t.progress}%`,
      '标签': t.tags?.join(', ') || ''
    }));

    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, projectName);

    // Set column widths
    ws['!cols'] = [
      { wch: 20 }, { wch: 30 }, { wch: 10 }, { wch: 10 }, { wch: 15 }, { wch: 12 }, { wch: 12 }, { wch: 10 }, { wch: 10 }, { wch: 8 }, { wch: 20 }
    ];

    XLSX.writeFile(wb, filename);
  },

  // Export statistics to Excel
  statistics: (stats: any, filename = 'statistics.xlsx') => {
    const data = [
      { '指标': '总项目数', '数值': stats.totalProjects || 0, '说明': '系统中所有项目总数' },
      { '指标': '进行中项目', '数值': stats.activeProjects || 0, '说明': '状态为进行中的项目' },
      { '指标': '已完成项目', '数值': stats.completedProjects || 0, '说明': '状态为已完成的项目' },
      { '指标': '总任务数', '数值': stats.totalTasks || 0, '说明': '系统中所有任务总数' },
      { '指标': '已完成任务', '数值': stats.completedTasks || 0, '说明': '状态为已完成的任务' },
      { '指标': '进行中任务', '数值': stats.inProgressTasks || 0, '说明': '状态为进行中的任务' },
      { '指标': '团队成员', '数值': stats.totalMembers || 0, '说明': '系统中注册的成员数量' },
      { '指标': '完成率', '数值': `${stats.completionRate || 0}%`, '说明': '任务完成百分比' }
    ];

    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '统计数据');

    // Set column widths
    ws['!cols'] = [{ wch: 15 }, { wch: 15 }, { wch: 30 }];

    XLSX.writeFile(wb, filename);
  },

  // Export comprehensive report to Excel with multiple sheets
  comprehensive: (data: {
    projects: Project[],
    tasks: Task[],
    users: User[],
    stats: any
  }, filename = 'comprehensive-report.xlsx') => {
    const wb = XLSX.utils.book_new();

    // Sheet 1: Statistics
    const statsData = [
      { '指标': '总项目数', '数值': data.projects.length },
      { '指标': '总任务数', '数值': data.tasks.length },
      { '指标': '团队成员', '数值': data.users.length },
      { '指标': '完成任务', '数值': data.tasks.filter(t => t.status === 'done').length },
      { '指标': '进行中任务', '数值': data.tasks.filter(t => t.status === 'in-progress').length },
      { '指标': '完成率', '数值': `${data.stats.completionRate || 0}%` }
    ];
    const statsWs = XLSX.utils.json_to_sheet(statsData);
    XLSX.utils.book_append_sheet(wb, statsWs, '数据概览');

    // Sheet 2: Projects
    const projectsData = data.projects.map(p => ({
      '项目名称': p.name,
      '状态': getStatusLabel(p.status),
      '优先级': getPriorityLabel(p.priority),
      '进度': `${p.progress}%`,
      '开始日期': p.startDate,
      '结束日期': p.endDate
    }));
    const projectsWs = XLSX.utils.json_to_sheet(projectsData);
    XLSX.utils.book_append_sheet(wb, projectsWs, '项目列表');

    // Sheet 3: Tasks
    const tasksData = data.tasks.slice(0, 500).map(t => ({
      '任务名称': t.title,
      '状态': getStatusLabel(t.status),
      '优先级': getPriorityLabel(t.priority),
      '进度': `${t.progress}%`,
      '开始日期': t.startDate,
      '结束日期': t.endDate
    }));
    const tasksWs = XLSX.utils.json_to_sheet(tasksData);
    XLSX.utils.book_append_sheet(wb, tasksWs, '任务列表');

    // Sheet 4: Users
    const usersData = data.users.map(u => ({
      '姓名': u.name,
      '邮箱': u.email,
      '角色': getRoleLabel(u.role),
      '部门': u.department,
      '技能': u.skills.join(', ')
    }));
    const usersWs = XLSX.utils.json_to_sheet(usersData);
    XLSX.utils.book_append_sheet(wb, usersWs, '团队成员');

    XLSX.writeFile(wb, filename);
  }
};

// Helper functions
function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    planning: '计划中',
    active: '进行中',
    completed: '已完成',
    'on-hold': '已暂停',
    cancelled: '已取消',
    todo: '待办',
    'in-progress': '进行中',
    review: '审核中',
    done: '已完成'
  };
  return labels[status] || status;
}

function getPriorityLabel(priority: string): string {
  const labels: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    urgent: '紧急',
    critical: '紧急'
  };
  return labels[priority] || priority;
}

function getRoleLabel(role: string): string {
  const normalizedRole = role?.replace(/_/g, '-');
  const labels: Record<string, string> = {
    admin: '管理员',
    'project-manager': '项目经理',
    member: '成员',
    viewer: '观察者'
  };
  return labels[normalizedRole] || role;
}
