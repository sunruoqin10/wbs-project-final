<template>
  <div
    class="group rounded-lg bg-white p-4 shadow-sm transition-all duration-200 hover:shadow-md cursor-pointer"
    :class="[borderClass, { 'has-subtasks': hasSubtasks, 'is-expanded': isExpanded }]"
    @click="handleClick"
  >
    <!-- Header -->
    <div class="mb-2 flex items-start justify-between">
      <h4 class="font-medium text-secondary-900 line-clamp-2">{{ task.title }}</h4>
      <div class="flex items-center gap-2">
        <!-- 展开/折叠按钮（仅父任务显示） -->
        <button
          v-if="hasSubtasks"
          @click.stop="$emit('toggle-expand')"
          class="expand-collapse-btn flex items-center justify-center rounded-md border border-secondary-200 bg-secondary-50 text-secondary-600 transition-all duration-200 hover:bg-primary-50 hover:border-primary-200 hover:text-primary-600"
          :aria-label="isExpanded ? '收起子任务' : '展开子任务'"
          :title="isExpanded ? '收起子任务' : '展开子任务'"
        >
          <svg
            class="h-4 w-4 transition-transform duration-200"
            :class="{ 'rotate-180': isExpanded }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
          <span
            v-if="summary"
            class="ml-1 text-xs font-medium"
          >
            {{ summary.total }}
          </span>
        </button>
        <button
          @click.stop="showMenu = !showMenu"
          class="opacity-0 group-hover:opacity-100 transition-opacity rounded p-1 hover:bg-secondary-100"
        >
          <svg class="h-4 w-4 text-secondary-500" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Description -->
    <p v-if="task.description" class="mb-3 text-sm text-secondary-600 line-clamp-2">
      {{ task.description }}
    </p>

    <!-- Tags -->
    <div v-if="task.tags && task.tags.length > 0" class="mb-3 flex flex-wrap gap-1">
      <span
        v-for="tag in task.tags.slice(0, 3)"
        :key="tag"
        class="rounded bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
      >
        {{ tag }}
      </span>
    </div>

    <!-- Footer -->
    <div class="flex items-center justify-between">
      <!-- Priority Badge -->
      <Badge :variant="priorityVariant" size="sm">
        {{ priorityLabel }}
      </Badge>

      <!-- Assignee -->
      <div v-if="assignee" class="flex items-center gap-2">
        <img
          :src="assignee.avatar"
          :alt="assignee.name"
          class="h-6 w-6 rounded-full border-2 border-white"
          :title="assignee.name"
        />
        <span class="text-xs text-secondary-600">{{ assignee.name }}</span>
      </div>
    </div>

    <!-- Progress (仅无子任务时显示任务自身的进度) -->
    <div v-if="displayProgress >= 0 && !hasSubtasks" class="mt-3">
      <ProgressBar
        :value="displayProgress"
        :show-label="true"
        color="primary"
        size="sm"
      />
    </div>

    <!-- Estimated Hours -->
    <div v-if="totalEstimatedHours > 0" class="mt-2 flex items-center text-xs text-secondary-600">
      <svg class="mr-1 h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <span>
        {{ hasSubtasks ? '总预估工时' : '预估工时' }}: {{ formatHoursToDays(totalEstimatedHours) }}
        <span v-if="totalActualHours > 0" class="text-secondary-500">/ 已用 {{ formatHoursToDays(totalActualHours) }}</span>
      </span>
    </div>

    <!-- Due Date -->
    <div v-if="task.startDate || task.endDate" class="mt-2 text-xs" :class="dueDateClass">
      <svg class="mr-1 inline h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
      {{ formattedStartDate }} ~ {{ formattedEndDate }}
    </div>

    <!-- 延期标识（仅无子任务的任务显示） -->
    <div v-if="!hasSubtasks && (task.isDelayed || (task.delayedDays || 0) > 0)" class="mt-2 flex items-center gap-2">
      <Badge :variant="delaySeverity" size="sm">{{ delayLabel }}</Badge>
      <span v-if="task.delayedDays && task.delayedDays > 0" class="text-xs" :class="delayTextClass">
        延期 {{ task.delayedDays }} 天
      </span>
    </div>

    <!-- 子任务延期累计（仅父任务显示） -->
    <div v-if="hasSubtasks && (task.childrenDelayedCount || 0) > 0" class="mt-2 flex items-center gap-2 text-xs">
      <span class="text-secondary-600">子任务延期：</span>
      <span :class="childrenDelayClass">
        {{ task.childrenDelayedCount }} 个任务
      </span>
      <span v-if="task.childrenTotalDelayedDays && task.childrenTotalDelayedDays > 0" :class="childrenDelayClass">
        累计 +{{ task.childrenTotalDelayedDays }} 天
      </span>
    </div>

    <!-- 子任务摘要（仅父任务显示） -->
    <div v-if="hasSubtasks && summary" class="subtask-summary">
      <div class="summary-header">
        <span class="summary-text">
          {{ summary.totalDescendants }} 个任务 ({{ summary.total }} 个直接子任务)
        </span>
        <span class="percentage-text">{{ summary.aggregatedProgress }}%</span>
      </div>

      <ProgressBar
        :value="summary.aggregatedProgress"
        :show-label="false"
        color="warning"
        size="sm"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import type { Task, SubtaskSummary } from '@/types';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import dayjs from 'dayjs';

interface Props {
  task: Task;
  summary?: SubtaskSummary;
  isExpanded?: boolean;
}

interface Emits {
  (e: 'click', task: Task): void;
  (e: 'toggle-expand'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();
const userStore = useUserStore();
const taskStore = useTaskStore();

// 计算显示的进度（待办状态强制为0，已完成状态强制为100）
const displayProgress = computed(() => {
  if (props.task.status === 'todo') {
    return 0;
  }
  if (props.task.status === 'done') {
    return 100;
  }
  return props.task.progress;
});

// 获取直接子任务列表
const directSubtasks = computed(() => {
  return taskStore.getSubtasks(props.task.id);
});

// 判断是否有子任务
const hasSubtasks = computed(() => {
  if (props.summary) {
    const result = props.summary.total > 0;
    // 调试输出
    if (result) {
      console.log(`任务 [${props.task.title}] 子任务延期信息:`, {
        childrenDelayedCount: props.task.childrenDelayedCount,
        childrenTotalDelayedDays: props.task.childrenTotalDelayedDays,
        hasSubtasks: result
      });
    }
    return result;
  }
  // 如果没有 summary，直接查询（向后兼容）
  return directSubtasks.value.length > 0;
});

const showMenu = computed(() => false);

const assignee = computed(() => {
  return props.task.assigneeId ? userStore.userById(props.task.assigneeId) : null;
});

const priorityLabel = computed(() => {
  const labels: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    urgent: '紧急'
  };
  return labels[props.task.priority] || props.task.priority;
});

const priorityVariant = computed(() => {
  const variants: Record<string, 'default' | 'info' | 'warning' | 'danger'> = {
    low: 'default',
    medium: 'info',
    high: 'warning',
    urgent: 'danger'
  };
  return variants[props.task.priority] || 'default';
});

const borderClass = computed(() => {
  const classes: Record<string, string> = {
    low: 'border-l-4 border-secondary-300',
    medium: 'border-l-4 border-info-400',
    high: 'border-l-4 border-warning-400',
    urgent: 'border-l-4 border-danger-500'
  };
  return classes[props.task.priority] || classes.medium;
});

const formattedStartDate = computed(() => {
  return dayjs(props.task.startDate).format('MM/DD');
});

const formattedEndDate = computed(() => {
  return dayjs(props.task.endDate).format('MM/DD');
});

const dueDateClass = computed(() => {
  if (!props.task.endDate) return 'text-secondary-500';

  const now = dayjs();
  const dueDate = dayjs(props.task.endDate);
  const diff = dueDate.diff(now, 'day');

  if (diff < 0) return 'text-danger-600 font-medium';
  if (diff <= 2) return 'text-warning-600 font-medium';
  return 'text-secondary-500';
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

// 子任务延期样式
const childrenDelayClass = computed(() => {
  const days = props.task.childrenTotalDelayedDays || 0;
  const count = props.task.childrenDelayedCount || 0;

  if (days >= 7 || count >= 3) return 'text-danger-600 font-medium';
  if (days >= 3 || count >= 1) return 'text-warning-600 font-medium';
  return 'text-info-600';
});

// 直接使用任务的工时值（数据库中已经维护了正确的值）
const totalEstimatedHours = computed(() => {
  return props.task.estimatedHours || 0;
});

const totalActualHours = computed(() => {
  return props.task.actualHours || 0;
});

// 将小时转换为天的显示格式（1天=8小时）
const formatHoursToDays = (hours: number): string => {
  if (hours <= 0) return '0 天';
  const days = hours / 8;
  if (days < 1) {
    return `${hours} 小时`;
  }
  const fullDays = Math.floor(days);
  const remainingHours = hours % 8;
  if (remainingHours > 0) {
    return `${fullDays} 天 ${remainingHours} 小时`;
  }
  return `${fullDays} 天`;
};

const handleClick = () => {
  emit('click', props.task);
};
</script>

<style scoped>
.expand-collapse-btn {
  flex-shrink: 0;
  width: auto;
  min-width: 36px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
  padding: 0 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.expand-collapse-btn:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.expand-collapse-btn svg {
  width: 14px;
  height: 14px;
}

/* 父任务卡片样式 */
.task-card.has-subtasks {
  border-left-width: 4px;
  padding-bottom: 16px;
  margin-bottom: 0;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.task-card.is-expanded {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

/* 子任务摘要 */
.subtask-summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.summary-text {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.percentage-text {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
}
</style>
