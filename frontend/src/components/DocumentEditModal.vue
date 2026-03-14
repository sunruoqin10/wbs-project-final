<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div class="w-full max-w-2xl rounded-lg bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-xl font-semibold text-secondary-900">{{ $t('documents.edit') }}</h2>
        <button @click="$emit('close')" class="rounded-full p-2 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>

      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.name') }}</label>
          <input
            v-model="formData.name"
            type="text"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.category') }}</label>
          <select
            v-model="formData.category"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          >
            <option v-for="category in categories" :key="category.value" :value="category.value">
              {{ category.label }}
            </option>
          </select>
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.project') }}</label>
          <select
            v-model="formData.projectId"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          >
            <option value="">{{ $t('documents.selectProject') }}</option>
            <option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </option>
          </select>
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.description') }}</label>
          <textarea
            v-model="formData.description"
            rows="3"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          />
        </div>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
        {{ error }}
      </div>

      <div class="flex justify-end gap-3">
        <Button variant="ghost" @click="$emit('close')">
          {{ $t('common.cancel') }}
        </Button>
        <Button
          variant="primary"
          @click="handleUpdate"
          :disabled="updating"
          :loading="updating"
        >
          {{ updating ? $t('documents.updating') : $t('documents.update') }}
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import apiService from '@/services/api';
import type { Document } from '@/types';
import Button from '@/components/common/Button.vue';

const props = defineProps<{
  document: Document;
  projects: any[];
}>();

const emit = defineEmits<{
  close: [];
  updated: [document: Document];
}>();

const updating = ref(false);
const error = ref('');

const formData = ref({
  name: props.document.name,
  category: props.document.category,
  projectId: props.document.projectId || '',
  description: props.document.description || '',
});

const categories = [
  { value: 'requirements', label: '需求文档' },
  { value: 'design', label: '设计文档' },
  { value: 'development', label: '开发文档' },
  { value: 'testing', label: '测试文档' },
  { value: 'deployment', label: '部署文档' },
  { value: 'documentation', label: '用户手册' },
  { value: 'other', label: '其他' },
];

watch(() => props.document, (newDoc) => {
  formData.value = {
    name: newDoc.name,
    category: newDoc.category,
    projectId: newDoc.projectId || '',
    description: newDoc.description || '',
  };
});

async function handleUpdate() {
  updating.value = true;
  error.value = '';

  try {
    const updated = await apiService.updateDocument(props.document.id, {
      name: formData.value.name,
      category: formData.value.category,
      projectId: formData.value.projectId || undefined,
      description: formData.value.description,
    });
    emit('updated', updated);
  } catch (err: any) {
    error.value = err.message || '更新失败，请重试';
  } finally {
    updating.value = false;
  }
}
</script>
