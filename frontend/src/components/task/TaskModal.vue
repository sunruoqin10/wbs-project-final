<template>
  <Modal
    :open="open"
    :title="isEditing ? '编辑任务' : '新建任务'"
    size="lg"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="space-y-4">
      <!-- Title -->
      <Input
        v-model="formData.title"
        label="任务标题"
        placeholder="请输入任务标题"
        :error="errors.title"
        required
      />

      <!-- Parent Task (只读，创建子任务时显示) -->
      <div v-if="parentTaskId || parentTaskName">
        <label class="mb-1 block text-sm font-medium text-secondary-700">父任务</label>
        <div class="w-full rounded-lg border border-secondary-200 bg-secondary-50 px-4 py-2 text-secondary-700">
          {{ parentTaskName || formData.parentTaskId }}
        </div>
        <input type="hidden" v-model="formData.parentTaskId" />
      </div>

      <!-- Description -->
      <div>
        <label class="mb-1 block text-sm font-medium text-secondary-700">描述</label>
        <textarea
          v-model="formData.description"
          rows="3"
          class="w-full rounded-lg border border-secondary-200 px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          placeholder="请输入任务描述"
        ></textarea>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <!-- Status -->
        <div>
          <Select
            v-model="formData.status"
            label="状态"
            :error="errors.status"
            :disabled="hasSubtasks"
            required
          >
            <option value="todo">待办</option>
            <option value="in-progress">进行中</option>
            <option value="done">已完成</option>
          </Select>
          <div
            v-if="hasSubtasks"
            class="mt-1 text-xs text-secondary-500"
          >
            此任务有子任务，状态由子任务自动确定
          </div>
        </div>

        <!-- Priority -->
        <Select
          v-model="formData.priority"
          label="优先级"
          :error="errors.priority"
          required
        >
          <option value="low">低</option>
          <option value="medium">中</option>
          <option value="high">高</option>
          <option value="urgent">紧急</option>
        </Select>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <!-- Start Date -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            开始日期
            <span class="text-danger-500">*</span>
          </label>
          <div
            v-if="hasSubtasks"
            class="mb-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
          >
            此任务有子任务，开始时间将由子任务自动计算，无法手动修改
          </div>
          <input
            v-model="formData.startDate"
            type="date"
            :disabled="hasSubtasks"
            :class="[
              'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              errors.startDate ? 'border-danger-300' : 'border-secondary-200',
              { 'opacity-50 cursor-not-allowed bg-secondary-50': hasSubtasks }
            ]"
            required
          />
          <p v-if="errors.startDate" class="mt-1 text-sm text-danger-600">{{ errors.startDate }}</p>
        </div>

        <!-- End Date -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            结束日期
            <span class="text-danger-500">*</span>
          </label>
          <div
            v-if="hasSubtasks"
            class="mb-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
          >
            此任务有子任务，结束时间将由子任务自动计算，无法手动修改
          </div>
          <input
            v-model="formData.endDate"
            type="date"
            :disabled="hasSubtasks"
            :class="[
              'w-full rounded-lg border px-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              errors.endDate ? 'border-danger-300' : 'border-secondary-200',
              { 'opacity-50 cursor-not-allowed bg-secondary-50': hasSubtasks }
            ]"
            required
          />
          <p v-if="errors.endDate" class="mt-1 text-sm text-danger-600">{{ errors.endDate }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <!-- Assignee -->
        <Select
          v-model="formData.assigneeId"
          label="负责人"
          :error="errors.assigneeId"
          required
        >
          <option value="">请选择负责人</option>
          <option v-for="user in projectMembers" :key="user.id" :value="user.id">
            {{ user.name }}
          </option>
        </Select>

        <!-- Estimated Hours (只读，自动计算) -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            预估工时: {{ formData.estimatedHours || 0 }} 小时
            <span class="text-xs text-secondary-500 font-normal ml-2">(根据工作日自动计算)</span>
          </label>
          <input
            v-model.number="formData.estimatedHours"
            type="number"
            class="w-full rounded-lg border border-secondary-200 px-4 py-2 bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60"
            disabled
          />
          <p class="mt-1 text-xs text-secondary-500">此字段根据任务的开始日期和结束日期自动计算工作日（每天8小时），无法手动编辑</p>
        </div>
      </div>

      <!-- Progress -->
      <div>
        <label class="mb-1 block text-sm font-medium text-secondary-700">进度: {{ formData.progress }}%</label>
        <div
          v-if="hasSubtasks"
          class="mb-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
        >
          此任务有子任务，进度将由子任务自动计算，无法手动修改
        </div>
        <div
          v-if="isTodoStatus"
          class="mb-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
        >
          待办状态的任务进度固定为0%，无法修改
        </div>
        <div
          v-if="isDoneStatus"
          class="mb-2 rounded-md bg-secondary-100 px-3 py-2 text-xs text-secondary-600"
        >
          已完成状态的任务进度固定为100%，无法修改
        </div>
        <input
          v-model.number="formData.progress"
          type="range"
          min="0"
          max="100"
          class="w-full"
          :disabled="hasSubtasks || isTodoStatus || isDoneStatus"
          :class="{ 'opacity-50 cursor-not-allowed': hasSubtasks || isTodoStatus || isDoneStatus }"
        />
      </div>

      <!-- Tags -->
      <div>
        <label class="mb-1 block text-sm font-medium text-secondary-700">标签</label>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="tag in commonTags"
            :key="tag"
            @click="toggleTag(tag)"
            :class="[
              'cursor-pointer rounded-full px-3 py-1 text-sm transition-colors',
              formData.tags.includes(tag)
                ? 'bg-primary-600 text-white'
                : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
            ]"
          >
            {{ tag }}
          </span>
        </div>
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
import Input from '@/components/common/Input.vue';
import Select from '@/components/common/Select.vue';
import type { Task } from '@/types';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { useProjectStore } from '@/stores/project';

interface Props {
  open: boolean;
  task?: Task | null;
  projectId?: string;
  initialStatus?: Task['status'];
  showParentTaskSelector?: boolean; // 控制是否显示父任务下拉菜单
  parentTaskId?: string; // 创建子任务时，默认选中的父任务ID（只读模式）
}

const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
  save: [task: Partial<Task>];
}>();

const userStore = useUserStore();
const taskStore = useTaskStore();
const projectStore = useProjectStore();

const users = computed(() => userStore.users);
const isEditing = computed(() => !!props.task);
const saving = ref(false);

// 判断任务是否有子任务
const hasSubtasks = computed(() => {
  if (!props.task?.id) return false;
  return taskStore.getSubtasks(props.task.id).length > 0;
});

// 判断任务状态是否为待办
const isTodoStatus = computed(() => {
  return formData.status === 'todo';
});

// 判断任务状态是否为已完成
const isDoneStatus = computed(() => {
  return formData.status === 'done';
});

// 获取项目成员列表作为可选的负责人
const projectMembers = computed(() => {
  const projectId = props.projectId || props.task?.projectId;
  if (!projectId) return [];

  const project = projectStore.projectById(projectId);
  if (!project || !project.memberIds) return [];

  // 根据 memberIds 筛选用户
  const members = users.value.filter(user => project.memberIds?.includes(user.id));

  // 如果正在编辑任务，且当前负责人不在成员列表中，也添加进去
  if (props.task?.assigneeId) {
    const currentAssignee = userStore.userById(props.task.assigneeId);
    if (currentAssignee && !members.find(m => m.id === currentAssignee.id)) {
      members.push(currentAssignee);
    }
  }

  return members;
});

// 获取可作为父任务的列表
const availableParentTasks = computed(() => {
  const projectId = props.projectId || props.task?.projectId;
  if (!projectId) return [];

  // 获取当前项目的所有任务
  const projectTasks = taskStore.tasksByProject(projectId);

  // 排除当前正在编辑的任务（避免将自己设置为父任务）
  // 并且排除已经是当前任务子任务的任务（避免循环引用）
  return projectTasks.filter(t => {
    if (!t) return false;
    // 如果正在编辑任务，不能将自己或自己的子孙任务作为父任务
    if (props.task?.id === t.id) return false;
    // 如果任务已经有父任务，不能作为父任务（只允许两级结构）
    // 如果想要支持多级，可以去掉这个限制
    return true;
  });
});

// 获取父任务名称（只读模式下显示）
const parentTaskName = computed(() => {
  if (!props.parentTaskId) return '';
  const parentTask = taskStore.getTaskById(props.parentTaskId);
  return parentTask?.title || '';
});

const commonTags = ['前端', '后端', '设计', '数据库', 'API', '测试', '文档', '优化'];

const formData = reactive({
  title: '',
  description: '',
  status: 'todo' as Task['status'], // 确保类型是字符串 'todo'
  priority: 'medium' as Task['priority'],
  startDate: '',
  endDate: '',
  assigneeId: '',
  parentTaskId: '',
  estimatedHours: undefined as number | undefined,
  progress: 0,
  tags: [] as string[]
});

// 确保状态始终有默认值（防止响应式丢失）
const ensureDefaultStatus = () => {
  if (!formData.status || formData.status === '') {
    formData.status = 'todo';
  }
};

const errors = reactive({
  title: '',
  status: '',
  priority: '',
  startDate: '',
  endDate: '',
  assigneeId: ''
});

// Reset form
const resetForm = () => {
  formData.title = '';
  formData.description = '';
  formData.status = 'todo';  // 明确设置为"待办"
  formData.priority = 'medium';
  formData.startDate = new Date().toISOString().split('T')[0];
  formData.endDate = '';
  formData.assigneeId = '';
  formData.parentTaskId = props.parentTaskId || ''; // 保留父任务ID
  formData.estimatedHours = 0;
  formData.progress = 0;
  formData.tags = [];
  // 清除所有错误
  errors.title = '';
  errors.status = '';
  errors.priority = '';
  errors.startDate = '';
  errors.endDate = '';
  errors.assigneeId = '';
};

// Load task data for editing
const loadTaskData = () => {
  if (props.task) {
    formData.title = props.task.title;
    formData.description = props.task.description;
    formData.status = props.task.status || 'todo'; // 确保状态有默认值
    formData.priority = props.task.priority;
    formData.startDate = props.task.startDate;
    formData.endDate = props.task.endDate;
    formData.assigneeId = props.task.assigneeId || '';
    formData.parentTaskId = props.task.parentTaskId || '';
    formData.estimatedHours = props.task.estimatedHours || calculateEstimatedHours();
    formData.progress = props.task.progress;
    // 确保 tags 是数组，避免 undefined 错误
    formData.tags = props.task.tags && Array.isArray(props.task.tags) ? [...props.task.tags] : [];
  } else {
    // 新建任务时，明确设置默认值
    formData.title = '';
    formData.description = '';
    formData.status = 'todo'; // 明确设置为"待办"
    formData.priority = 'medium';
    formData.startDate = new Date().toISOString().split('T')[0];
    formData.endDate = '';
    formData.assigneeId = '';
    formData.parentTaskId = props.parentTaskId || ''; // 如果有父任务ID，使用它
    formData.estimatedHours = 0;
    formData.progress = 0;
    formData.tags = [];
  }
  // 确保状态始终有值
  ensureDefaultStatus();
};

// Toggle tag
const toggleTag = (tag: string) => {
  const index = formData.tags.indexOf(tag);
  if (index === -1) {
    formData.tags.push(tag);
  } else {
    formData.tags.splice(index, 1);
  }
};

// 计算预估工时（基于工作日，每天8小时）
const calculateEstimatedHours = (): number => {
  if (!formData.startDate || !formData.endDate) {
    return 0;
  }

  const start = new Date(formData.startDate);
  const end = new Date(formData.endDate);

  // 如果结束日期早于开始日期，返回0
  if (end < start) {
    return 0;
  }

  let workDays = 0;
  let current = new Date(start);

  // 遍历从开始日期到结束日期的每一天
  while (current <= end) {
    const dayOfWeek = current.getDay();
    // 0=周日, 6=周六，排除周末
    if (dayOfWeek !== 0 && dayOfWeek !== 6) {
      workDays++;
    }
    // 移到下一天
    current.setDate(current.getDate() + 1);
  }

  // 每个工作日按8小时计算
  return workDays * 8;
};

// Validate form
const validateForm = (): boolean => {
  let isValid = true;

  // 清除所有错误
  errors.title = '';
  errors.status = '';
  errors.priority = '';
  errors.startDate = '';
  errors.endDate = '';

  // 验证任务标题
  if (!formData.title.trim()) {
    errors.title = '请输入任务标题';
    isValid = false;
  }

  // 验证状态
  if (!formData.status) {
    errors.status = '请选择任务状态';
    isValid = false;
  }

  // 验证优先级
  if (!formData.priority) {
    errors.priority = '请选择优先级';
    isValid = false;
  }

  // 验证开始日期
  if (!formData.startDate) {
    errors.startDate = '请选择开始日期';
    isValid = false;
  }

  // 验证结束日期
  if (!formData.endDate) {
    errors.endDate = '请选择结束日期';
    isValid = false;
  }

  // 验证日期逻辑
  if (formData.startDate && formData.endDate) {
    if (new Date(formData.startDate) > new Date(formData.endDate)) {
      errors.endDate = '结束日期不能早于开始日期';
      isValid = false;
    }
  }

  // 验证负责人
  if (!formData.assigneeId) {
    errors.assigneeId = '请选择负责人';
    isValid = false;
  }

  // 验证预估工时
  if (formData.estimatedHours === undefined || formData.estimatedHours === null || formData.estimatedHours < 0) {
    errors.estimatedHours = '请输入有效的预估工时';
    isValid = false;
  }

  return isValid;
};

// Handle submit
const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }

  saving.value = true;

  // 验证 projectId 是否存在（新建任务时必须有 projectId）
  const finalProjectId = props.projectId || props.task?.projectId;
  if (!finalProjectId) {
    alert('项目ID不能为空');
    saving.value = false;
    return;
  }

  // 只发送后端支持的字段，过滤掉 tags、dependencies、attachments、comments 等
  const taskData: Partial<Task> = {
    projectId: finalProjectId,
    title: formData.title,
    description: formData.description || undefined, // 空字符串转为 undefined
    status: formData.status || 'todo', // status 是必填字段，提供默认值
    priority: formData.priority,
    assigneeId: formData.assigneeId || undefined,
    parentTaskId: formData.parentTaskId || undefined,
    startDate: formData.startDate || undefined,
    endDate: formData.endDate || undefined,
    estimatedHours: formData.estimatedHours,
    progress: formData.progress
  };

  console.log('TaskModal submitting data:', taskData);
  emit('save', taskData);

  // Reset saving state after a short delay
  // Let parent component handle closing the modal
  setTimeout(() => {
    saving.value = false;
  }, 300);
};

// Handle close
const handleClose = () => {
  resetForm();
  emit('close');
};

// Watch for task changes
watch(() => props.task, loadTaskData);

// 监听状态变化，当状态变为 todo 时，自动将进度设置为 0
// 当状态变为 done 时，自动将进度设置为 100
watch(() => formData.status, (newStatus) => {
  if (newStatus === 'todo' && formData.progress !== 0) {
    formData.progress = 0;
  }
  if (newStatus === 'done' && formData.progress !== 100) {
    formData.progress = 100;
  }
});

watch(() => props.open, async (isOpen) => {
  if (isOpen) {
    // 加载用户数据和项目数据
    await userStore.loadUsers();
    await projectStore.loadProjects();

    // 确保新建任务时状态有默认值 - 必须在 loadTaskData 之前
    if (!props.task) {
      formData.status = props.initialStatus || 'todo';
    }

    loadTaskData();

    // 再次确保状态有默认值（防御性编程）
    if (!formData.status) {
      formData.status = 'todo';
    }

    // 如果状态是 todo，确保进度为 0
    if (formData.status === 'todo') {
      formData.progress = 0;
    }

    // 如果状态是 done，确保进度为 100
    if (formData.status === 'done') {
      formData.progress = 100;
    }

    // 打开表单后，如果有日期则计算预估工时
    if (formData.startDate && formData.endDate) {
      formData.estimatedHours = calculateEstimatedHours();
    }
  }
});

// 监听开始日期和结束日期变化，自动计算预估工时
watch([() => formData.startDate, () => formData.endDate], () => {
  formData.estimatedHours = calculateEstimatedHours();
}, { immediate: true }); // 添加 immediate: true 确保初始时就计算
</script>
