<template>
  <Modal
    :open="open"
    :title="isEditing ? '编辑项目' : '新建项目'"
    size="2xl"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="space-y-5">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Input
          v-model="formData.name"
          label="项目名称"
          placeholder="请输入项目名称"
          :error="errors.name"
          required
        />
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">项目描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
            placeholder="请输入项目描述"
          ></textarea>
        </div>
      </div>

      <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Select v-model="formData.status" label="状态" required :disabled="!isEditing">
          <option value="planning">计划中</option>
          <option value="active">进行中</option>
          <option value="completed">已完成</option>
          <option value="on-hold">已暂停</option>
          <option value="cancelled">废弃</option>
        </Select>

        <Select v-model="formData.priority" label="优先级" required>
          <option value="low">低</option>
          <option value="medium">中</option>
          <option value="high">高</option>
          <option value="critical">紧急</option>
        </Select>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            开始日期 *
            <span v-if="isEditing" class="text-xs text-secondary-500 font-normal ml-1">(不可编辑)</span>
          </label>
          <input
            v-model="formData.startDate"
            type="date"
            :disabled="isEditing"
            :class="[
              'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              isEditing ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200' : 'border-secondary-200'
            ]"
            required
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            结束日期 *
            <span v-if="isEditing" class="text-xs text-secondary-500 font-normal ml-1">(不可编辑)</span>
          </label>
          <input
            v-model="formData.endDate"
            type="date"
            :disabled="isEditing"
            :class="[
              'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              isEditing ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200' : 'border-secondary-200'
            ]"
            required
          />
        </div>
      </div>

      <div v-if="isEditing" class="rounded-lg bg-info-50 border border-info-200 px-3 py-2">
        <div class="flex gap-2 items-start">
          <svg class="h-4 w-4 text-info-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="text-xs text-info-800 leading-relaxed">
            项目的开始日期和结束日期会根据任务的日期自动调整，无法手动编辑。如需修改项目日期，请调整相关任务的日期。
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            项目负责人 *
            <span v-if="!hasTeamMembers" class="text-xs text-secondary-500 font-normal ml-1">(请先选择团队成员)</span>
          </label>
          <select
            v-model="formData.ownerId"
            :disabled="!hasTeamMembers"
            :class="[
              'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 transition-colors',
              !hasTeamMembers
                ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                : 'border-secondary-200 bg-white'
            ]"
            required
          >
            <option value="">
              {{ hasTeamMembers ? '请选择' : '请先选择团队成员' }}
            </option>
            <option v-for="user in availableOwners" :key="user.id" :value="user.id">
              {{ user.name }}
            </option>
          </select>
          <p v-if="!hasTeamMembers" class="mt-1 text-xs text-secondary-500">项目负责人必须从已选中的团队成员中选择</p>
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            预估工时: {{ formData.estimatedHours || 0 }} 小时
          </label>
          <input
            v-model.number="formData.estimatedHours"
            type="number"
            class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60"
            disabled
          />
          <p class="mt-1 text-xs text-secondary-500">根据工作日自动计算（每天8小时）</p>
        </div>
      </div>

      <div>
        <label class="mb-2 block text-sm font-medium text-secondary-700">团队成员</label>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2 rounded-lg border border-secondary-200 p-3 max-h-40 overflow-y-auto">
          <label
            v-for="user in users"
            :key="user.id"
            class="flex items-center gap-2 rounded-md px-2 py-2 hover:bg-secondary-50 cursor-pointer transition-colors"
          >
            <input
              type="checkbox"
              :value="user.id"
              v-model="formData.memberIds"
              class="rounded border-secondary-300 text-primary-600 focus:ring-primary-500 h-4 w-4"
            />
            <img :src="user.avatar" :alt="user.name" class="h-5 w-5 rounded-full" />
            <span class="text-xs text-secondary-700 truncate">{{ user.name }}</span>
          </label>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-secondary-700">项目颜色</label>
          <div class="flex gap-2 flex-wrap">
            <button
              v-for="color in colors"
              :key="color"
              type="button"
              @click="formData.color = color"
              :class="[
                'h-9 w-9 rounded-md border-2 transition-all flex-shrink-0',
                formData.color === color
                  ? 'border-secondary-900 scale-110 shadow-md'
                  : 'border-transparent hover:scale-105 hover:shadow'
              ]"
              :style="{ backgroundColor: color }"
            ></button>
          </div>
        </div>

        <div>
          <label class="mb-2 block text-sm font-medium text-secondary-700">标签</label>
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in commonTags"
              :key="tag"
              @click="toggleTag(tag)"
              :class="[
                'cursor-pointer rounded-full px-2.5 py-1 text-xs transition-colors',
                formData.tags.includes(tag)
                  ? 'bg-primary-600 text-white'
                  : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
              ]"
            >
              {{ tag }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="isEditing" class="pt-2">
        <div class="flex items-center gap-3">
          <div class="flex-1">
            <label class="mb-1 block text-sm font-medium text-secondary-700">
              进度: {{ formData.progress }}%
              <span class="text-xs text-secondary-500 font-normal ml-1">(根据任务完成情况自动计算)</span>
            </label>
            <input
              v-model.number="formData.progress"
              type="range"
              min="0"
              max="100"
              class="w-full opacity-60 cursor-not-allowed h-2"
              disabled
            />
          </div>
          <div class="text-2xl font-bold text-primary-600 min-w-[3rem] text-center">
            {{ formData.progress }}%
          </div>
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
import type { Project } from '@/types';
import { useUserStore } from '@/stores/user';
import { useProjectStore } from '@/stores/project';

interface Props {
  open: boolean;
  project?: Project | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
  save: [project: Partial<Project>];
}>();

const userStore = useUserStore();
const projectStore = useProjectStore();

const users = computed(() => userStore.users);
const isEditing = computed(() => !!props.project);
const saving = ref(false);

const hasTeamMembers = computed(() => formData.memberIds.length > 0);
const availableOwners = computed(() => {
  return users.value.filter(user => formData.memberIds.includes(user.id));
});

const commonTags = ['前端', '后端', '全栈', '移动端', '设计', '数据库', 'API', 'DevOps', '测试'];

const colors = [
  '#3b82f6', // blue
  '#10b981', // green
  '#8b5cf6', // purple
  '#f59e0b', // amber
  '#06b6d4', // cyan
  '#ec4899', // pink
  '#64748b', // slate
  '#ef4444'  // red
];

const formData = reactive({
  name: '',
  description: '',
  status: 'planning' as Project['status'],
  priority: 'medium' as Project['priority'],
  startDate: '',
  endDate: '',
  ownerId: '',
  memberIds: [] as string[],
  color: '#3b82f6',
  tags: [] as string[],
  progress: 0,
  estimatedHours: 0
});

const errors = reactive({
  name: ''
});

// Reset form
const resetForm = () => {
  formData.name = '';
  formData.description = '';
  formData.status = 'planning';
  formData.priority = 'medium';
  formData.startDate = new Date().toISOString().split('T')[0];
  formData.endDate = '';
  formData.ownerId = '';
  formData.memberIds = [];
  formData.color = '#3b82f6';
  formData.tags = [];
  formData.progress = 0;
  formData.estimatedHours = 0;
  errors.name = '';
};

// Load project data for editing
const loadProjectData = () => {
  if (props.project) {
    formData.name = props.project.name;
    formData.description = props.project.description;
    formData.status = props.project.status;
    formData.priority = props.project.priority;
    formData.startDate = props.project.startDate;
    formData.endDate = props.project.endDate;
    formData.ownerId = props.project.ownerId;
    formData.memberIds = [...(props.project.memberIds || [])];
    formData.color = props.project.color || '#3b82f6';
    formData.tags = [...(props.project.tags || [])];
    formData.progress = props.project.progress;
    formData.estimatedHours = props.project.estimatedHours || calculateEstimatedHours();
  } else {
    resetForm();
  }
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
  errors.name = '';

  if (!formData.name.trim()) {
    errors.name = '请输入项目名称';
    return false;
  }

  if (formData.startDate && formData.endDate) {
    if (new Date(formData.startDate) > new Date(formData.endDate)) {
      alert('开始日期不能晚于结束日期');
      return false;
    }
  }

  if (!formData.ownerId) {
    alert('请选择项目负责人');
    return false;
  }

  return true;
};

// Handle submit
const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }

  saving.value = true;

  const projectData: Partial<Project> = {
    ...formData
  };

  emit('save', projectData);

  setTimeout(() => {
    saving.value = false;
    handleClose();
  }, 500);
};

// Handle close
const handleClose = () => {
  resetForm();
  emit('close');
};

// Watch for project changes
watch(() => props.project, loadProjectData);
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    loadProjectData();
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

// 监听团队成员变化，如果当前负责人不在选中列表中，则清空负责人选择
watch(() => formData.memberIds, (newMemberIds) => {
  if (formData.ownerId && !newMemberIds.includes(formData.ownerId)) {
    formData.ownerId = '';
  }
});
</script>
