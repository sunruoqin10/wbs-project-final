<template>
  <div class="gantt-wrapper">
    <div ref="ganttContainer" class="w-full" style="height: 500px;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue';
import { useI18n } from 'vue-i18n';
import { gantt } from 'dhtmlx-gantt';
import 'dhtmlx-gantt/codebase/skins/dhtmlxgantt_meadow.css';
import type { Task } from '@/types';
import type { Project } from '@/types';
import { useUserStore } from '@/stores/user';

interface GanttTask {
  id: string | number;
  text: string;
  start_date: string;
  duration: number;
  workingDays: number;
  progress: number;
  parent: string | number;
  priority?: string;
  assigneeId?: string;
  color?: string;
  description?: string;
  status?: string;
  estimatedHours?: number;
  actualHours?: number;
  delayedDays?: number;
  isDelayed?: boolean;
  hasSubtasks?: boolean;
  childrenDelayedCount?: number;
  childrenTotalDelayedDays?: number;
  actualStartDate?: string;
  actualEndDate?: string;
}

interface Props {
  tasks: Task[];
  project: Project;
  scale: 'day' | 'week' | 'month';
}

const props = defineProps<Props>();
const { t } = useI18n();
const userStore = useUserStore();
const isInitialized = ref(false);

const ganttContainer = ref<HTMLElement>();

const convertToGanttTasks = (): GanttTask[] => {
  return props.tasks.map(task => {
    const hasSubtasks = props.tasks.some(t => t.parentTaskId === task.id);
    return {
      id: task.id,
      text: task.title,
      start_date: task.startDate,
      duration: calculateDuration(task.startDate, task.endDate),
      workingDays: countWorkingDays(task.startDate, task.endDate),
      progress: task.progress / 100,
      parent: task.parentTaskId || 0,
      priority: task.priority,
      assigneeId: task.assigneeId,
      color: getTaskColor(task, hasSubtasks),
      description: task.description,
      status: task.status,
      estimatedHours: task.estimatedHours,
      actualHours: task.actualHours,
      delayedDays: task.delayedDays,
      isDelayed: task.isDelayed,
      hasSubtasks,
      childrenDelayedCount: task.childrenDelayedCount,
      childrenTotalDelayedDays: task.childrenTotalDelayedDays,
      actualStartDate: task.actualStartDate,
      actualEndDate: task.actualEndDate
    };
  });
};

const calculateDuration = (start: string, end: string): number => {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const diffTime = Math.abs(endDate.getTime() - startDate.getTime());
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays + 1;
};

const countWorkingDays = (start: string, end: string): number => {
  const startDate = new Date(start);
  const endDate = new Date(end);
  let count = 0;
  const current = new Date(startDate);
  while (current <= endDate) {
    const day = current.getDay();
    if (day !== 0 && day !== 6) {
      count++;
    }
    current.setDate(current.getDate() + 1);
  }
  return count || 1;
};

const getTaskColor = (task: Task, hasSubtasks: boolean = false): string => {
  // 父任务：优先显示子任务延期状态
  if (hasSubtasks) {
    const childrenDelayedCount = task.childrenDelayedCount || 0;
    const childrenTotalDelayedDays = task.childrenTotalDelayedDays || 0;
    if (childrenDelayedCount > 0) {
      if (childrenTotalDelayedDays >= 7) return '#ef4444';    // 红色
      if (childrenTotalDelayedDays >= 3) return '#f59e0b';    // 橙色
      return '#3b82f6';                                       // 蓝色
    }
  }
  
  // 无子任务的任务：显示自己的延期状态
  if (task.isDelayed || (task.delayedDays || 0) > 0) {
    const days = task.delayedDays || 0;
    if (days >= 7) return '#ef4444';    // 红色
    if (days >= 3) return '#f59e0b';    // 橙色
    return '#3b82f6';                    // 蓝色
  }

  // 按状态设置颜色
  const colors: Record<string, string> = {
    'todo': '#95a5a6',         // 灰色 - 待办
    'in-progress': '#3498db',  // 蓝色 - 进行中
    'done': '#27ae60'          // 绿色 - 已完成
  };
  return colors[task.status] || colors['todo'];
};

// 辅助函数：获取状态颜色
const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    'todo': '#95a5a6',
    'in-progress': '#3498db',
    'done': '#27ae60'
  };
  return colors[status] || '#7f8c8d';
};

// 辅助函数：获取优先级颜色
const getPriorityColor = (priority: string): string => {
  const colors: Record<string, string> = {
    'low': '#008080',
    'medium': '#CD853F',
    'high': '#9370D8',
    'urgent': '#F08080'
  };
  return colors[priority] || '#7f8c8d';
};

const getScaleConfig = () => {
  switch (props.scale) {
    case 'day':
      return [
        { unit: 'month', step: 1, format: t('gantt.yearMonthFormat') },
        { unit: 'day', step: 1, format: t('gantt.dayFormat') }
      ];
    case 'week':
      return [
        { unit: 'month', step: 1, format: t('gantt.yearMonthFormat') },
        { unit: 'week', step: 1, format: t('gantt.weekFormat') }
      ];
    case 'month':
      return [
        { unit: 'year', step: 1, format: t('gantt.yearFormat') },
        { unit: 'month', step: 1, format: t('gantt.monthFormat') }
      ];
    default:
      return [
        { unit: 'month', step: 1, format: t('gantt.yearMonthFormat') },
        { unit: 'day', step: 1, format: t('gantt.dayFormat') }
      ];
  }
};

const updateScale = () => {
  if (!ganttContainer.value || !isInitialized.value) return;

  // 更新刻度配置
  gantt.config.scales = getScaleConfig();
  gantt.config.scale_unit = props.scale;

  // 重新加载数据
  const ganttTasks = convertToGanttTasks();
  gantt.clearAll();
  gantt.parse({ data: ganttTasks });

  // 强制重绘时间轴
  gantt.render();
};

const initGantt = () => {
  if (!ganttContainer.value) return;

  gantt.plugins({
    auto_scheduling: true,
    tooltip: true
  });

  gantt.config.date_format = '%Y-%m-%d';
  gantt.config.scale_unit = props.scale;
  gantt.config.duration_unit = 'day'; // duration 始终以天为单位

  gantt.config.auto_scheduling = true;
  gantt.config.auto_scheduling_strict = true;
  gantt.config.auto_scheduling_compatibility = true;

  // 禁用连线功能
  gantt.config.links = false;

  // 设置为只读模式 - 禁用所有编辑功能
  gantt.config.readonly = true;
  gantt.config.drag_resize = false;  // 禁用调整大小
  gantt.config.drag_move = false;    // 禁用拖动
  gantt.config.drag_progress = false; // 禁用调整进度
  gantt.config.drag_links = false;   // 禁用拖动连线

  // 禁用双击编辑
  gantt.config.details_on_create = false;
  gantt.config.details_on_dblclick = false;

  gantt.config.columns = [
    {
      name: 'text',
      label: t('gantt.taskName'),
      tree: true,
      width: '*',
      resize: true
    },
    {
      name: 'start_date',
      label: t('gantt.startDate'),
      align: 'center',
      width: 100
    },
    {
      name: 'duration',
      label: t('gantt.duration'),
      align: 'center',
      width: 60,
      template: (task: any) => task.workingDays || task.duration
    }
    // 移除了 "add" 列，隐藏添加按钮
  ];

  gantt.config.grid_width = 450;
  gantt.config.task_height = 28;
  gantt.config.row_height = 45;
  gantt.config.fit_tasks = true;
  gantt.config.auto_finish = true;

  gantt.config.scales = getScaleConfig();

  gantt.templates.task_class = (_start: Date, _end: Date, task: any) => {
    const ganttTask = task as any;
    let classes = ['gantt_task'];

    if (ganttTask.status) {
      classes.push(`gantt_task_${ganttTask.status}`);
    }

    // 添加延期样式类
    if (ganttTask.isDelayed || (ganttTask.delayedDays || 0) > 0) {
      const days = ganttTask.delayedDays || 0;
      if (days >= 7) classes.push('gantt_task_delayed_critical');
      else if (days >= 3) classes.push('gantt_task_delayed_warning');
      else classes.push('gantt_task_delayed');
    }

    return classes.join(' ');
  };

  // 设置任务条文本颜色为白色并显示进度百分比
  gantt.templates.task_text = (_start: Date, _end: Date, task: any) => {
    const actualPct = Math.round(task.progress * 100);
    const ganttTask = task as GanttTask;
    // 计算预期进度（仅进行中/待办任务有意义）
    let expectedPct = -1;
    if (ganttTask.start_date && ganttTask.duration && ganttTask.status !== 'done') {
      const today = new Date();
      const start = new Date(ganttTask.start_date);
      const end = gantt.date.add(start, ganttTask.duration - 1, 'day');
      const total = end.getTime() - start.getTime();
      if (total > 0) {
        const elapsed = today.getTime() - start.getTime();
        expectedPct = Math.max(0, Math.min(100, Math.round((elapsed / total) * 100)));
      }
    }
    if (expectedPct >= 0) {
      const diff = actualPct - expectedPct;
      const diffColor = diff >= 0 ? '#4ade80' : '#f87171';
      const diffSign = diff >= 0 ? '+' : '';
      return `<div style="color: #ffffff; display: flex; justify-content: center; align-items: center; gap: 8px; width: 100%; padding: 0 8px; font-size: 11px;">
        <span style="font-weight: bold;">${actualPct}%</span>
        <span style="color: ${diffColor}; font-size: 10px;">${diffSign}${diff}%</span>
        <span>${task.text}</span>
      </div>`;
    }
    return `<div style="color: #ffffff; display: flex; justify-content: center; align-items: center; gap: 12px; width: 100%; padding: 0 8px;">
      <span style="font-weight: bold;">${actualPct}%</span>
      <span>${task.text}</span>
    </div>`;
  };

  // 自定义任务工具提示
  gantt.templates.tooltip_text = function(start: Date, end: Date, task: any) {
    const ganttTask = task as any;

    // 获取负责人信息
    let assigneeInfo = '';
    if (ganttTask.assigneeId) {
      const assignee = userStore.users.find(u => u.id === ganttTask.assigneeId);
      if (assignee) {
        assigneeInfo = `<b>${t('gantt.tooltip.assignee')}：</b>${assignee.name}<br/>`;
      }
    }

    // 状态映射
    const statusMap: Record<string, string> = {
      'todo': t('gantt.statusTodo'),
      'in-progress': t('gantt.statusInProgress'),
      'done': t('gantt.statusDone')
    };

    // 优先级映射
    const priorityMap: Record<string, string> = {
      'low': t('priorities.low'),
      'medium': t('priorities.medium'),
      'high': t('priorities.high'),
      'urgent': t('priorities.urgent')
    };

    // 格式化日期
    const formatDate = (date: Date) => {
      return gantt.date.date_to_str('%Y-%m-%d')(date);
    };

    // 构建工具提示HTML
    let tooltip = `
      <div style="min-width: 250px; line-height: 1.6;">
        <h4 style="margin: 0 0 8px 0; color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 4px;">
          ${ganttTask.text}
        </h4>
    `;

    // 描述（如果有）
    if (ganttTask.description) {
      const shortDesc = ganttTask.description.length > 50
        ? ganttTask.description.substring(0, 50) + '...'
        : ganttTask.description;
      tooltip += `<div style="margin-bottom: 6px; color: #7f8c8d;"><i>${shortDesc}</i></div>`;
    }

    // 基本信息
    tooltip += `
      <div style="margin-bottom: 4px;">
        <span style="color: #95a5a6;">${t('gantt.tooltip.status')}：</span>
        <span style="color: ${getStatusColor(ganttTask.status)}; font-weight: bold;">
          ${statusMap[ganttTask.status] || ganttTask.status}
        </span>
        <span style="margin-left: 10px; color: #95a5a6;">${t('gantt.tooltip.priority')}：</span>
        <span style="color: ${getPriorityColor(ganttTask.priority)}; font-weight: bold;">
          ${priorityMap[ganttTask.priority] || ganttTask.priority}
        </span>
      </div>
    `;

    // 时间信息
    const plannedEndDate = gantt.date.add(start, ganttTask.duration - 1, 'day');
    tooltip += `
      <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
        <div style="color: #95a5a6;">📋 ${t('gantt.tooltip.startTime')}：${formatDate(start)}</div>
        <div style="color: #95a5a6;">📋 ${t('gantt.tooltip.endTime')}：${formatDate(plannedEndDate)}</div>
        <div style="color: #95a5a6;">${t('gantt.tooltip.duration')}：${ganttTask.workingDays || ganttTask.duration} ${t('gantt.tooltip.days')}</div>
      </div>
    `;

    // 实际日期（如果有且与计划不同）
    if (ganttTask.actualStartDate || ganttTask.actualEndDate) {
      const plannedEndStr = formatDate(plannedEndDate);
      const startDiff = ganttTask.actualStartDate && ganttTask.actualStartDate !== formatDate(start);
      const endDiff = ganttTask.actualEndDate && ganttTask.actualEndDate !== plannedEndStr;
      if (startDiff || endDiff) {
        const actualStartDisplay = ganttTask.actualStartDate || formatDate(start);
        const actualEndDisplay = ganttTask.actualEndDate || plannedEndStr;
        const isLate = ganttTask.actualEndDate && ganttTask.actualEndDate > plannedEndStr;
        tooltip += `
          <div style="margin-top: 4px; padding-top: 4px; border-top: 1px dashed #bdc3c7;">
            <div style="color: #10b981;">🟢 实际开始：${actualStartDisplay}</div>
            <div style="color: ${isLate ? '#ef4444' : '#10b981'};">${isLate ? '🔴' : '🟢'} 实际结束：${actualEndDisplay}</div>
          </div>
        `;
      }
    }

    // 工时信息（如果有）
    if (ganttTask.estimatedHours || ganttTask.actualHours) {
      tooltip += `<div style="margin-top: 4px;">`;
      if (ganttTask.estimatedHours) {
        tooltip += `<span style="color: #95a5a6;">${t('gantt.tooltip.estimated')}：${ganttTask.estimatedHours}h</span>`;
      }
      if (ganttTask.actualHours) {
        tooltip += `<span style="margin-left: 10px; color: #95a5a6;">${t('gantt.tooltip.actual')}：${ganttTask.actualHours}h</span>`;
      }
      tooltip += `</div>`;
    }

    // 延期信息（如果有）- 匹配任务看板逻辑
    const hasSubtasks = ganttTask.hasSubtasks;
    const delayedDays = ganttTask.delayedDays || 0;
    const childrenDelayedCount = ganttTask.childrenDelayedCount || 0;
    const childrenTotalDelayedDays = ganttTask.childrenTotalDelayedDays || 0;
    
    // 父任务：显示子任务延期信息
    if (hasSubtasks && childrenDelayedCount > 0) {
      let delayColor = '#3b82f6';
      if (childrenTotalDelayedDays >= 7) delayColor = '#ef4444';
      else if (childrenTotalDelayedDays >= 3) delayColor = '#f59e0b';
      
      let delayLabel = '延期';
      if (childrenTotalDelayedDays >= 7) delayLabel = '严重延期';
      else if (childrenTotalDelayedDays >= 3) delayLabel = '已延期';
      
      tooltip += `
        <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
          <span style="color: ${delayColor}; font-weight: bold;">⚠ 子任务延期：${childrenDelayedCount} 个任务</span>
          <span style="color: ${delayColor}; font-weight: bold; margin-left: 8px;">累计 +${childrenTotalDelayedDays} 天</span>
        </div>
      `;
    }
    // 无子任务的任务：显示自己的延期信息
    else if (ganttTask.isDelayed || delayedDays > 0) {
      let delayColor = '#3b82f6';
      if (delayedDays >= 7) delayColor = '#ef4444';
      else if (delayedDays >= 3) delayColor = '#f59e0b';
      
      let delayLabel = '延期';
      if (delayedDays >= 7) delayLabel = '严重延期';
      else if (delayedDays >= 3) delayLabel = '已延期';
      
      tooltip += `
        <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
          <span style="color: ${delayColor}; font-weight: bold;">⚠ ${delayLabel} ${delayedDays} 天</span>
        </div>
      `;
    }

    // 负责人信息（如果有）
    if (assigneeInfo) {
      tooltip += `<div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">${assigneeInfo}</div>`;
    }

    // 进度信息（预期 vs 实际）
    const actualPct = Math.round(ganttTask.progress * 100);
    let expectedPctStr = '';
    if (ganttTask.start_date && ganttTask.duration && ganttTask.status !== 'done') {
      const s = new Date(ganttTask.start_date);
      const e = gantt.date.add(s, ganttTask.duration - 1, 'day');
      const total = e.getTime() - s.getTime();
      if (total > 0) {
        const elapsed = new Date().getTime() - s.getTime();
        const expectedPct = Math.max(0, Math.min(100, Math.round((elapsed / total) * 100)));
        const diff = actualPct - expectedPct;
        const diffColor = diff >= 0 ? '#10b981' : '#ef4444';
        const diffSign = diff >= 0 ? '+' : '';
        expectedPctStr = `
          <div style="margin-top: 4px; display: flex; align-items: center; gap: 8px;">
            <div style="flex: 1; height: 6px; background: #ecf0f1; border-radius: 3px; position: relative;">
              <div style="width: ${actualPct}%; height: 100%; background: #3498db; border-radius: 3px; position: absolute; left: 0;"></div>
              <div style="position: absolute; left: ${expectedPct}%; top: -2px; height: 10px; width: 2px; background: ${diffColor}; border-radius: 1px; z-index: 2;"></div>
            </div>
          </div>
          <div style="display: flex; justify-content: space-between; margin-top: 2px; font-size: 11px;">
            <span>实际 ${actualPct}%</span>
            <span style="color: ${diffColor};">▼ 预期 ${expectedPct}% &nbsp; ${diffSign}${diff}%</span>
          </div>
        `;
      }
    }
    if (!expectedPctStr) {
      expectedPctStr = `
        <div style="color: #95a5a6;">${t('gantt.tooltip.progress')}：${actualPct}%</div>
        <div style="width: 100%; background: #ecf0f1; height: 6px; border-radius: 3px; margin-top: 4px;">
          <div style="width: ${actualPct}%; background: #3498db; height: 100%; border-radius: 3px;"></div>
        </div>
      `;
    }
    tooltip += `
      <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
        ${expectedPctStr}
      </div>
    `;

    tooltip += `</div>`;
    return tooltip;
  };

  // 高亮周末
  gantt.templates.timeline_cell_class = (_task: any, date: Date) => {
    const dayOfWeek = date.getDay();
    // 0 = 周日, 6 = 周六
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      return 'weekend';
    }
    return '';
  };

  gantt.init(ganttContainer.value);

  const ganttTasks = convertToGanttTasks();
  // 不传入 links，移除任务条之间的连线
  gantt.parse({ data: ganttTasks });

  // 绘制实际日期覆盖层（计划 vs 实际对比）
  // addTaskLayer 为 Pro 版功能，GPL 版无此 API，通过特性检测避免报错
  if (typeof gantt.addTaskLayer === 'function') {
    gantt.addTaskLayer((task: any) => {
    const ganttTask = task as GanttTask;
    if (!ganttTask.actualStartDate || !ganttTask.actualEndDate) return false;

    const plannedStart = new Date(ganttTask.start_date);
    const plannedEnd = gantt.date.add(plannedStart, ganttTask.duration - 1, 'day');

    const actualStart = new Date(ganttTask.actualStartDate);
    const actualEnd = new Date(ganttTask.actualEndDate);

    // 只有实际日期和计划日期不同时才显示覆盖条
    const startDiff = Math.abs(actualStart.getTime() - plannedStart.getTime());
    const endDiff = Math.abs(actualEnd.getTime() - plannedEnd.getTime());
    if (startDiff < 86400000 && endDiff < 86400000) return false;

    const taskY = task.y + task.height * 0.6;
    const barH = task.height * 0.35;

    const xStart = gantt.posFromDate(actualStart);
    const xEnd = gantt.posFromDate(gantt.date.add(actualEnd, 1, 'day'));

    const isEarly = actualEnd < plannedEnd;
    const fillColor = isEarly ? '#10b981' : '#ef4444';

    const el = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    el.setAttribute('x', String(xStart));
    el.setAttribute('y', String(taskY));
    el.setAttribute('width', String(Math.max(xEnd - xStart, 2)));
    el.setAttribute('height', String(barH));
    el.setAttribute('rx', '3');
    el.setAttribute('fill', fillColor);
    el.setAttribute('fill-opacity', '0.5');
    el.setAttribute('stroke', fillColor);
    el.setAttribute('stroke-width', '1');
    el.setAttribute('stroke-opacity', '0.8');
    el.setAttribute('class', 'gantt-actual-bar');
    return el;
  });

    // 绘制预期进度标记（计划位置 vs 实际进度）
    gantt.addTaskLayer((task: any) => {
      const ganttTask = task as GanttTask;
      // 已完成的任务不需要标记
      if (ganttTask.status === 'done') return false;
      if (!ganttTask.start_date || !ganttTask.duration) return false;

      const today = new Date();
      const start = new Date(ganttTask.start_date);
      const end = gantt.date.add(start, ganttTask.duration - 1, 'day');

      // 今天不在任务区间内，不画标记
      if (today < start || today > end) return false;

      const total = end.getTime() - start.getTime();
      if (total <= 0) return false;

      // 预期进度位置（今天在任务时间轴上的百分比位置）
      const expectedRatio = (today.getTime() - start.getTime()) / total;
      const barWidth = task.width || gantt.posFromDate(end) - gantt.posFromDate(start);
      const markerX = gantt.posFromDate(start) + barWidth * expectedRatio;

      // 实际进度 vs 预期进度
      const actualPct = ganttTask.progress || 0;
      const expectedPct = Math.round(expectedRatio * 100);
      const isAhead = actualPct >= expectedPct / 100;

      // 标记位于任务条顶部
      const markerTop = task.y;
      const markerHeight = task.height;

      // 绘制垂直标线 + 顶部菱形
      const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');

      // 细线贯穿任务条
      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', String(markerX));
      line.setAttribute('y1', String(markerTop));
      line.setAttribute('x2', String(markerX));
      line.setAttribute('y2', String(markerTop + markerHeight));
      line.setAttribute('stroke', isAhead ? '#22c55e' : '#ef4444');
      line.setAttribute('stroke-width', '2');
      line.setAttribute('stroke-dasharray', '3,2');
      line.setAttribute('opacity', '0.85');
      group.appendChild(line);

      // 顶部小菱形标记
      const diamondSize = 4;
      const diamond = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
      const cx = markerX;
      const cy = markerTop - 1;
      const points = [
        `${cx},${cy - diamondSize}`,
        `${cx + diamondSize},${cy}`,
        `${cx},${cy + diamondSize}`,
        `${cx - diamondSize},${cy}`
      ].join(' ');
      diamond.setAttribute('points', points);
      diamond.setAttribute('fill', isAhead ? '#22c55e' : '#ef4444');
      diamond.setAttribute('stroke', '#ffffff');
      diamond.setAttribute('stroke-width', '1');
      group.appendChild(diamond);

      group.setAttribute('class', 'gantt-expected-progress');
      return group;
    });
  }

  // 只读模式下不需要这些事件监听器，已注释掉
  // gantt.attachEvent('onAfterTaskUpdate', (id, item) => {
  //   const task = item as any;
  //   updateTaskInStore(id as string, {
  //     title: task.text,
  //     startDate: task.start_date,
  //     endDate: gantt.date.add(task.start_date, task.duration, 'day'),
  //     progress: Math.round(task.progress * 100),
  //     priority: task.priority,
  //     status: task.status
  //   });
  // });

  // gantt.attachEvent('onAfterTaskDrag', (id, mode, task) => {
  //   const ganttTask = task as any;
  //   updateTaskInStore(id as string, {
  //     startDate: ganttTask.start_date,
  //     endDate: gantt.date.add(ganttTask.start_date, ganttTask.duration, 'day')
  //   });
  //   gantt.autoSchedule();
  // });

  // // 处理新建任务，设置默认优先级并保存到 store
  // gantt.attachEvent('onAfterTaskAdd', (id, item) => {
  //   const task = item as any;
  //   const updates: any = {
  //     title: task.text || '新任务',
  //     startDate: task.start_date,
  //     endDate: gantt.date.add(task.start_date, task.duration, 'day'),
  //     progress: Math.round((task.progress || 0) * 100),
  //     priority: task.priority || 'medium',
  //     status: task.status || 'todo'
  //   };
  //   if (!task.priority) {
  //     gantt.updateTask(id, { priority: 'medium' });
  //   }
  //   if (!task.status) {
  //     gantt.updateTask(id, { status: 'todo' });
  //   }
  //   updateTaskInStore(id as string, updates);
  // });

  isInitialized.value = true;
};

const refreshGantt = () => {
  if (!ganttContainer.value || !gantt.getTaskCount()) return;

  const ganttTasks = convertToGanttTasks();
  gantt.clearAll();
  gantt.parse({ data: ganttTasks });
};

onMounted(() => {
  gantt.config.locale = {
    date: {
      month_full: t('gantt.months'),
      month_short: t('gantt.months'),
      day_full: t('gantt.days'),
      day_short: t('gantt.daysShort')
    },
    labels: {
      new_task: t('gantt.locale.newTask'),
      dhx_cal_today_button: t('gantt.locale.todayButton'),
      day_tab: t('gantt.day'),
      week_tab: t('gantt.week'),
      month_tab: t('gantt.month'),
      new_event: t('gantt.locale.newTask'),
      icon_save: t('common.save'),
      icon_cancel: t('common.cancel'),
      icon_details: t('common.description'),
      icon_edit: t('common.edit'),
      icon_delete: t('common.delete'),
      confirm_closing: '',
      confirm_deleting: t('gantt.locale.confirmDeleting'),
      section_description: t('gantt.locale.description'),
      section_time: t('gantt.locale.timePeriod'),
      section_type: t('gantt.locale.type'),
      column_text: t('gantt.taskName'),
      column_start_date: t('gantt.startDate'),
      column_duration: t('gantt.duration'),
      column_add: ''
    }
  };

  initGantt();
});

watch(() => [props.tasks, props.project], () => {
  if (isInitialized.value) {
    refreshGantt();
  }
}, { deep: true });

watch(() => props.scale, () => {
  updateScale();
});

onBeforeUnmount(() => {
  if (ganttContainer.value) {
    gantt.clearAll();
  }
});
</script>

<style>
.gantt_task_line {
  border-radius: 4px;
}

/* 待办状态 - 灰色 */
.gantt_task_todo {
  background-color: #95a5a6 !important;
  border-color: #7f8c8d !important;
}

/* 进行中状态 - 蓝色 */
.gantt_task_in-progress {
  background-color: #3498db !important;
  border-color: #2980b9 !important;
}

/* 已完成状态 - 绿色 */
.gantt_task_done {
  background-color: #27ae60 !important;
  border-color: #229954 !important;
}

.gantt_task_progress {
  background-color: rgba(255, 255, 255, 0.3);
}

.gantt_task_content,
.gantt_side_content,
.gantt_task_line .gantt_task_content {
  color: #ffffff !important;
}

.gantt_container {
  border: 1px solid #C4D4B7;
}

.gantt_task_row {
  border-bottom: 1px solid #E8F0E0;
}

.gantt_scale_line {
  border-bottom: 1px solid #C4D4B7;
}

.gantt_side_gantt_task,
.gantt_scale_cell {
  border-right: 1px solid #E8F0E0;
}

.gantt_grid_head_cell {
  border-right: 1px solid #C4D4B7;
  border-bottom: 1px solid #C4D4B7;
}

.weekend {
  background-color: #F5F5DC !important;
}

.gantt_scale_cell.weekend,
.gantt_task_cell.weekend {
  background-color: #F5F5DC !important;
}

/* 工具提示样式优化 */
.gantt_tooltip {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  border-radius: 6px !important;
  border: 1px solid #bdc3c7 !important;
  max-width: 350px !important;
  font-size: 13px !important;
  background: white !important;
  padding: 12px !important;
  line-height: 1.5 !important;
  z-index: 10000 !important;
}

.gantt_tooltip h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: bold;
}

/* 延期任务样式 */
.gantt_task_delayed {
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);
}

.gantt_task_delayed_warning {
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.5);
}

.gantt_task_delayed_critical {
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.6);
  animation: pulse-red 2s infinite;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}
</style>
