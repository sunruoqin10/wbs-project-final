<template>
  <div class="gantt-wrapper">
    <div ref="ganttContainer" class="w-full" style="height: 500px;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue';
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
  // 基于状态设置颜色
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
        { unit: 'month', step: 1, format: '%Y年 %m月' },
        { unit: 'day', step: 1, format: '%d' }
      ];
    case 'week':
      return [
        { unit: 'month', step: 1, format: '%Y年 %m月' },
        { unit: 'week', step: 1, format: '第%W周' }
      ];
    case 'month':
      return [
        { unit: 'year', step: 1, format: '%Y年' },
        { unit: 'month', step: 1, format: '%m月' }
      ];
    default:
      return [
        { unit: 'month', step: 1, format: '%Y年 %m月' },
        { unit: 'day', step: 1, format: '%d' }
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
      label: '任务名称',
      tree: true,
      width: '*',
      resize: true
    },
    {
      name: 'start_date',
      label: '开始时间',
      align: 'center',
      width: 100
    },
    {
      name: 'duration',
      label: '工期',
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
    return ganttTask.status ? `gantt_task_${ganttTask.status}` : 'gantt_task_todo';
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
        assigneeInfo = `<b>负责人：</b>${assignee.name}<br/>`;
      }
    }

    // 状态映射
    const statusMap: Record<string, string> = {
      'todo': '待办',
      'in-progress': '进行中',
      'done': '已完成'
    };

    // 优先级映射
    const priorityMap: Record<string, string> = {
      'low': '低',
      'medium': '中',
      'high': '高',
      'urgent': '紧急'
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
        <span style="color: #95a5a6;">状态：</span>
        <span style="color: ${getStatusColor(ganttTask.status)}; font-weight: bold;">
          ${statusMap[ganttTask.status] || ganttTask.status}
        </span>
        <span style="margin-left: 10px; color: #95a5a6;">优先级：</span>
        <span style="color: ${getPriorityColor(ganttTask.priority)}; font-weight: bold;">
          ${priorityMap[ganttTask.priority] || ganttTask.priority}
        </span>
      </div>
    `;

    // 时间信息
    tooltip += `
      <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #bdc3c7;">
        <div style="color: #95a5a6;">开始：${formatDate(start)}</div>
        <div style="color: #95a5a6;">结束：${formatDate(end)}</div>
        <div style="color: #95a5a6;">工期：${ganttTask.duration} 天</div>
      </div>
    `;

    // 工时信息（如果有）
    if (ganttTask.estimatedHours || ganttTask.actualHours) {
      tooltip += `<div style="margin-top: 4px;">`;
      if (ganttTask.estimatedHours) {
        tooltip += `<span style="color: #95a5a6;">预估：${ganttTask.estimatedHours}h</span>`;
      }
      if (ganttTask.actualHours) {
        tooltip += `<span style="margin-left: 10px; color: #95a5a6;">实际：${ganttTask.actualHours}h</span>`;
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
        <div style="color: #95a5a6;">进度：${Math.round(ganttTask.progress * 100)}%</div>
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
      month_full: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
      month_short: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
      day_full: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
      day_short: ['日', '一', '二', '三', '四', '五', '六']
    },
    labels: {
      new_task: '新任务',
      dhx_cal_today_button: '今天',
      day_tab: '日',
      week_tab: '周',
      month_tab: '月',
      new_event: '新事件',
      icon_save: '保存',
      icon_cancel: '关闭',
      icon_details: '详情',
      icon_edit: '编辑',
      icon_delete: '删除',
      confirm_closing: '',
      confirm_deleting: '确定删除任务?',
      section_description: '描述',
      section_time: '时间周期',
      section_type: '类型',
      column_text: '任务名',
      column_start_date: '开始时间',
      column_duration: '工期',
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
</style>
