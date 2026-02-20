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
import type { Task as GanttTask } from 'dhtmlx-gantt';
import type { Task } from '@/types';
import type { Project } from '@/types';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';

interface Props {
  tasks: Task[];
  project: Project;
  scale: 'day' | 'week' | 'month';
}

const props = defineProps<Props>();
const { t } = useI18n();
const userStore = useUserStore();
const taskStore = useTaskStore();
const isInitialized = ref(false);

const ganttContainer = ref<HTMLElement>();

const convertToGanttTasks = (): GanttTask[] => {
  return props.tasks.map(task => ({
    id: task.id,
    text: task.title,
    start_date: task.startDate,
    duration: calculateDuration(task.startDate, task.endDate),
    progress: task.progress / 100,
    parent: task.parentTaskId || 0,
    priority: task.priority,
    assigneeId: task.assigneeId,
    color: getTaskColor(task),
    // 新增字段
    description: task.description,
    status: task.status,
    estimatedHours: task.estimatedHours,
    actualHours: task.actualHours
  }));
};

const calculateDuration = (start: string, end: string): number => {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const diffTime = Math.abs(endDate.getTime() - startDate.getTime());
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays || 1;
};

const getTaskColor = (task: Task): string => {
  // 优先显示延期状态
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
  gantt.config.scales = getScaleConfig();
  gantt.config.scale_unit = props.scale;
  gantt.config.duration_unit = props.scale;

  const ganttTasks = convertToGanttTasks();
  gantt.clearAll();
  gantt.parse({ data: ganttTasks });
};

const updateTaskInStore = (taskId: string, updates: Partial<Task>) => {
  taskStore.updateTask(taskId, updates);
};

const initGantt = () => {
  if (!ganttContainer.value) return;

  gantt.plugins({
    auto_scheduling: true,
    tooltip: true
  });

  gantt.config.date_format = '%Y-%m-%d';
  gantt.config.scale_unit = props.scale;
  gantt.config.duration_unit = props.scale;

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
      width: 60
    }
    // 移除了 "add" 列，隐藏添加按钮
  ];

  gantt.config.grid_width = 450;
  gantt.config.task_height = 28;
  gantt.config.row_height = 45;
  gantt.config.fit_tasks = true;
  gantt.config.auto_finish = true;

  gantt.config.scales = getScaleConfig();

  // Task template
  gantt.templates.task_class = (start, end, task) => {
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
  gantt.templates.task_text = (start, end, task) => {
    const progress = Math.round(task.progress * 100);
    return `<div style="color: #ffffff; display: flex; justify-content: center; align-items: center; gap: 12px; width: 100%; padding: 0 8px;">
      <span style="font-weight: bold;">${progress}%</span>
      <span>${task.text}</span>
    </div>`;
  };

  // 自定义任务工具提示
  gantt.templates.tooltip_text = function(start, end, task) {
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
    tooltip += `
      <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
        <div style="color: #95a5a6;">${t('gantt.tooltip.startTime')}：${formatDate(start)}</div>
        <div style="color: #95a5a6;">${t('gantt.tooltip.endTime')}：${formatDate(end)}</div>
        <div style="color: #95a5a6;">${t('gantt.tooltip.duration')}：${ganttTask.duration} ${t('gantt.tooltip.days')}</div>
      </div>
    `;

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

    // 负责人信息（如果有）
    if (assigneeInfo) {
      tooltip += `<div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">${assigneeInfo}</div>`;
    }

    // 进度信息
    tooltip += `
      <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
        <div style="color: #95a5a6;">${t('gantt.tooltip.progress')}：${Math.round(ganttTask.progress * 100)}%</div>
        <div style="width: 100%; background: #ecf0f1; height: 6px; border-radius: 3px; margin-top: 4px;">
          <div style="width: ${Math.round(ganttTask.progress * 100)}%; background: #3498db; height: 100%; border-radius: 3px;"></div>
        </div>
      </div>
    `;

    tooltip += `</div>`;
    return tooltip;
  };

  // 高亮周末
  gantt.templates.timeline_cell_class = (task, date) => {
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
