<template>
  <div class="space-y-4">
    <div class="flex flex-col gap-4 md:flex-row md:items-center">
      <div class="flex-1">
        <label class="mb-2 block text-sm font-medium text-secondary-700">
          {{ $t('documents.project') }}
        </label>
        <select
          v-model="selectedProject"
          class="w-full rounded-lg border border-secondary-300 px-3 py-2.5 text-sm font-medium text-secondary-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white"
        >
          <option value="">{{ $t('documents.allProjects') }}</option>
          <option v-for="project in projects" :key="project.id" :value="project.id">
            {{ project.name }}
          </option>
        </select>
      </div>

      <div class="flex-1">
        <label class="mb-2 block text-sm font-medium text-secondary-700">
          {{ $t('documents.category') }}
        </label>
        <select
          v-model="selectedCategory"
          class="w-full rounded-lg border border-secondary-300 px-3 py-2.5 text-sm font-medium text-secondary-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white"
        >
          <option value="">{{ $t('documents.allCategories') }}</option>
          <option v-for="category in categories" :key="category.value" :value="category.value">
            {{ category.label }}
          </option>
        </select>
      </div>

      <div class="flex-1">
        <label class="mb-2 block text-sm font-medium text-secondary-700">
          {{ $t('documents.searchPlaceholder') }}
        </label>
        <div class="relative">
          <input
            v-model="searchQuery"
            :placeholder="$t('documents.searchPlaceholder')"
            class="w-full rounded-lg border border-secondary-300 px-4 py-2.5 pl-10 text-sm text-secondary-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
          />
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      <div class="flex items-end gap-2 pb-2">
        <Button
          v-if="selectedProject || selectedCategory || searchQuery"
          variant="secondary"
          size="sm"
          @click="clearFilters"
        >
          {{ $t('documents.clearFilters') }}
        </Button>
        <div class="flex items-center rounded-lg bg-secondary-100 p-1">
          <button
            @click="viewMode = 'grid'"
            :class="['rounded-md p-2 transition-all', viewMode === 'grid' ? 'bg-white text-primary-600 shadow-sm' : 'text-secondary-600 hover:bg-white/50']"
            :title="$t('documents.viewModes.grid')"
          >
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </button>
          <button
            @click="viewMode = 'list'"
            :class="['rounded-md p-2 transition-all', viewMode === 'list' ? 'bg-white text-primary-600 shadow-sm' : 'text-secondary-600 hover:bg-white/50']"
            :title="$t('documents.viewModes.list')"
          >
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
          </button>
        </div>
        <Button v-if="permissionStore.canCreateDocument()" variant="primary" @click="showUploadModal = true">
          <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('documents.upload') }}
        </Button>
      </div>
    </div>

    <div v-if="selectedProject || selectedCategory || searchQuery" class="flex flex-wrap items-center gap-2 rounded-lg bg-secondary-50 px-4 py-2 text-sm">
      <span class="font-medium text-secondary-600">{{ $t('documents.activeFilters') }}:</span>
      <Badge v-if="searchQuery" variant="secondary" class="flex items-center gap-1">
        <span>{{ $t('documents.search') }}: {{ searchQuery }}</span>
        <button @click="searchQuery = ''" class="ml-1 rounded-full p-0.5 hover:bg-secondary-200">
          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </Badge>
      <Badge v-if="selectedProject" variant="secondary" class="flex items-center gap-1">
        <span>{{ getProjectName(selectedProject) }}</span>
        <button @click="selectedProject = ''" class="ml-1 rounded-full p-0.5 hover:bg-secondary-200">
          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </Badge>
      <Badge v-if="selectedCategory" variant="secondary" class="flex items-center gap-1">
        <span>{{ getCategoryLabel(selectedCategory) }}</span>
        <button @click="selectedCategory = ''" class="ml-1 rounded-full p-0.5 hover:bg-secondary-200">
          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </Badge>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-4 border-secondary-300 border-t-primary-600"></div>
    </div>

    <div v-else-if="error" class="rounded-lg bg-red-50 p-6 text-center">
      <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-12 w-12 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="text-red-600 font-medium">{{ error }}</p>
      <button
        @click="loadData"
        class="mt-4 rounded-lg bg-red-100 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-200"
      >
        重试
      </button>
    </div>

    <div v-else-if="filteredDocuments.length === 0" class="text-center py-12 text-secondary-500">
      <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-16 w-16 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" />
      </svg>
      <p>{{ $t('documents.noDocuments') }}</p>
    </div>

    <div v-else>
      <div class="mb-4 flex items-center justify-between text-sm text-secondary-500">
        <span>{{ $t('documents.total') }}: <span class="font-semibold text-primary-600">{{ filteredDocuments.length }}</span> {{ $t('documents.documents') }}</span>
        <span>{{ $t('documents.totalSize') }}: <span class="font-semibold text-primary-600">{{ totalSize }}</span></span>
      </div>

      <div v-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="doc in filteredDocuments"
          :key="doc.id"
          class="border border-secondary-200 rounded-lg p-4 hover:border-primary-500 hover:shadow-md transition-all"
        >
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0">
              <div
                v-if="doc.fileType?.startsWith('image/')"
                class="h-16 w-16 rounded-lg bg-secondary-100 flex items-center justify-center overflow-hidden"
              >
                <img :src="`${apiBaseUrl}/documents/${doc.id}/preview`" :alt="doc.name" class="h-full w-full object-cover" />
              </div>
              <div v-else class="h-16 w-16 rounded-lg bg-secondary-100 flex items-center justify-center text-secondary-400">
                <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-secondary-900 truncate" :title="doc.name">{{ doc.name }}</p>
              <p class="text-xs text-secondary-500">{{ getProjectName(doc.projectId) || '-' }}</p>
              <p class="text-xs text-secondary-500">{{ formatFileSize(doc.fileSize) }}</p>
            </div>
          </div>
          <div class="mt-3 flex items-center justify-between">
            <Badge :variant="getCategoryVariant(doc.category)">{{ getCategoryLabel(doc.category) }}</Badge>
            <div class="flex items-center gap-1">
              <button
                v-if="canPreview(doc.fileType)"
                @click="openPreview(doc)"
                class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600"
                :title="$t('documents.preview')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
              </button>
              <button
                @click="downloadDocument(doc)"
                class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600"
                :title="$t('documents.download')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </button>
              <button
                v-if="permissionStore.canEditDocument(doc.uploadedBy)"
                @click="deleteDocument(doc)"
                class="p-1.5 rounded hover:bg-secondary-100 text-danger-500"
                :title="$t('documents.delete')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="border border-secondary-200 rounded-lg overflow-hidden">
        <table class="w-full">
          <thead class="bg-secondary-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.name') }}</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.category') }}</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.project') }}</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.fileSize') }}</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-semibold uppercase text-secondary-500">{{ $t('documents.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-secondary-100">
            <tr v-for="doc in filteredDocuments" :key="doc.id" class="group transition-colors hover:bg-secondary-50">
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary-100 text-secondary-400">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div>
                    <p class="font-medium text-secondary-900">{{ doc.name }}</p>
                    <p class="text-sm text-secondary-500">{{ doc.fileName }}</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3">
                <Badge :variant="getCategoryVariant(doc.category)">{{ getCategoryLabel(doc.category) }}</Badge>
              </td>
              <td class="px-4 py-3 text-sm text-secondary-600">{{ getProjectName(doc.projectId) || '-' }}</td>
              <td class="px-4 py-3 text-sm text-secondary-600">{{ formatFileSize(doc.fileSize) }}</td>
              <td class="px-4 py-3 text-sm text-secondary-600">{{ formatDate(doc.createdAt) }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center justify-end gap-1">
                  <button v-if="canPreview(doc.fileType)" @click="openPreview(doc)" class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600" :title="$t('documents.preview')">
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                  <button @click="downloadDocument(doc)" class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600" :title="$t('documents.download')">
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </button>
                  <button v-if="permissionStore.canEditDocument(doc.uploadedBy)" @click="deleteDocument(doc)" class="p-1.5 rounded hover:bg-secondary-100 text-danger-500" :title="$t('documents.delete')">
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <DocumentUploadModal
      v-if="showUploadModal"
      :projects="projects"
      @close="showUploadModal = false"
      @uploaded="handleDocumentUploaded"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import DocumentUploadModal from '@/components/DocumentUploadModal.vue';
import { useProjectStore } from '@/stores/project';
import { usePermissionStore } from '@/stores/permission';
import apiService from '@/services/api';
import type { Document } from '@/types';
import dayjs from 'dayjs';

const { t } = useI18n();
const projectStore = useProjectStore();
const permissionStore = usePermissionStore();

const loading = ref(true);
const error = ref('');
const documents = ref<Document[]>([]);
const projects = ref<any[]>([]);
const selectedProject = ref('');
const selectedCategory = ref('');
const searchQuery = ref('');
const viewMode = ref<'grid' | 'list'>('grid');
const showUploadModal = ref(false);

const apiBaseUrl = computed(() => import.meta.env.VITE_API_BASE_URL || '/api');

const categories = [
  { value: 'requirements', label: t('documents.categories.requirements') },
  { value: 'design', label: t('documents.categories.design') },
  { value: 'development', label: t('documents.categories.development') },
  { value: 'testing', label: t('documents.categories.testing') },
  { value: 'deployment', label: t('documents.categories.deployment') },
  { value: 'documentation', label: t('documents.categories.documentation') },
  { value: 'other', label: t('documents.categories.other') }
];

const filteredDocuments = computed(() => {
  let result = documents.value.filter(doc => doc.status === 'active' && doc.projectId && !doc.reportId);

  if (selectedProject.value) {
    result = result.filter(doc => doc.projectId === selectedProject.value);
  }

  if (selectedCategory.value) {
    result = result.filter(doc => doc.category === selectedCategory.value);
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(doc =>
      doc.name.toLowerCase().includes(query) ||
      doc.fileName.toLowerCase().includes(query) ||
      (doc.description && doc.description.toLowerCase().includes(query))
    );
  }

  return result.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
});

const totalSize = computed(() => {
  const total = filteredDocuments.value.reduce((sum, doc) => sum + (doc.fileSize || 0), 0);
  return formatFileSize(total);
});

onMounted(async () => {
  console.log('[ProjectDocuments] 组件已挂载，开始加载数据...');
  await loadData();
});

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    console.log('[ProjectDocuments] 加载项目列表...');
    projects.value = [];
    await projectStore.loadProjects();
    projects.value = projectStore.projects;
    console.log('[ProjectDocuments] 项目列表加载完成:', projects.value.length, '个项目');

    console.log('[ProjectDocuments] 加载文档列表...');
    documents.value = await apiService.getAllDocuments();
    console.log('[ProjectDocuments] 文档列表加载完成:', documents.value.length, '个文档');
  } catch (err: any) {
    console.error('[ProjectDocuments] 加载数据失败:', err);
    error.value = err.message || '加载数据失败，请检查网络连接或重新登录';
    documents.value = [];
    projects.value = [];
  } finally {
    loading.value = false;
  }
}

function getProjectName(projectId: string): string {
  if (!projects.value || !projectId) return '';
  const project = projects.value.find(p => p.id === projectId);
  return project?.name || '';
}

function getCategoryLabel(category: string): string {
  const cat = categories.find(c => c.value === category);
  return cat ? cat.label : category;
}

function getCategoryVariant(category: string): 'primary' | 'secondary' | 'accent' | 'danger' {
  const variantMap: Record<string, 'primary' | 'secondary' | 'accent' | 'danger'> = {
    requirements: 'primary',
    design: 'secondary',
    development: 'accent',
    testing: 'accent',
    deployment: 'danger',
    documentation: 'secondary',
    other: 'secondary'
  };
  return variantMap[category] || 'secondary';
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  return dayjs(dateStr).format('YYYY-MM-DD');
}

function canPreview(fileType: string): boolean {
  if (!fileType) return false;
  return fileType.startsWith('image/') || fileType === 'application/pdf';
}

function openPreview(doc: Document) {
  window.open(`${apiBaseUrl.value}/documents/${doc.id}/preview`, '_blank');
}

async function downloadDocument(doc: Document) {
  try {
    const blob = await apiService.downloadDocument(doc.id);
    const url = window.URL.createObjectURL(blob);
    const a = window.document.createElement('a');
    a.href = url;
    a.download = doc.fileName;
    window.document.body.appendChild(a);
    a.click();
    window.document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('下载文档失败:', error);
    alert(t('documents.downloadError'));
  }
}

async function deleteDocument(doc: Document) {
  if (!confirm(t('documents.deleteConfirm', { name: doc.name }))) return;
  try {
    await apiService.deleteDocument(doc.id);
    documents.value = documents.value.filter(d => d.id !== doc.id);
  } catch (error) {
    console.error('删除文档失败:', error);
    alert(t('documents.deleteError'));
  }
}

function handleDocumentUploaded(doc: Document) {
  documents.value.unshift(doc);
  showUploadModal.value = false;
}

function clearFilters() {
  selectedProject.value = '';
  selectedCategory.value = '';
  searchQuery.value = '';
}
</script>
