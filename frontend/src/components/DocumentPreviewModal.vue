<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/75">
    <div class="relative w-full max-w-6xl max-h-screen overflow-auto rounded-lg bg-white">
      <button
        @click="$emit('close')"
        class="absolute right-4 top-4 z-10 rounded-full bg-white p-2 text-secondary-400 hover:text-secondary-600"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
        </svg>
      </button>

      <div v-if="loading" class="flex min-h-[400px] items-center justify-center">
        <div class="h-12 w-12 animate-spin rounded-full border-4 border-secondary-300 border-t-primary-600"></div>
      </div>

      <div v-else-if="error" class="flex min-h-[400px] items-center justify-center">
        <div class="text-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-16 w-16 text-red-500" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 8 8 0 0116 8zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm2-3.293a1 1 0 00-.707.293l-3-3a1 1 0 00-1.414 1.414l3 3a1 1 0 00.707-.293z" clip-rule="evenodd" />
          </svg>
          <h3 class="mb-2 text-lg font-semibold text-secondary-900">{{ $t('documents.previewFailed') }}</h3>
          <p class="text-secondary-500">{{ error }}</p>
          <Button variant="primary" class="mt-4" @click="handleDownload">
            {{ $t('documents.download') }}
          </Button>
        </div>
      </div>

      <div v-else-if="previewUrl" class="p-8">
        <div class="mb-6">
          <h2 class="text-2xl font-bold text-secondary-900">{{ document.name }}</h2>
          <div class="mt-2 flex flex-wrap items-center gap-4 text-sm text-secondary-500">
            <span>{{ document.fileName }}</span>
            <span>•</span>
            <span>{{ formatFileSize(document.fileSize) }}</span>
            <span>•</span>
            <span>{{ formatDate(document.createdAt) }}</span>
            <span>•</span>
            <span>{{ $t('documents.downloaded') }} {{ document.downloadCount }} {{ $t('documents.times') }}</span>
          </div>
        </div>

        <div v-if="document.description" class="mb-6 rounded-lg bg-secondary-50 p-4">
          <p class="text-secondary-700">{{ document.description }}</p>
        </div>

        <div v-if="document.fileType.toLowerCase().startsWith('image/')" class="rounded-lg border border-secondary-200">
          <img :src="previewUrl" :alt="document.name" class="max-h-[70vh] w-auto" />
        </div>

        <iframe
          v-else-if="document.fileType === 'application/pdf'"
          :src="previewUrl"
          class="h-[70vh] w-full rounded-lg border border-secondary-200"
          :title="$t('documents.pdfPreview')"
        ></iframe>

        <div v-else class="rounded-lg border-2 border-dashed border-secondary-300 p-12 text-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-16 w-16 text-secondary-400" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
          </svg>
          <h3 class="mb-2 text-lg font-semibold text-secondary-900">{{ $t('documents.cannotPreview') }}</h3>
          <p class="mb-4 text-secondary-500">{{ $t('documents.unsupportedFileType') }}</p>
          <Button variant="primary" @click="handleDownload">
            {{ $t('documents.download') }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiService from '@/services/api';
import type { Document } from '@/types';
import Button from '@/components/common/Button.vue';

const props = defineProps<{
  document: Document;
}>();

const emit = defineEmits<{
  close: [];
}>();

const loading = ref(false);
const error = ref('');
const previewUrl = ref('');

onMounted(async () => {
  await loadPreview();
});

async function loadPreview() {
  loading.value = true;
  error.value = '';

  try {
    const blob = await apiService.previewDocument(props.document.id);
    previewUrl.value = window.URL.createObjectURL(blob);
  } catch (err: any) {
    error.value = err.message || '加载预览失败';
  } finally {
    loading.value = false;
  }
}

function formatFileSize(bytes: number) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

async function handleDownload() {
  try {
    const blob = await apiService.downloadDocument(props.document.id);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = props.document.fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  } catch (err: any) {
    console.error('下载失败:', err);
    alert('下载失败，请重试');
  }
}
</script>
