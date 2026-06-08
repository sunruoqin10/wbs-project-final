<template>
  <Modal
    :open="open"
    :title="isEditing ? $t('projectForm.editTitle') : $t('projectForm.createTitle')"
    size="2xl"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="space-y-5">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Input
          v-model="formData.name"
          :label="$t('projectForm.name')"
          :placeholder="$t('projectForm.namePlaceholder')"
          :error="errors.name"
          required
        />
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('projectForm.description') }}</label>
          <textarea
            v-model="formData.description"
            rows="3"
            class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
            :placeholder="$t('projectForm.descriptionPlaceholder')"
          ></textarea>
        </div>
      </div>

      <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Select v-model="formData.status" :label="$t('projectForm.status')" required disabled>
          <option value="planning">{{ $t('projectForm.statusOptions.planning') }}</option>
          <option value="active">{{ $t('projectForm.statusOptions.active') }}</option>
          <option value="completed">{{ $t('projectForm.statusOptions.completed') }}</option>
          <option value="on-hold">{{ $t('projectForm.statusOptions.onHold') }}</option>
          <option value="cancelled">{{ $t('projectForm.statusOptions.cancelled') }}</option>
        </Select>

        <Select v-model="formData.priority" :label="$t('projectForm.priority')" required>
          <option value="low">{{ $t('projectForm.priorityOptions.low') }}</option>
          <option value="medium">{{ $t('projectForm.priorityOptions.medium') }}</option>
          <option value="high">{{ $t('projectForm.priorityOptions.high') }}</option>
          <option value="critical">{{ $t('projectForm.priorityOptions.critical') }}</option>
        </Select>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ $t('projectForm.startDate') }} *
            <span v-if="isEditing" class="text-xs text-secondary-500 font-normal ml-1">{{ $t('projectForm.notEditable') }}</span>
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
            {{ $t('projectForm.endDate') }} *
            <span v-if="isEditing" class="text-xs text-secondary-500 font-normal ml-1">{{ $t('projectForm.notEditable') }}</span>
          </label>
          <input
            v-model="formData.endDate"
            type="date"
            :disabled="isEditing"
            :class="[
              'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
              errors.endDate ? 'border-danger-300' : '',
              isEditing ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200' : 'border-secondary-200'
            ]"
            required
          />
          <p v-if="errors.endDate" class="mt-1 text-xs text-danger-600">{{ errors.endDate }}</p>
        </div>
      </div>

      <div v-if="isEditing" class="rounded-lg bg-info-50 border border-info-200 px-3 py-2">
        <div class="flex gap-2 items-start">
          <svg class="h-4 w-4 text-info-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="text-xs text-info-800 leading-relaxed">
            {{ $t('projectForm.dateInfoTip') }}
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ $t('projectForm.owner') }} *
            <span v-if="!hasTeamMembers" class="text-xs text-secondary-500 font-normal ml-1">{{ $t('projectForm.ownerHint') }}</span>
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
              {{ hasTeamMembers ? $t('projectForm.ownerSelectPlaceholder') : $t('projectForm.ownerEmptyPlaceholder') }}
            </option>
            <option v-for="user in availableOwners" :key="user.id" :value="user.id">
              {{ user.name }}
            </option>
          </select>
          <p v-if="!hasTeamMembers" class="mt-1 text-xs text-secondary-500">{{ $t('projectForm.ownerEmptyHint') }}</p>
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ $t('projectForm.estimatedHoursLabel') }}: {{ formData.estimatedHours || 0 }} {{ $t('projectForm.estimatedHoursUnit') }}
          </label>
          <input
            v-model.number="formData.estimatedHours"
            type="number"
            class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60"
            disabled
          />
          <p class="mt-1 text-xs text-secondary-500">{{ $t('projectForm.estimatedHoursHint') }}</p>
        </div>
      </div>

      <div>
        <label class="mb-2 block text-sm font-medium text-secondary-700">{{ $t('projectForm.members') }}</label>
        <SearchableMultiSelect v-model="formData.memberIds" />
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-secondary-700">{{ $t('projectForm.color') }}</label>
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
          <label class="mb-2 block text-sm font-medium text-secondary-700">{{ $t('projectForm.tags') }}</label>
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
              {{ $t('projectForm.progressLabel') }}: {{ formData.progress }}%
              <span class="text-xs text-secondary-500 font-normal ml-1">{{ $t('projectForm.progressHint') }}</span>
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
        <Button variant="secondary" @click="handleClose">{{ $t('projectForm.cancelButton') }}</Button>
        <Button variant="primary" @click="handleSubmit" :loading="saving">
          {{ isEditing ? $t('projectForm.updateButton') : $t('projectForm.createButton') }}
        </Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import Modal from '@/components/common/Modal.vue';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Select from '@/components/common/Select.vue';
import SearchableMultiSelect from '@/components/common/SearchableMultiSelect.vue';
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
const { t } = useI18n();

const users = computed(() => userStore.users);
const isEditing = computed(() => !!props.project);
const saving = ref(false);

const hasTeamMembers = computed(() => formData.memberIds.length > 0);
const availableOwners = computed(() => {
  return users.value.filter(user => formData.memberIds.includes(user.id));
});

const commonTags = computed(() => t('projectForm.commonTags') as unknown as string[]);

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
  name: '',
  endDate: ''
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
  errors.endDate = '';
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
  errors.endDate = '';

  if (!formData.name.trim()) {
    errors.name = t('projectForm.validation.nameRequired');
    return false;
  }

  if (!formData.endDate) {
    errors.endDate = t('projectForm.validation.endDateRequired');
    return false;
  }

  if (formData.startDate && formData.endDate) {
    if (new Date(formData.startDate) > new Date(formData.endDate)) {
      errors.endDate = t('projectForm.validation.endDateInvalid');
      return false;
    }
  }

  if (!formData.ownerId) {
    alert(t('projectForm.validation.ownerRequired'));
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
