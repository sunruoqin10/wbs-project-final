<template>
  <div class="space-y-6">
    <!-- Title and Status -->
    <div>
      <div class="flex items-start justify-between gap-4">
        <h3 class="text-2xl font-bold text-secondary-900">{{ task.title }}</h3>
        <div class="flex items-center gap-2">
          <Badge :variant="statusVariant">{{ statusLabel }}</Badge>
          <div
            v-if="hasSubtasks"
            class="rounded-md bg-secondary-100 px-2 py-1 text-xs text-secondary-600"
          >
            {{ t('taskDetail.statusAutoCalculated') }}
          </div>
        </div>
      </div>

      <!-- Task Hierarchy Path (if has parent task) -->
      <div v-if="taskHierarchyPath.length > 1" class="mt-3 flex items-center gap-2 text-sm">
        <template v-for="(pathTask, index) in taskHierarchyPath" :key="pathTask.id">
          <!-- Separator -->
          <svg v-if="index > 0" class="h-4 w-4 text-secondary-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>

          <!-- Task link -->
          <div
            class="flex items-center gap-1"
            :class="index === taskHierarchyPath.length - 1 ? 'text-secondary-900 font-medium' : 'text-secondary-500 hover:text-primary-600 cursor-pointer'"
            @click="index === taskHierarchyPath.length - 1 ? null : handlePathTaskClick(pathTask)"
          >
            <span v-if="index === 0">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
            </span>
            <span :class="index === taskHierarchyPath.length - 1 ? 'font-medium' : ''">
              {{ pathTask.title }}
            </span>
          </div>
        </template>
      </div>
    </div>

    <!-- Description -->
    <div v-if="task.description">
      <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('taskDetail.description') }}</h4>
      <p class="text-secondary-600 whitespace-pre-wrap">{{ task.description }}</p>
    </div>

    <!-- Basic Info Grid -->
    <div class="grid grid-cols-2 gap-4">
      <!-- Assignee -->
      <div v-if="assignee">
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('common.owner') }}</h4>
        <div class="flex items-center gap-3">
          <img
            :src="assignee.avatar"
            :alt="assignee.name"
            class="h-10 w-10 rounded-full"
          />
          <div>
            <p class="font-medium text-secondary-900">{{ assignee.name }}</p>
            <p class="text-xs text-secondary-500">{{ assignee.department }}</p>
          </div>
        </div>
      </div>

      <!-- Priority -->
      <div>
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('common.priority') }}</h4>
        <Badge :variant="priorityVariant">{{ priorityLabel }}</Badge>
      </div>

      <!-- Start Date -->
      <div>
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('common.startDate') }}</h4>
        <div class="flex items-center gap-2 text-secondary-600">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span>{{ formattedStartDate }}</span>
        </div>
        <div
          v-if="hasSubtasks"
          class="mt-2 rounded-md bg-blue-50 px-3 py-2 text-xs text-blue-600"
        >
          {{ t('taskDetail.startDateAutoCalculated') }}
        </div>
      </div>

      <!-- End Date -->
      <div>
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('common.endDate') }}</h4>
        <div class="flex items-center gap-2 text-secondary-600">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span :class="dueDateClass">{{ formattedEndDate }}</span>
        </div>
        <div
          v-if="hasSubtasks"
          class="mt-2 rounded-md bg-blue-50 px-3 py-2 text-xs text-blue-600"
        >
          {{ t('taskDetail.endDateAutoCalculated') }}
        </div>
      </div>
    </div>

    <!-- Progress -->
    <div>
      <div class="mb-2 flex items-center justify-between">
        <h4 class="text-sm font-medium text-secondary-700">{{ t('common.progress') }}</h4>
        <span class="text-sm font-medium text-secondary-900">{{ displayProgress }}%</span>
      </div>
      <ProgressBar :value="displayProgress" />
      <div
        v-if="hasSubtasks"
        class="mt-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
      >
        {{ t('taskDetail.progressAutoCalculated') }}
      </div>
      <div
        v-if="task.status === 'done' && !hasSubtasks"
        class="mt-2 rounded-md bg-green-50 px-3 py-2 text-xs text-green-600"
      >
        {{ t('taskDetail.progressFixedWhenDone') }}
      </div>
    </div>

    <!-- Hours -->
    <div v-if="task.estimatedHours || task.actualHours" class="grid grid-cols-2 gap-4">
      <div v-if="task.estimatedHours">
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('taskDetail.estimatedHours') }}</h4>
        <p class="text-lg font-semibold text-secondary-900">{{ formatHoursToDays(task.estimatedHours) }}</p>
      </div>
      <div v-if="task.actualHours">
        <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('taskDetail.actualHours') }}</h4>
        <p class="text-lg font-semibold text-secondary-900">{{ formatHoursToDays(task.actualHours) }}</p>
      </div>
    </div>

    <!-- Tags -->
    <div v-if="task.tags && task.tags.length > 0">
      <h4 class="mb-2 text-sm font-medium text-secondary-700">{{ t('common.tags') }}</h4>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="tag in task.tags"
          :key="tag"
          class="rounded bg-secondary-100 px-3 py-1 text-sm text-secondary-700"
        >
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- Subtasks -->
    <div>
      <div class="mb-3 flex items-center justify-between">
        <h4 class="text-sm font-medium text-secondary-700">{{ t('taskDetail.subtasks') }} ({{ subtasks.length }})</h4>
        <Button
          variant="secondary"
          size="sm"
          @click="openCreateSubtaskModal"
        >
          <svg class="mr-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ t('taskDetail.addSubtask') }}
        </Button>
      </div>

      <div v-if="subtasks.length > 0" class="space-y-2">
        <div
          v-for="subtask in subtasks"
          :key="subtask.id"
          class="flex items-center justify-between rounded-lg border border-secondary-200 p-3 hover:bg-secondary-50 cursor-pointer"
          @click="handleSubtaskClick(subtask)"
        >
          <div class="flex items-center gap-3">
            <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
            <div>
              <p class="font-medium text-secondary-900">{{ subtask.title }}</p>
              <div class="mt-1 flex items-center gap-2 text-xs text-secondary-500">
                <span :class="getSubtaskStatusClass(subtask.status)">{{ getSubtaskStatusLabel(subtask.status) }}</span>
                <span v-if="subtask.assigneeId">• {{ getSubtaskAssigneeName(subtask.assigneeId) }}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-secondary-600">{{ subtask.progress }}%</span>
            <Badge :variant="getSubtaskPriorityVariant(subtask.priority)" size="sm">
              {{ getSubtaskPriorityLabel(subtask.priority) }}
            </Badge>
          </div>
        </div>
      </div>

      <div v-else class="rounded-lg border-2 border-dashed border-secondary-200 p-6 text-center text-sm text-secondary-500">
        {{ t('taskDetail.noSubtasks') }}
      </div>
    </div>

    <!-- Attachments -->
    <div v-if="task.attachments && task.attachments.length > 0">
      <h4 class="mb-3 text-sm font-medium text-secondary-700">{{ t('taskDetail.attachments') }} ({{ task.attachments.length }})</h4>
      <div class="space-y-2">
        <div
          v-for="attachment in task.attachments"
          :key="attachment.id"
          class="flex items-center justify-between rounded-lg border border-secondary-200 p-3"
        >
          <div class="flex items-center gap-3">
            <svg class="h-8 w-8 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
            </svg>
            <div>
              <p class="font-medium text-secondary-900">{{ attachment.name }}</p>
              <p class="text-xs text-secondary-500">{{ formatFileSize(attachment.size) }}</p>
            </div>
          </div>
          <a
            :href="attachment.url"
            target="_blank"
            rel="noopener noreferrer"
            class="text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            {{ t('taskDetail.download') }}
          </a>
        </div>
      </div>
    </div>

    <!-- Comments -->
    <div v-if="task.comments && task.comments.length > 0">
      <h4 class="mb-3 text-sm font-medium text-secondary-700">{{ t('taskDetail.comments') }} ({{ task.comments.length }})</h4>
      <div class="space-y-4">
        <div
          v-for="comment in task.comments"
          :key="comment.id"
          class="flex gap-3"
        >
          <img
            v-if="commentUser(comment)"
            :src="commentUser(comment)?.avatar"
            :alt="commentUser(comment)?.name"
            class="h-8 w-8 rounded-full"
          />
          <div class="flex-1 rounded-lg bg-secondary-50 p-3">
            <div class="mb-1 flex items-center justify-between">
              <span class="font-medium text-secondary-900">{{ commentUser(comment)?.name }}</span>
              <span class="text-xs text-secondary-500">{{ formattedCommentDate(comment.createdAt) }}</span>
            </div>
            <p class="text-sm text-secondary-600">{{ comment.content }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="flex gap-3 pt-4 border-t border-secondary-200">
      <Button variant="primary" @click="handleEdit">
        <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
        </svg>
        {{ t('taskDetail.editTask') }}
      </Button>
      <Button variant="danger" @click="handleDelete">
        <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        {{ t('taskDetail.deleteTask') }}
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import Button from '@/components/common/Button.vue';
import type { Task } from '@/types';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { currentLocale } from '@/i18n';
import dayjs from 'dayjs';

interface Props {
  task: Task;
}

interface Emits {
  (e: 'edit', task: Task): void;
  (e: 'delete', task: Task): void;
  (e: 'createSubtask', parentTaskId: string): void;
  (e: 'viewSubtask', subtask: Task): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();
const { t } = useI18n();
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

// 获取任务的层级路径（从根任务到当前任务）
const taskHierarchyPath = computed(() => {
  const path: Task[] = [];
  let currentTask: Task | undefined = props.task;

  // 递归向上查找父任务，直到找到根任务（没有父任务的任务）
  while (currentTask) {
    path.unshift(currentTask);

    // 获取父任务
    if (currentTask.parentTaskId) {
      currentTask = taskStore.getTaskById(currentTask.parentTaskId);
    } else {
      // 没有父任务了，到达根任务
      break;
    }
  }

  return path;
});

// 获取子任务列表
const subtasks = computed(() => {
  return taskStore.getSubtasks(props.task.id);
});

// 判断任务是否有子任务
const hasSubtasks = computed(() => {
  return subtasks.value.length > 0;
});

const assignee = computed(() => {
  return props.task.assigneeId ? userStore.userById(props.task.assigneeId) : null;
});

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    'todo': t('taskStatus.todo'),
    'in-progress': t('taskStatus.inProgress'),
    'done': t('taskStatus.done')
  };
  return labels[props.task.status] || props.task.status;
});

const statusVariant = computed(() => {
  const variants: Record<string, string> = {
    'todo': 'default',
    'in-progress': 'primary',
    'done': 'success'
  };
  return variants[props.task.status] || 'default';
});

const priorityLabel = computed(() => {
  const labels: Record<string, string> = {
    low: t('priorities.low'),
    medium: t('priorities.medium'),
    high: t('priorities.high'),
    urgent: t('priorities.urgent')
  };
  return labels[props.task.priority] || props.task.priority;
});

const priorityVariant = computed(() => {
  const variants: Record<string, string> = {
    low: 'default',
    medium: 'info',
    high: 'warning',
    urgent: 'danger'
  };
  return variants[props.task.priority] || 'default';
});

const formattedStartDate = computed(() => {
  const locale = currentLocale();
  if (locale === 'ko') {
    return dayjs(props.task.startDate).format('YYYY년 MM월 DD일');
  }
  return dayjs(props.task.startDate).format('YYYY年MM月DD日');
});

const formattedEndDate = computed(() => {
  const locale = currentLocale();
  if (locale === 'ko') {
    return dayjs(props.task.endDate).format('YYYY년 MM월 DD일');
  }
  return dayjs(props.task.endDate).format('YYYY年MM月DD日');
});

const dueDateClass = computed(() => {
  const now = dayjs();
  const dueDate = dayjs(props.task.endDate);
  const diff = dueDate.diff(now, 'day');

  if (diff < 0) return 'text-danger-600 font-medium';
  if (diff <= 2) return 'text-warning-600 font-medium';
  return 'text-secondary-600';
});

const commentUser = (comment: { userId: string }) => {
  return userStore.userById(comment.userId);
};

const formattedCommentDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

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

const handleEdit = () => {
  emit('edit', props.task);
};

const handleDelete = () => {
  if (confirm(t('taskDetail.deleteConfirm'))) {
    emit('delete', props.task);
  }
};

// 子任务相关函数
const getSubtaskStatusLabel = (status: Task['status']) => {
  const labels: Record<string, string> = {
    'todo': t('taskStatus.todo'),
    'in-progress': t('taskStatus.inProgress'),
    'done': t('taskStatus.done')
  };
  return labels[status] || status;
};

const getSubtaskStatusClass = (status: Task['status']) => {
  const classes: Record<string, string> = {
    'todo': 'text-secondary-600',
    'in-progress': 'text-primary-600',
    'done': 'text-success-600'
  };
  return classes[status] || 'text-secondary-600';
};

const getSubtaskPriorityLabel = (priority: Task['priority']) => {
  const labels: Record<string, string> = {
    low: t('priorities.low'),
    medium: t('priorities.medium'),
    high: t('priorities.high'),
    urgent: t('priorities.urgent')
  };
  return labels[priority] || priority;
};

const getSubtaskPriorityVariant = (priority: Task['priority']): 'default' | 'info' | 'warning' | 'danger' => {
  const variants: Record<string, 'default' | 'info' | 'warning' | 'danger'> = {
    low: 'default',
    medium: 'info',
    high: 'warning',
    urgent: 'danger'
  };
  return variants[priority] || 'default';
};

const getSubtaskAssigneeName = (assigneeId: string) => {
  const user = userStore.userById(assigneeId);
  return user?.name || '';
};

const openCreateSubtaskModal = () => {
  emit('createSubtask', props.task.id);
};

const handleSubtaskClick = (subtask: Task) => {
  emit('viewSubtask', subtask);
};

// 点击层级路径中的任务
const handlePathTaskClick = (pathTask: Task) => {
  emit('viewSubtask', pathTask);
};
</script>
