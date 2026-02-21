<template>
  <Modal
    :open="open"
    :title="isEditing ? '编辑加班记录' : '新增加班记录'"
    size="lg"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="space-y-4">
      <!-- 所属项目 -->
      <Select
        v-model="formData.projectId"
        label="所属项目"
        :error="errors.projectId"
        required
      >
        <option value="">请选择项目</option>
        <option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
          {{ project.name }}
        </option>
      </Select>

      <!-- 关联任务 -->
      <div>
        <Select
          v-model="formData.taskId"
          label="关联任务"
          :error="errors.taskId"
          :disabled="!formData.projectId"
        >
          <option value="">无关联任务</option>
          <option v-for="task in projectTasks" :key="task.id" :value="task.id">
            {{ task.title }}
          </option>
        </Select>
        <p v-if="formData.projectId && projectTasks.length === 0" class="mt-1 text-xs text-secondary-500">
          该项目暂无可关联的叶子任务（已完成或无子任务的任务）
        </p>
        <p v-else-if="formData.projectId" class="mt-1 text-xs text-secondary-500">
          仅显示未完成的叶子任务（无子任务的任务）
        </p>
      </div>

      <!-- 加班人员 -->
      <Select
        v-model="formData.userId"
        label="加班人员"
        :error="errors.userId"
        required
      >
        <option value="">请选择人员</option>
        <option v-for="user in availableUsers" :key="user.id" :value="user.id">
          {{ user.name }} ({{ user.department }})
        </option>
      </Select>

      <!-- 加班日期 -->
      <div>
        <label class="mb-1 block text-sm font-medium text-secondary-700">
          加班日期
          <span class="text-danger-500">*</span>
        </label>
        <input
          v-model="formData.overtimeDate"
          type="date"
          :class="[
            'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
            errors.overtimeDate ? 'border-danger-300' : 'border-secondary-200'
          ]"
          required
        />
        <p v-if="errors.overtimeDate" class="mt-1 text-sm text-danger-600">{{ errors.overtimeDate }}</p>
      </div>

      <!-- 时间段 -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            开始时间
            <span class="text-danger-500">*</span>
          </label>
          <input
            v-model="formData.startTime"
            type="time"
            :class="[
              'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              errors.startTime ? 'border-danger-300' : 'border-secondary-200'
            ]"
            required
          />
          <p v-if="errors.startTime" class="mt-1 text-sm text-danger-600">{{ errors.startTime }}</p>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            结束时间
            <span class="text-danger-500">*</span>
          </label>
          <input
            v-model="formData.endTime"
            type="time"
            :class="[
              'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              errors.endTime ? 'border-danger-300' : 'border-secondary-200'
            ]"
            required
          />
          <p v-if="errors.endTime" class="mt-1 text-sm text-danger-600">{{ errors.endTime }}</p>
        </div>
      </div>

      <!-- 自动计算时长 -->
      <div class="rounded-lg bg-secondary-50 p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-secondary-700">加班时长</span>
          <span class="text-lg font-semibold text-primary-600">{{ formData.hours }} 小时</span>
        </div>
        <p class="mt-1 text-xs text-secondary-500">根据开始时间和结束时间自动计算</p>
      </div>

      <!-- 加班类型 -->
      <Select
        v-model="formData.overtimeType"
        label="加班类型"
        :error="errors.overtimeType"
        required
      >
        <option value="">请选择类型</option>
        <option value="weekday">工作日加班</option>
        <option value="weekend">周末加班</option>
        <option value="holiday">节假日加班</option>
      </Select>

      <!-- 补偿方式 -->
      <Select
        v-model="formData.compensationType"
        label="补偿方式"
        :error="errors.compensationType"
        required
      >
        <option value="">请选择补偿方式</option>
        <option value="pay">加班费</option>
        <option value="timeoff">调休</option>
      </Select>

      <!-- 加班原因 -->
      <div>
        <label class="mb-1 block text-sm font-medium text-secondary-700">
          加班原因
          <span class="text-danger-500">*</span>
        </label>
        <textarea
          v-model="formData.reason"
          rows="3"
          :class="[
            'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
            errors.reason ? 'border-danger-300' : 'border-secondary-200'
          ]"
          placeholder="请详细说明加班原因..."
          required
        ></textarea>
        <p v-if="errors.reason" class="mt-1 text-sm text-danger-600">{{ errors.reason }}</p>
      </div>
    </form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button variant="secondary" @click="handleClose">取消</Button>
        <Button variant="primary" @click="handleSubmit" :loading="saving">
          {{ isEditing ? '保存' : '创建' }}
        </Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import Modal from '@/components/common/Modal.vue';
import Button from '@/components/common/Button.vue';
import Select from '@/components/common/Select.vue';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import type { OvertimeRecord } from '@/types';

interface Props {
  open: boolean;
  record?: OvertimeRecord | null;
}

const props = withDefaults(defineProps<Props>(), {
  record: null
});

const emit = defineEmits<{
  close: [];
  save: [data: Partial<OvertimeRecord>];
}>();

const projectStore = useProjectStore();
const userStore = useUserStore();
const taskStore = useTaskStore();

const isEditing = computed(() => !!props.record);
const saving = ref(false);

// 表单数据
const formData = reactive({
  projectId: '',
  taskId: '',
  userId: '',
  overtimeDate: '',
  startTime: '',
  endTime: '',
  hours: 0,
  overtimeType: '' as OvertimeRecord['overtimeType'] | '',
  compensationType: '' as OvertimeRecord['compensationType'] | '',
  reason: ''
});

// 错误信息
const errors = reactive({
  projectId: '',
  taskId: '',
  userId: '',
  overtimeDate: '',
  startTime: '',
  endTime: '',
  overtimeType: '',
  compensationType: '',
  reason: ''
});

// 获取选中项目的叶子任务列表（没有子任务的任务，且未完成）
const projectTasks = computed(() => {
  if (!formData.projectId) return [];

  const allProjectTasks = taskStore.tasksByProject(formData.projectId);

  // 获取所有有子任务的任务ID（即父任务）
  const parentTaskIds = new Set(
    allProjectTasks
      .filter(task => task.parentTaskId)
      .map(task => task.parentTaskId)
  );

  // 返回没有子任务的任务（叶子任务）且状态不是"已完成"的任务
  return allProjectTasks.filter(task =>
    !parentTaskIds.has(task.id) && task.status !== 'done'
  );
});

// 获取可选用户列表（项目成员）
const availableUsers = computed(() => {
  if (!formData.projectId) return userStore.users;
  
  const project = projectStore.projectById(formData.projectId);
  if (!project || !project.memberIds) return userStore.users;
  
  return userStore.users.filter(user => project.memberIds?.includes(user.id));
});

// 计算加班时长
const calculateHours = () => {
  if (!formData.startTime || !formData.endTime) {
    formData.hours = 0;
    return;
  }

  const [startHour, startMinute] = formData.startTime.split(':').map(Number);
  const [endHour, endMinute] = formData.endTime.split(':').map(Number);

  const startMinutes = startHour * 60 + startMinute;
  const endMinutes = endHour * 60 + endMinute;

  if (endMinutes <= startMinutes) {
    formData.hours = 0;
    return;
  }

  const diffMinutes = endMinutes - startMinutes;
  formData.hours = Math.round(diffMinutes / 60 * 10) / 10; // 保留一位小数
};

// 重置表单
const resetForm = () => {
  formData.projectId = '';
  formData.taskId = '';
  formData.userId = '';
  formData.overtimeDate = new Date().toISOString().split('T')[0];
  formData.startTime = '';
  formData.endTime = '';
  formData.hours = 0;
  formData.overtimeType = '';
  formData.compensationType = '';
  formData.reason = '';

  // 清除错误
  Object.keys(errors).forEach(key => {
    errors[key as keyof typeof errors] = '';
  });
};

// 加载记录数据（编辑模式）
const loadRecordData = () => {
  if (props.record) {
    formData.projectId = props.record.projectId;
    formData.taskId = props.record.taskId || '';
    formData.userId = props.record.userId;
    formData.overtimeDate = props.record.overtimeDate;
    formData.startTime = props.record.startTime;
    formData.endTime = props.record.endTime;
    formData.hours = props.record.hours;
    formData.overtimeType = props.record.overtimeType;
    formData.compensationType = props.record.compensationType || '';
    formData.reason = props.record.reason;
  } else {
    resetForm();
    // 默认使用当前用户
    if (userStore.currentUserId) {
      formData.userId = userStore.currentUserId;
    }
  }
};

// 验证表单
const validateForm = (): boolean => {
  let isValid = true;

  // 清除所有错误
  Object.keys(errors).forEach(key => {
    errors[key as keyof typeof errors] = '';
  });

  if (!formData.projectId) {
    errors.projectId = '请选择所属项目';
    isValid = false;
  }

  if (!formData.userId) {
    errors.userId = '请选择加班人员';
    isValid = false;
  }

  if (!formData.overtimeDate) {
    errors.overtimeDate = '请选择加班日期';
    isValid = false;
  }

  if (!formData.startTime) {
    errors.startTime = '请选择开始时间';
    isValid = false;
  }

  if (!formData.endTime) {
    errors.endTime = '请选择结束时间';
    isValid = false;
  }

  if (formData.startTime && formData.endTime) {
    const [startHour, startMinute] = formData.startTime.split(':').map(Number);
    const [endHour, endMinute] = formData.endTime.split(':').map(Number);
    const startMinutes = startHour * 60 + startMinute;
    const endMinutes = endHour * 60 + endMinute;

    if (endMinutes <= startMinutes) {
      errors.endTime = '结束时间必须晚于开始时间';
      isValid = false;
    }
  }

  if (!formData.overtimeType) {
    errors.overtimeType = '请选择加班类型';
    isValid = false;
  }

  if (!formData.compensationType) {
    errors.compensationType = '请选择补偿方式';
    isValid = false;
  }

  if (!formData.reason.trim()) {
    errors.reason = '请填写加班原因';
    isValid = false;
  }

  return isValid;
};

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }

  saving.value = true;

  const data: Partial<OvertimeRecord> = {
    projectId: formData.projectId,
    taskId: formData.taskId || undefined,
    userId: formData.userId,
    overtimeDate: formData.overtimeDate,
    startTime: formData.startTime,
    endTime: formData.endTime,
    hours: formData.hours,
    overtimeType: formData.overtimeType as OvertimeRecord['overtimeType'],
    compensationType: formData.compensationType as OvertimeRecord['compensationType'],
    reason: formData.reason
  };

  emit('save', data);

  setTimeout(() => {
    saving.value = false;
  }, 300);
};

// 关闭弹窗
const handleClose = () => {
  resetForm();
  emit('close');
};

// 监听时间变化，自动计算时长
watch([() => formData.startTime, () => formData.endTime], () => {
  calculateHours();
});

// 监听项目变化，清空不在新项目中的任务选择
watch(() => formData.projectId, (newProjectId, oldProjectId) => {
  if (oldProjectId && newProjectId !== oldProjectId && formData.taskId) {
    // 检查当前选择的任务是否还在新项目的叶子任务列表中
    const taskExists = projectTasks.value.some(task => task.id === formData.taskId);
    if (!taskExists) {
      formData.taskId = '';
    }
  }
});

// 监听弹窗打开
watch(() => props.open, async (isOpen) => {
  if (isOpen) {
    await Promise.all([
      projectStore.loadProjects(),
      userStore.loadUsers(),
      taskStore.loadTasks()
    ]);
    loadRecordData();
  }
});

// 监听记录变化
watch(() => props.record, loadRecordData);
</script>
