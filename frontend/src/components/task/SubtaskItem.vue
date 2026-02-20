<template>
  <div
    class="subtask-item"
    :class="{ 'has-children': hasChildren }"
    draggable="true"
    @click="handleClick"
  >
    <!-- 任务内容 -->
    <div class="subtask-content">
      <div class="subtask-header">
        <div class="flex items-center gap-2">
          <!-- 展开/折叠按钮 -->
          <button
            v-if="hasChildren"
            @click.stop="toggleExpand"
            class="expand-btn"
            :aria-label="isExpanded ? '收起子任务' : '展开子任务'"
          >
            {{ isExpanded ? '▼' : '▶' }}
          </button>
          <span v-else class="expand-placeholder"></span>

          <h5 class="task-title">{{ task.title }}</h5>
        </div>

        <Badge :variant="getStatusVariant(task.status)" size="sm">
          {{ getStatusLabel(task.status) }}
        </Badge>
        <Badge :variant="getPriorityVariant(task.priority)" size="sm">
          {{ getPriorityLabel(task.priority) }}
        </Badge>
      </div>

      <div class="subtask-meta">
        <ProgressBar :value="task.progress" :show-label="true" size="sm" />
        <span v-if="totalEstimatedHours > 0" class="meta-item estimated-hours">
          <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {{ hasChildren ? '总' : '' }}{{ totalEstimatedHours }}h
          <span v-if="totalActualHours > 0" class="text-secondary-500">/ {{ totalActualHours }}h</span>
        </span>
        <div v-if="task.assigneeId" class="meta-item assignee-info">
          <img
            :src="getAssigneeAvatar(task.assigneeId)"
            :alt="getAssigneeName(task.assigneeId)"
            class="h-5 w-5 rounded-full"
          />
          <span>{{ getAssigneeName(task.assigneeId) }}</span>
        </div>
        <span v-if="task.startDate || task.endDate" class="meta-item date-range" :class="dueDateClass">
          <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          {{ formatStartDate }} ~ {{ formatEndDate }}
        </span>

        <!-- 延期标识 -->
        <Badge v-if="task.isDelayed || (task.delayedDays || 0) > 0" :variant="delaySeverity" size="sm">
          {{ delayLabel }}
        </Badge>
        <span v-if="task.delayedDays && task.delayedDays > 0" class="meta-item" :class="delayTextClass">
          +{{ task.delayedDays }}天
        </span>
        <span v-if="hasChildren" class="meta-item child-count">
          → {{ getSubtasks(task.id).length }} 个子任务
        </span>
      </div>
    </div>

    <!-- 递归渲染子任务 -->
    <div v-if="isExpanded && hasChildren" class="nested-subtasks">
      <SubtaskItem
        v-for="child in getSubtasks(task.id)"
        :key="child.id"
        :task="child"
        @click="handleChildClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import type { Task } from '@/types';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import dayjs from 'dayjs';

interface Props {
  task: Task;
}

interface Emits {
  (e: 'click', task: Task): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();
const taskStore = useTaskStore();
const userStore = useUserStore();

// 获取直接子任务列表
const directSubtasks = computed(() => {
  return taskStore.getSubtasks(props.task.id);
});

const hasChildren = computed(() => {
  return directSubtasks.value.length > 0;
});

const isExpanded = computed({
  get: () => props.task.isExpanded || false,
  set: (value) => {
    // 更新展开状态（只更新本地，不调用API）
    const index = taskStore.tasks.findIndex(t => t && t.id === props.task.id);
    if (index !== -1 && taskStore.tasks[index]) {
      taskStore.tasks[index] = { ...taskStore.tasks[index]!, isExpanded: value };
    }
  }
});

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};

const handleClick = () => {
  emit('click', props.task);
};

const handleChildClick = (task: Task) => {
  emit('click', task);
};

const getSubtasks = (taskId: string) => {
  return taskStore.getSubtasks(taskId);
};

const getStatusLabel = (status: Task['status']) => {
  const labels: Record<string, string> = {
    'todo': '待办',
    'in-progress': '进行中',
    'done': '已完成'
  };
  return labels[status] || status;
};

const getStatusVariant = (status: Task['status']): 'default' | 'primary' | 'success' => {
  const variants: Record<string, 'default' | 'primary' | 'success'> = {
    'todo': 'default',
    'in-progress': 'primary',
    'done': 'success'
  };
  return variants[status] || 'default';
};

const getAssigneeName = (assigneeId: string) => {
  const user = userStore.userById(assigneeId);
  return user?.name || '';
};

const getAssigneeAvatar = (assigneeId: string) => {
  const user = userStore.userById(assigneeId);
  return user?.avatar || '';
};

const formatStartDate = computed(() => {
  if (!props.task.startDate) return '';
  return dayjs(props.task.startDate).format('MM/DD');
});

const formatEndDate = computed(() => {
  if (!props.task.endDate) return '';
  return dayjs(props.task.endDate).format('MM/DD');
});

const formatDate = (date: string) => {
  return dayjs(date).format('MM/DD');
};

const getPriorityLabel = (priority: Task['priority']) => {
  const labels: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    urgent: '紧急'
  };
  return labels[priority] || priority;
};

const getPriorityVariant = (priority: Task['priority']): 'default' | 'info' | 'warning' | 'danger' => {
  const variants: Record<string, 'default' | 'info' | 'warning' | 'danger'> = {
    low: 'default',
    medium: 'info',
    high: 'warning',
    urgent: 'danger'
  };
  return variants[priority] || 'default';
};

// 直接使用任务的工时值（数据库中已经维护了正确的值）
const totalEstimatedHours = computed(() => {
  return props.task.estimatedHours || 0;
});

const totalActualHours = computed(() => {
  return props.task.actualHours || 0;
});

// 日期样式（根据到期时间显示颜色）
const dueDateClass = computed(() => {
  if (!props.task.endDate) return '';

  const now = dayjs();
  const dueDate = dayjs(props.task.endDate);
  const diff = dueDate.diff(now, 'day');

  if (diff < 0) return 'text-danger-600 font-medium';
  if (diff <= 2) return 'text-warning-600 font-medium';
  return '';
});

// 延期相关计算属性
const delaySeverity = computed(() => {
  const days = props.task.delayedDays || 0;
  if (days >= 7) return 'danger';
  if (days >= 3) return 'warning';
  return 'info';
});

const delayLabel = computed(() => {
  const days = props.task.delayedDays || 0;
  if (days >= 7) return '严重延期';
  if (days >= 3) return '已延期';
  return '延期';
});

const delayTextClass = computed(() => {
  const days = props.task.delayedDays || 0;
  if (days >= 7) return 'text-danger-600';
  if (days >= 3) return 'text-warning-600';
  return 'text-info-600';
});
</script>

<style scoped>
.subtask-item {
  position: relative;
  padding: 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.subtask-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #3b82f6;
}

.subtask-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.subtask-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 8px;
}

.subtask-header .flex {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.expand-btn {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 10px;
  padding: 0;
  border-radius: 4px;
  transition: all 0.2s;
}

.expand-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.expand-placeholder {
  width: 20px;
  flex-shrink: 0;
}

.task-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.subtask-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #6b7280;
}

.meta-item {
  display: flex;
  align-items: center;
}

.assignee-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.assignee-info img {
  border: 1px solid #e5e7eb;
}

.assignee-info span {
  font-size: 12px;
  color: #6b7280;
}

.child-count {
  display: flex;
  align-items: center;
  color: #9ca3af;
  font-size: 11px;
}

/* 延期文本样式 */
.meta-item.text-danger-600 {
  color: #dc2626;
  font-weight: 500;
}

.meta-item.text-warning-600 {
  color: #d97706;
  font-weight: 500;
}

.meta-item.text-info-600 {
  color: #2563eb;
  font-weight: 500;
}

/* 日期范围样式 */
.meta-item.date-range.text-danger-600 {
  color: #dc2626;
  font-weight: 500;
}

.meta-item.date-range.text-warning-600 {
  color: #d97706;
  font-weight: 500;
}

.nested-subtasks {
  margin-top: 12px;
  margin-left: 16px;
  padding-left: 12px;
  border-left: 2px solid #e5e7eb;
}
</style>
