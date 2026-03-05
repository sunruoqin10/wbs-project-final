import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';
import type { Project, Task, User } from '@/types/index';

// ==================== 类型定义 ====================

interface GanttExportData {
  projects: Project[];
  tasks: Task[];
  users: User[];
}

// ==================== 辅助函数 ====================

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

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateShort(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${month}/${day}`;
}

function getUserName(userId: string | undefined, users: User[]): string {
  if (!userId) return '';

  const userIdStr = String(userId);

  const user = users.find(u => {
    const uid = String(u.id);
    return uid === userIdStr;
  });

  if (user) {
    return user.name;
  }

  const fallbackUser = users.find(u => {
    const uid = String(u.id);
    return uid.includes(userIdStr) || userIdStr.includes(uid);
  });

  if (fallbackUser) {
    return fallbackUser.name;
  }

  return '未分配';
}

function getTaskIndent(level: number): string {
  return '  '.repeat(level);
}

function buildTaskHierarchy(
  tasks: Task[],
  parentId: string | null = null,
  level: number = 0
): Array<{ task: Task; level: number }> {
  const result: Array<{ task: Task; level: number }> = [];

  const children = tasks.filter(t => {
    if (parentId === null) {
      return !t.parentTaskId;
    }
    return t.parentTaskId === parentId;
  });

  children.forEach(child => {
    result.push({ task: child, level });
    result.push(...buildTaskHierarchy(tasks, child.id, level + 1));
  });

  return result;
}

function getDateRange(tasks: Task[]): { minDate: Date; maxDate: Date } {
  let minDate = new Date('2099-12-31');
  let maxDate = new Date('2000-01-01');

  tasks.forEach(task => {
    const start = new Date(task.startDate);
    const end = new Date(task.endDate);
    if (start < minDate) minDate = start;
    if (end > maxDate) maxDate = end;
  });

  // 添加一些边距
  minDate.setDate(minDate.getDate() - 2);
  maxDate.setDate(maxDate.getDate() + 5);

  return { minDate, maxDate };
}

function generateDateRange(minDate: Date, maxDate: Date): Date[] {
  const dates: Date[] = [];
  const current = new Date(minDate);

  while (current <= maxDate) {
    dates.push(new Date(current));
    current.setDate(current.getDate() + 1);
  }

  return dates;
}

function isWeekend(date: Date): boolean {
  const day = date.getDay();
  return day === 0 || day === 6;
}

function getTaskColor(task: Task): string {
  // 先检查延期
  if (task.isDelayed && task.delayedDays && task.delayedDays > 0) {
    if (task.delayedDays >= 7) return 'FFFF6B6B';  // 红色
    if (task.delayedDays >= 3) return 'FFFFB74D';  // 橙色
    return 'FF64B5F6';  // 蓝色
  }

  // 按状态设置颜色
  const colors: Record<string, string> = {
    'todo': 'FFBDC3C7',         // 灰色
    'in-progress': 'FF3498DB',  // 蓝色
    'done': 'FF27AE60'          // 绿色
  };
  return colors[task.status] || colors['todo'];
}

// ==================== 主导出函数 ====================

/**
 * 导出甘特图（带时间轴和进度条的可视化甘特图）
 * @param data - 包含项目、任务、用户的数据
 * @param filename - 导出的文件名
 */
export async function exportGanttChart(
  data: GanttExportData,
  filename: string
): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'WBS Project Management System';
  workbook.created = new Date();

  // 为每个项目创建一个工作表
  for (const project of data.projects) {
    const projectTasks = data.tasks.filter(t => t.projectId === project.id);

    if (projectTasks.length === 0) {
      continue;
    }

    // 工作表名称
    const sheetName = project.name.substring(0, 31);
    const worksheet = workbook.addWorksheet(sheetName);

    // 第1行：项目信息
    worksheet.addRow([`项目: ${project.name}`, `状态: ${translateProjectStatus(project.status)}`,
                     `优先级: ${translatePriority(project.priority)}`, `进度: ${project.progress}%`]);
    worksheet.lastRow.eachCell((cell) => {
      cell.style = {
        font: { bold: true, size: 11, color: { argb: 'FF0070C0' } },
        fill: { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE7E6E6' } },
        alignment: { vertical: 'middle', horizontal: 'left' }
      };
    });

    // 第2行：空行
    worksheet.addRow([]);

    // 获取日期范围
    const { minDate, maxDate } = getDateRange(projectTasks);
    const dateRange = generateDateRange(minDate, maxDate);

    // 构建任务层级
    const taskHierarchy = buildTaskHierarchy(projectTasks, null, 0);

    // 左侧信息列数
    const infoColCount = 4;

    // 第3行：月份行 - 不合并，直接在每个单元格显示月份
    const monthRow = ['任务名称', '状态', '负责人', '进度'];
    dateRange.forEach(date => {
      const monthLabel = `${date.getMonth() + 1}月`;
      monthRow.push(monthLabel);
    });
    worksheet.addRow(monthRow);

    // 设置月份行样式
    const monthRowObj = worksheet.getRow(3);
    for (let col = 1; col <= infoColCount + dateRange.length; col++) {
      const cell = monthRowObj.getCell(col);

      if (col <= infoColCount) {
        // 左侧信息列 - 空白
        cell.value = '';
        cell.style = {
          font: { size: 10 },
          alignment: { vertical: 'middle', horizontal: 'center' },
          fill: { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE7E6E6' } }
        };
      } else {
        // 月份列 - 显示月份
        cell.style = {
          font: { bold: true, size: 9, color: { argb: 'FF0070C0' } },
          fill: { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE7E6E6' } },
          alignment: { vertical: 'middle', horizontal: 'center' }
        };
      }
    }

    // 第4行：日期表头
    const headerRow = ['任务名称', '状态', '负责人', '进度'];
    dateRange.forEach(date => {
      headerRow.push(formatDateShort(date));
    });
    worksheet.addRow(headerRow);

    // 设置日期表头样式
    const headerRowObj = worksheet.getRow(4);
    for (let col = 1; col <= infoColCount + dateRange.length; col++) {
      const cell = headerRowObj.getCell(col);
      cell.style = {
        font: { bold: true, size: 11, color: { argb: 'FFFFFFFF' } },
        fill: { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF4472C4' } },
        alignment: { vertical: 'middle', horizontal: 'center' },
        border: {
          top: { style: 'thin', color: { argb: 'FFD3D3D3' } },
          left: { style: 'thin', color: { argb: 'FFD3D3D3' } },
          bottom: { style: 'thin', color: { argb: 'FFD3D3D3' } },
          right: { style: 'thin', color: { argb: 'FFD3D3D3' } }
        }
      };
    }

    // 第5行开始：任务数据
    taskHierarchy.forEach(({ task, level }) => {
      const hasSubtasks = projectTasks.some(t => t.parentTaskId === task.id);
      const indent = getTaskIndent(level);
      const icon = hasSubtasks ? '📁 ' : '📄 ';
      const taskTitle = indent + icon + task.title;

      const taskStart = new Date(task.startDate);
      const taskEnd = new Date(task.endDate);

      // 添加基本信息
      const rowData = [
        taskTitle,
        translateTaskStatus(task.status),
        getUserName(task.assigneeId, data.users),
        `${task.progress}%`
      ];

      // 添加时间轴单元格（不添加字符，只用背景色）
      dateRange.forEach(date => {
        const isInRange = date >= taskStart && date <= taskEnd;
        rowData.push(''); // 空字符串，只通过背景色显示进度
      });

      worksheet.addRow(rowData);

      const currentRow = worksheet.lastRow;

      // 设置单元格样式和颜色
      for (let col = 1; col <= infoColCount + dateRange.length; col++) {
        const cell = currentRow.getCell(col);

        if (col <= infoColCount) {
          // 左侧信息列样式
          cell.style = {
            font: { size: 10 },
            alignment: { vertical: 'middle', horizontal: 'left' },
            border: {
              top: { style: 'thin', color: { argb: 'FFD3D3D3' } },
              left: { style: 'thin', color: { argb: 'FFD3D3D3' } },
              bottom: { style: 'thin', color: { argb: 'FFD3D3D3' } },
              right: { style: 'thin', color: { argb: 'FFD3D3D3' } }
            }
          };
        } else {
          // 时间轴列
          const dateIndex = col - infoColCount - 1;
          const currentDate = dateRange[dateIndex];
          const isInRange = currentDate >= taskStart && currentDate <= taskEnd;

          if (isInRange) {
            // 计算进度
            const totalDays = Math.ceil((taskEnd.getTime() - taskStart.getTime()) / (1000 * 60 * 60 * 24));
            const daysPassed = Math.ceil((currentDate.getTime() - taskStart.getTime()) / (1000 * 60 * 60 * 24));
            const progressRatio = task.progress / 100;
            const progressDay = Math.floor(totalDays * progressRatio);
            const isInCompleted = daysPassed <= progressDay;

            const baseColor = getTaskColor(task);

            cell.style = {
              font: {
                size: 14,
                color: { argb: isInCompleted ? 'FFFFFFFF' : baseColor }
              },
              fill: {
                type: 'pattern',
                pattern: 'solid',
                fgColor: { argb: isInCompleted ? baseColor : 'FFF5F5DC' }
              },
              alignment: { vertical: 'middle', horizontal: 'center' },
              border: {
                top: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                left: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                bottom: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                right: { style: 'thin', color: { argb: 'FFD3D3D3' } }
              }
            };
          } else {
            // 非任务期间
            cell.style = {
              font: { size: 10 },
              alignment: { vertical: 'middle', horizontal: 'center' },
              border: {
                top: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                left: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                bottom: { style: 'thin', color: { argb: 'FFD3D3D3' } },
                right: { style: 'thin', color: { argb: 'FFD3D3D3' } }
              }
            };

            // 周末标记
            if (isWeekend(currentDate)) {
              cell.style.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE8E8E8' } };
            }
          }
        }
      }
    });

    // 设置列宽
    worksheet.getColumn(1).width = 35;  // 任务名称
    worksheet.getColumn(2).width = 10;  // 状态
    worksheet.getColumn(3).width = 12;  // 负责人
    worksheet.getColumn(4).width = 10;  // 进度

    // 时间轴列宽
    for (let i = infoColCount + 1; i <= infoColCount + dateRange.length; i++) {
      worksheet.getColumn(i).width = 3.5;
    }

    // 设置行高
    for (let row = 5; row <= worksheet.rowCount; row++) {
      worksheet.getRow(row).height = 28;
    }

    // 冻结窗格
    worksheet.views = [
      {
        state: 'frozen',
        xSplit: infoColCount,
        ySplit: 4
      }
    ];
  }

  // 如果没有项目数据，添加提示工作表
  if (data.projects.length === 0) {
    const worksheet = workbook.addWorksheet('提示');
    worksheet.addRow(['没有项目数据']);
    worksheet.addRow(['请先创建项目后再导出甘特图']);
  }

  // 生成文件
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  saveAs(blob, filename);
}
