<template>
  <Modal
    :open="open"
    title="加班审批"
    size="lg"
    @close="handleClose"
  >
    <div v-if="record" class="space-y-6">
      <!-- 加班详情 -->
      <div class="rounded-lg bg-secondary-50 p-4">
        <h4 class="mb-4 text-sm font-semibold text-secondary-900">加班详情</h4>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span class="text-secondary-500">加班人员：</span>
            <span class="font-medium text-secondary-900">{{ getUserName(record.userId) }}</span>
          </div>
          <div>
            <span class="text-secondary-500">所属项目：</span>
            <span class="font-medium text-secondary-900">{{ getProjectName(record.projectId) }}</span>
          </div>
          <div>
            <span class="text-secondary-500">加班日期：</span>
            <span class="font-medium text-secondary-900">{{ formatDate(record.overtimeDate) }}</span>
          </div>
          <div>
            <span class="text-secondary-500">时间段：</span>
            <span class="font-medium text-secondary-900">{{ record.startTime }} - {{ record.endTime }}</span>
          </div>
          <div>
            <span class="text-secondary-500">加班时长：</span>
            <span class="font-semibold text-primary-600">{{ record.hours }} 小时</span>
          </div>
          <div>
            <span class="text-secondary-500">加班类型：</span>
            <span :class="getTypeBadgeClass(record.overtimeType)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
              {{ getTypeLabel(record.overtimeType) }}
            </span>
          </div>
          <div>
            <span class="text-secondary-500">补偿方式：</span>
            <span class="font-medium text-secondary-900">{{ getCompensationLabel(record.compensationType) }}</span>
          </div>
          <div v-if="record.taskId">
            <span class="text-secondary-500">关联任务：</span>
            <span class="font-medium text-secondary-900">{{ getTaskName(record.taskId) }}</span>
          </div>
        </div>
      </div>

      <!-- 加班原因 -->
      <div>
        <h4 class="mb-2 text-sm font-semibold text-secondary-900">加班原因</h4>
        <p class="rounded-lg bg-secondary-50 p-3 text-sm text-secondary-700">{{ record.reason }}</p>
      </div>

      <!-- 审批操作 -->
      <div>
        <h4 class="mb-2 text-sm font-semibold text-secondary-900">审批操作</h4>
        <div class="flex gap-4">
          <label class="flex items-center">
            <input
              v-model="approvalDecision"
              type="radio"
              value="approve"
              class="h-4 w-4 border-secondary-300 text-green-600 focus:ring-green-500"
            />
            <span class="ml-2 text-sm text-secondary-700">通过</span>
          </label>
          <label class="flex items-center">
            <input
              v-model="approvalDecision"
              type="radio"
              value="reject"
              class="h-4 w-4 border-secondary-300 text-red-600 focus:ring-red-500"
            />
            <span class="ml-2 text-sm text-secondary-700">拒绝</span>
          </label>
        </div>
      </div>

      <!-- 拒绝原因（仅拒绝时显示） -->
      <div v-if="approvalDecision === 'reject'">
        <label class="mb-1 block text-sm font-medium text-secondary-700">
          拒绝原因
          <span class="text-danger-500">*</span>
        </label>
        <textarea
          v-model="rejectReason"
          rows="3"
          :class="[
            'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
            rejectError ? 'border-danger-300' : 'border-secondary-200'
          ]"
          placeholder="请说明拒绝原因..."
        ></textarea>
        <p v-if="rejectError" class="mt-1 text-sm text-danger-600">{{ rejectError }}</p>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button variant="secondary" @click="handleClose">取消</Button>
        <Button
          v-if="approvalDecision === 'approve'"
          variant="primary"
          @click="handleApprove"
          :loading="saving"
          :disabled="!approvalDecision"
        >
          通过
        </Button>
        <Button
          v-else
          variant="danger"
          @click="handleReject"
          :loading="saving"
          :disabled="!approvalDecision"
        >
          拒绝
        </Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import Modal from '@/components/common/Modal.vue';
import Button from '@/components/common/Button.vue';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import type { OvertimeRecord } from '@/types';
import dayjs from 'dayjs';

interface Props {
  open: boolean;
  record?: OvertimeRecord | null;
}

const props = withDefaults(defineProps<Props>(), {
  record: null
});

const emit = defineEmits<{
  close: [];
  approve: [recordId: string];
  reject: [recordId: string, rejectReason: string];
}>();

const projectStore = useProjectStore();
const userStore = useUserStore();
const taskStore = useTaskStore();

const approvalDecision = ref<'approve' | 'reject' | ''>('');
const rejectReason = ref('');
const rejectError = ref('');
const saving = ref(false);

// Helper functions
const getUserName = (userId: string) => {
  const user = userStore.userById(userId);
  return user?.name || '未知';
};

const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || '未知项目';
};

const getTaskName = (taskId: string) => {
  const task = taskStore.getTaskById(taskId);
  return task?.title || '未知任务';
};

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD');
};

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    weekday: '工作日',
    weekend: '周末',
    holiday: '节假日'
  };
  return labels[type] || type;
};

const getTypeBadgeClass = (type: string) => {
  const classes: Record<string, string> = {
    weekday: 'bg-blue-100 text-blue-800',
    weekend: 'bg-green-100 text-green-800',
    holiday: 'bg-purple-100 text-purple-800'
  };
  return classes[type] || 'bg-secondary-100 text-secondary-800';
};

const getCompensationLabel = (type?: string) => {
  if (!type) return '-';
  const labels: Record<string, string> = {
    pay: '加班费',
    timeoff: '调休'
  };
  return labels[type] || type;
};

// 通过审批
const handleApprove = async () => {
  if (!props.record) return;
  
  saving.value = true;
  emit('approve', props.record.id);
  setTimeout(() => {
    saving.value = false;
  }, 300);
};

// 拒绝审批
const handleReject = async () => {
  if (!props.record) return;

  // 验证拒绝原因
  if (!rejectReason.value.trim()) {
    rejectError.value = '请填写拒绝原因';
    return;
  }

  rejectError.value = '';
  saving.value = true;
  emit('reject', props.record.id, rejectReason.value);
  setTimeout(() => {
    saving.value = false;
  }, 300);
};

// 关闭弹窗
const handleClose = () => {
  approvalDecision.value = '';
  rejectReason.value = '';
  rejectError.value = '';
  emit('close');
};

// 监听弹窗打开
watch(() => props.open, async (isOpen) => {
  if (isOpen) {
    // 重置状态
    approvalDecision.value = '';
    rejectReason.value = '';
    rejectError.value = '';
    
    // 加载数据
    await Promise.all([
      projectStore.loadProjects(),
      userStore.loadUsers(),
      taskStore.loadTasks()
    ]);
  }
});
</script>
