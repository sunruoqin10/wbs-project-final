<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div class="w-full max-w-2xl rounded-lg bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-xl font-semibold text-secondary-900">{{ $t('documents.upload') }}</h2>
        <button @click="$emit('close')" class="rounded-full p-2 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>

      <div
        class="mb-4 rounded-lg border-2 border-dashed border-secondary-300 p-8 text-center transition-colors hover:border-primary-500"
        :class="{ 'border-primary-500 bg-primary-50': isDragging }"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
        @click="selectFile"
      >
        <input
          ref="fileInput"
          type="file"
          class="hidden"
          @change="handleFileSelect"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.gif"
        />
        <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-12 w-12 text-secondary-400" viewBox="0 0 20 20" fill="currentColor">
          <path d="M5.5 2a.5.5 0 01.5.5v5a.5.5 0 00.5.5h2a.5.5 0 000-1H6.5v-5a.5.5 0 01.5-.5zM5 10a.5.5 0 00-.5.5v1a.5.5 0 00.5.5h2a.5.5 0 000-1H5v-1a.5.5 0 01-.5-.5zm0 3a.5.5 0 00-.5.5v1a.5.5 0 00.5.5h2a.5.5 0 000-1H5v-1a.5.5 0 01-.5-.5zm0 3a.5.5 0 00-.5.5v1a.5.5 0 00.5.5h2a.5.5 0 000-1H5v-1a.5.5 0 01-.5-.5z" />
          <path d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2h-1V3a1 1 0 00-1-1h-2a1 1 0 00-1 1v0zM3 5a2 2 0 012-2V3a2 2 0 012-2h2a2 2 0 012 2v0h1a1 1 0 001 1v1h-1V5a2 2 0 00-2-2H4a2 2 0 00-2 2z" />
        </svg>
        <p class="text-lg font-medium text-secondary-900">{{ $t('documents.dragOrClick') }}</p>
        <p class="mt-2 text-sm text-secondary-500">{{ $t('documents.orClickSelect') }}</p>
        <p class="mt-4 text-xs text-secondary-400">{{ $t('documents.fileSizeLimit') }}</p>
      </div>

      <div v-if="selectedFile" class="mb-4 rounded-lg bg-secondary-50 p-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-100 text-primary-600">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
              </svg>
            </div>
            <div>
              <p class="font-medium text-secondary-900">{{ selectedFile.name }}</p>
              <p class="text-sm text-secondary-500">{{ formatFileSize(selectedFile.size) }}</p>
            </div>
          </div>
          <button @click="removeFile" class="rounded p-1 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>

      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.name') }}</label>
          <input
            v-model="formData.name"
            type="text"
            :placeholder="$t('documents.namePlaceholder')"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">{{ $t('documents.category') }}</label>
          <select
            v-model="formData.category"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 focus:border-primary-500 focus:outline-none"
          >
            <option value="">{{ $t('documents.selectCategory') }}</option>
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
            :placeholder="$t('documents.descriptionPlaceholder')"
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
          @click="handleUpload"
          :disabled="!selectedFile || uploading"
          :loading="uploading"
        >
          {{ uploading ? $t('documents.uploading') : $t('documents.upload') }}
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import apiService from '@/services/api';
import type { Document } from '@/types';
import { useUserStore } from '@/stores/user';
import Button from '@/components/common/Button.vue';

defineProps<{
  projects: any[];
}>();

const emit = defineEmits<{
  close: [];
  uploaded: [document: Document];
}>();

const userStore = useUserStore();
const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const isDragging = ref(false);
const uploading = ref(false);
const error = ref('');

const formData = ref({
  name: '',
  category: '',
  projectId: '',
  description: '',
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

function selectFile() {
  fileInput.value?.click();
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    validateAndSetFile(file);
  }
}

function handleDrop(event: DragEvent) {
  isDragging.value = false;
  const file = event.dataTransfer?.files[0];
  if (file) {
    validateAndSetFile(file);
  }
}

function validateAndSetFile(file: File) {
  error.value = '';

  if (file.size > 10 * 1024 * 1024) {
    error.value = '文件大小不能超过 10MB';
    return;
  }

  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'image/jpeg',
    'image/png',
    'image/gif',
  ];

  if (!allowedTypes.includes(file.type)) {
    error.value = '不支持的文件类型';
    return;
  }

  selectedFile.value = file;
  formData.value.name = file.name.replace(/\.[^/.]+$/, '');
}

function removeFile() {
  selectedFile.value = null;
  formData.value.name = '';
  error.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

async function handleUpload() {
  if (!selectedFile.value || uploading.value) return;

  uploading.value = true;
  error.value = '';

  try {
    const formDataObj = new FormData();
    formDataObj.append('file', selectedFile.value);
    formDataObj.append('name', formData.value.name);
    formDataObj.append('category', formData.value.category);
    formDataObj.append('projectId', formData.value.projectId);
    formDataObj.append('description', formData.value.description);

    const document = await apiService.uploadDocument(formDataObj);
    emit('uploaded', document);
  } catch (err: any) {
    error.value = err.message || '上传失败，请重试';
  } finally {
    uploading.value = false;
  }
}

function formatFileSize(bytes: number) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
}
</script>
