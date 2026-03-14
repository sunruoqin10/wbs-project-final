<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('routes.documents') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('documents.subtitle') }}</p>
        </div>
        <div class="flex items-center gap-3">
          <div class="flex items-center rounded-lg bg-secondary-100 p-1">
            <button
              @click="viewMode = 'grid'"
              :class="[
                'rounded-md p-2 transition-all',
                viewMode === 'grid' ? 'bg-white text-primary-600 shadow-sm' : 'text-secondary-600 hover:bg-white/50'
              ]"
              title="卡片视图"
            >
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
            </button>
            <button
              @click="viewMode = 'list'"
              :class="[
                'rounded-md p-2 transition-all',
                viewMode === 'list' ? 'bg-white text-primary-600 shadow-sm' : 'text-secondary-600 hover:bg-white/50'
              ]"
              title="列表视图"
            >
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
              </svg>
            </button>
          </div>
          <Button v-if="permissionStore.canCreateDocument()" variant="primary" @click="showUploadModal = true">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ $t('documents.upload') }}
          </Button>
        </div>
      </div>

      <Card class="p-6">
            <div class="space-y-4">
              <div class="flex flex-col gap-4 md:flex-row md:items-center">
                <div class="flex-1">
                  <label class="mb-2 block text-sm font-medium text-secondary-700">
                    {{ $t('documents.searchPlaceholder') }}
                  </label>
                  <div class="relative group">
                    <input
                      v-model="searchQuery"
                      :placeholder="$t('documents.searchPlaceholder')"
                      class="w-full rounded-xl border-2 border-secondary-200 px-4 py-3 pl-12 text-sm text-secondary-900 placeholder:text-secondary-400 focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-500/10 transition-all duration-200 group-hover:border-secondary-300"
                    />
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-secondary-400 transition-colors group-hover:text-secondary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1">
                      <button
                        v-if="searchQuery"
                        @click="searchQuery = ''"
                        class="rounded-full p-1 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600 transition-colors"
                        title="清除搜索"
                      >
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>



                <div class="flex-1">
                  <label class="mb-2 block text-sm font-medium text-secondary-700">
                    {{ $t('documents.project') }}
                  </label>
                  <select
                    v-model="selectedProject"
                    class="w-full rounded-lg border border-secondary-300 px-3 py-2.5 text-sm font-medium text-secondary-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white transition-colors"
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
                    class="w-full rounded-lg border border-secondary-300 px-3 py-2.5 text-sm font-medium text-secondary-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white transition-colors"
                  >
                    <option value="">{{ $t('documents.allCategories') }}</option>
                    <option v-for="category in categories" :key="category.value" :value="category.value">
                      {{ category.label }}
                    </option>
                  </select>
                </div>

                <div class="flex items-end pb-2">
                  <Button
                    v-if="selectedProject || selectedCategory || searchQuery"
                    variant="secondary"
                    size="sm"
                    @click="clearFilters"
                    class="flex items-center gap-2"
                  >
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                    {{ $t('documents.clearFilters') }}
                  </Button>
                </div>
              </div>

              <div
                v-if="selectedProject || selectedCategory || searchQuery"
                class="flex flex-wrap items-center gap-2 rounded-lg bg-secondary-50 px-4 py-2 text-sm"
              >
                <span class="font-medium text-secondary-600">{{ $t('documents.activeFilters') }}:</span>
                <Badge v-if="searchQuery" variant="secondary" class="flex items-center gap-1">
                  <span>{{ $t('documents.search') }}: {{ searchQuery }}</span>
                  <button
                    @click="searchQuery = ''"
                    class="ml-1 rounded-full p-0.5 hover:bg-secondary-200"
                  >
                    <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </Badge>
                <Badge v-if="selectedProject" variant="secondary" class="flex items-center gap-1">
                  <span>{{ getProjectName(selectedProject) }}</span>
                  <button
                    @click="selectedProject = ''"
                    class="ml-1 rounded-full p-0.5 hover:bg-secondary-200"
                  >
                    <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </Badge>
                <Badge v-if="selectedCategory" variant="secondary" class="flex items-center gap-1">
                  <span>{{ getCategoryLabel(selectedCategory) }}</span>
                  <button
                    @click="selectedCategory = ''"
                    class="ml-1 rounded-full p-0.5 hover:bg-secondary-200"
                  >
                    <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </Badge>
              </div>
            </div>
          </Card>

      <Transition name="fade" mode="out-in">
        <div v-if="loading" key="loading" class="flex items-center justify-center py-12">
          <div class="h-12 w-12 animate-spin rounded-full border-4 border-secondary-300 border-t-primary-600"></div>
        </div>

        <div v-else-if="filteredDocuments.length === 0" key="empty" class="py-12 text-center text-secondary-500">
          <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto mb-4 h-16 w-16 text-secondary-400" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
          </svg>
          <p>{{ $t('documents.noDocuments') }}</p>
        </div>

        <div
          v-else-if="viewMode === 'grid'"
          key="documents-grid"
          class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3"
        >
          <Card
            v-for="document in filteredDocuments"
            :key="document.id"
            class="group cursor-pointer transition-all hover:shadow-md"
            @click="openPreview(document)"
          >
            <div class="mb-4 flex items-start justify-between">
              <div class="flex-1">
                <h3 class="truncate text-lg font-semibold text-secondary-900">{{ document.name }}</h3>
                <p class="text-sm text-secondary-500">{{ document.fileName }}</p>
              </div>
              <div class="relative">
                <button
                  v-if="permissionStore.canEditDocument(document.uploadedBy)"
                  @click.stop="openActionMenu(event, document)"
                  class="rounded-lg p-2 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 6a2 2 0 100-4 2 2 0 000 4z" />
                    <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.278 4.058-5.968 7-9.542 7S1.732 14.057.458 10zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="mb-3 flex flex-wrap gap-2">
              <Badge :variant="getCategoryVariant(document.category)">
                {{ getCategoryLabel(document.category) }}
              </Badge>
              <Badge
                v-if="document.version > 1"
                variant="secondary"
                size="sm"
              >
                v{{ document.version }}
              </Badge>
            </div>

            <div class="mb-3 text-sm text-secondary-500">
              <p v-if="document.description" class="line-clamp-2">{{ document.description }}</p>
            </div>

            <div class="flex items-center justify-between text-sm text-secondary-500">
              <div class="flex items-center gap-2">
                <span>{{ formatFileSize(document.fileSize) }}</span>
                <span>•</span>
                <span>{{ formatDate(document.createdAt) }}</span>
              </div>
              <div class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.278 4.058-5.968 7-9.542 7S1.732 14.057.458 10zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd" />
                </svg>
                <span>{{ document.downloadCount }}</span>
              </div>
            </div>

            <div class="mt-4 flex gap-2">
              <Button
                variant="secondary"
                size="sm"
                @click.stop="downloadDocument(document)"
                class="flex-1"
              >
                <svg class="mr-1.5 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                {{ $t('documents.download') }}
              </Button>
              <Button
                v-if="canPreview(document.fileType)"
                variant="secondary"
                size="sm"
                @click.stop="openPreview(document)"
                class="flex-1"
              >
                <svg class="mr-1.5 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path d="M2.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.278 4.058-5.968 7-9.542 7S1.732 14.057.458 10zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd" />
                </svg>
                {{ $t('documents.preview') }}
              </Button>
            </div>
          </Card>
        </div>

        <Card
          v-else-if="viewMode === 'list'"
          key="documents-list"
          class="p-0"
        >
          <table class="w-full">
            <thead class="bg-secondary-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.name') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.category') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.project') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.fileSize') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.createdAt') }}
                </th>
                <th class="px-6 py-3 text-right text-xs font-semibold uppercase text-secondary-500">
                  {{ $t('documents.actions') }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-100">
              <tr
                v-for="document in filteredDocuments"
                :key="document.id"
                class="group transition-colors hover:bg-secondary-50"
              >
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-100 text-primary-600">
                      <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                      </svg>
                    </div>
                    <div>
                      <p class="font-medium text-secondary-900">{{ document.name }}</p>
                      <p class="text-sm text-secondary-500">{{ document.fileName }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <Badge :variant="getCategoryVariant(document.category)">
                    {{ getCategoryLabel(document.category) }}
                  </Badge>
                </td>
                <td class="px-6 py-4 text-sm text-secondary-600">
                  {{ getProjectName(document.projectId) || '-' }}
                </td>
                <td class="px-6 py-4 text-sm text-secondary-600">
                  {{ formatFileSize(document.fileSize) }}
                </td>
                <td class="px-6 py-4 text-sm text-secondary-600">
                  {{ formatDate(document.createdAt) }}
                </td>
                <td class="px-6 py-4">
                  <div class="flex items-center justify-end gap-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      @click="downloadDocument(document)"
                    >
                      <svg class="h-4 w-4 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5m0 14l-4-4m4 4l4-4" />
                      </svg>
                    </Button>
                    <Button
                      v-if="canPreview(document.fileType)"
                      variant="secondary"
                      size="sm"
                      @click="openPreview(document)"
                    >
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path d="M2.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.278 4.058-5.968 7-9.542 7S1.732 14.057.458 10zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    <button
                      v-if="permissionStore.canEditDocument(document.uploadedBy)"
                      @click.stop="editDocument(document)"
                      class="rounded-lg p-2 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600"
                    >
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-5" />
                        <path d="M13.586 3.586a1 1 0 011.414 1.414L20.586 6.586a1 1 0 011.414 1.414L13.414 10l-2.586 2.586a1 1 0 01-1.414-1.414L10.586 6.586a1 1 0 00-1.414-1.414L8.586 10l2.586 2.586a1 1 0 01-1.414 1.414z" />
                      </svg>
                    </button>
                    <button
                      v-if="permissionStore.canDeleteDocument(document.uploadedBy)"
                      @click.stop="deleteDocument(document)"
                      class="rounded-lg p-2 text-danger-500 hover:bg-danger-100 hover:text-danger-700 transition-colors"
                    >
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </Card>
      </Transition>
    </div>

    <DocumentUploadModal
      v-if="showUploadModal"
      :projects="projects"
      @close="showUploadModal = false"
      @uploaded="handleDocumentUploaded"
    />

    <DocumentEditModal
      v-if="showEditModal"
      :document="selectedDocument"
      :projects="projects"
      @close="showEditModal = false"
      @updated="handleDocumentUpdated"
    />

    <DocumentPreviewModal
      v-if="showPreviewModal"
      :document="selectedDocument"
      @close="showPreviewModal = false"
    />

    <div
      v-if="showActionMenu && actionMenuDocument"
      class="fixed inset-0 z-50 bg-black bg-opacity-50"
      @click="closeActionMenu"
    >
      <div class="absolute rounded-lg bg-white shadow-lg p-2" :style="actionMenuStyle">
        <Button
          v-if="permissionStore.canEditDocument(actionMenuDocument?.uploadedBy)"
          variant="ghost"
          size="sm"
          @click="editDocument(actionMenuDocument)"
          class="w-full justify-start"
        >
          <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-5a1 1 0 10-2 2 2 0 00-2-2H9a1 1 0 00-1 1z" />
            <path d="M13.586 3.586a1 1 0 011.414 1.414L20.586 6.586a1 1 0 011.414 1.414L13.414 10l-2.586 2.586a1 1 0 01-1.414-1.414L10.586 6.586a1 1 0 00-1.414-1.414L8.586 10l2.586 2.586a1 1 0 01-1.414 1.414z" />
          </svg>
          {{ $t('documents.edit') }}
        </Button>
        <Button
          v-if="permissionStore.canDeleteDocument(actionMenuDocument?.uploadedBy)"
          variant="danger"
          size="sm"
          @click="deleteDocument(actionMenuDocument)"
          class="w-full justify-start mt-2"
        >
          <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 6h18v12H3V6zm9 5a2 2 0 100-4 2 2 0 000 4z" />
          </svg>
          {{ $t('documents.delete') }}
        </Button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import DocumentUploadModal from '@/components/DocumentUploadModal.vue';
import DocumentEditModal from '@/components/DocumentEditModal.vue';
import DocumentPreviewModal from '@/components/DocumentPreviewModal.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import apiService from '@/services/api';
import type { Document } from '@/types';
import dayjs from 'dayjs';

const { t } = useI18n();
const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const documents = ref<Document[]>([]);
const projects = ref<any[]>([]);
const loading = ref(false);
const searchQuery = ref('');
const selectedProject = ref('');
const selectedCategory = ref('');
const viewMode = ref<'grid' | 'list'>('grid');
const showUploadModal = ref(false);
const showEditModal = ref(false);
const showPreviewModal = ref(false);
const selectedDocument = ref<Document | null>(null);

const showActionMenu = ref(false);
const actionMenuDocument = ref<Document | null>(null);
const actionMenuPosition = ref({ x: 0, y: 0 });

const categories = [
  { value: 'requirements', label: t('documents.categories.requirements') },
  { value: 'design', label: t('documents.categories.design') },
  { value: 'development', label: t('documents.categories.development') },
  { value: 'testing', label: t('documents.categories.testing') },
  { value: 'deployment', label: t('documents.categories.deployment') },
  { value: 'documentation', label: t('documents.categories.documentation') },
  { value: 'other', label: t('documents.categories.other') },
];

const filteredDocuments = computed(() => {
  let result = documents.value.filter(doc => doc.status === 'active');

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(doc =>
      doc.name.toLowerCase().includes(query) ||
      doc.fileName.toLowerCase().includes(query) ||
      (doc.description && doc.description.toLowerCase().includes(query))
    );
  }

  if (selectedProject.value) {
    result = result.filter(doc => doc.projectId === selectedProject.value);
  }

  if (selectedCategory.value) {
    result = result.filter(doc => doc.category === selectedCategory.value);
  }

  return result;
});

const actionMenuStyle = computed(() => ({
  left: `${actionMenuPosition.value.x}px`,
  top: `${actionMenuPosition.value.y}px`,
}));

onMounted(async () => {
  await loadDocuments();
  await loadProjects();
});

async function loadDocuments() {
  loading.value = true;
  try {
    documents.value = await apiService.getAllDocuments();
  } catch (error) {
    console.error('加载文档失败:', error);
  } finally {
    loading.value = false;
  }
}

async function loadProjects() {
  try {
    projects.value = await apiService.getProjects();
  } catch (error) {
    console.error('加载项目失败:', error);
  }
}

function getCategoryLabel(category: string) {
  const cat = categories.find(c => c.value === category);
  return cat ? cat.label : category;
}

function getProjectName(projectId: string) {
  const project = projects.value.find(p => p.id === projectId);
  return project ? project.name : '';
}

function getCategoryVariant(category: string): 'primary' | 'secondary' | 'accent' | 'danger' {
  const variantMap: Record<string, 'primary' | 'secondary' | 'accent' | 'danger'> = {
    requirements: 'primary',
    design: 'secondary',
    development: 'accent',
    testing: 'accent',
    deployment: 'danger',
    documentation: 'secondary',
    other: 'secondary',
  };
  return variantMap[category] || 'secondary';
}

function formatFileSize(bytes: number) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
}

function formatDate(dateStr: string) {
  const date = dayjs(dateStr);
  const now = dayjs();
  const diff = now.diff(date, 'day');

  if (diff === 0) return t('documents.today');
  if (diff === 1) return t('documents.yesterday');
  if (diff < 7) return `${diff} ${t('documents.daysAgo')}`;
  if (diff < 30) return `${Math.floor(diff / 7)} ${t('documents.weeksAgo')}`;
  return date.format('YYYY-MM-DD');
}

function performSearch() {
  console.log('执行搜索:', searchQuery.value);
}

function canPreview(fileType: string) {
  const type = fileType.toLowerCase();
  return type.startsWith('image/') || type === 'application/pdf';
}

function openActionMenu(event: Event, document: Document) {
  const target = event.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  actionMenuPosition.value = {
    x: rect.right - 150,
    y: rect.bottom + 5,
  };
  actionMenuDocument.value = document;
  showActionMenu.value = true;
}

function closeActionMenu() {
  showActionMenu.value = false;
  actionMenuDocument.value = null;
}

function editDocument(document: Document) {
  selectedDocument.value = document;
  showEditModal.value = true;
  closeActionMenu();
}

async function deleteDocument(document: Document) {
  if (!confirm(t('documents.deleteConfirm', { name: document.name }))) {
    return;
  }

  try {
    await apiService.deleteDocument(document.id);
    documents.value = documents.value.filter(d => d.id !== document.id);
    closeActionMenu();
  } catch (error) {
    console.error('删除文档失败:', error);
    alert(t('documents.deleteError'));
  }
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

function openPreview(document: Document) {
  selectedDocument.value = document;
  showPreviewModal.value = true;
}

function handleDocumentUploaded(document: Document) {
  documents.value.unshift(document);
  showUploadModal.value = false;
}

function handleDocumentUpdated(document: Document) {
  const index = documents.value.findIndex(d => d.id === document.id);
  if (index !== -1) {
    documents.value[index] = document;
  }
  showEditModal.value = false;
}

function clearFilters() {
  searchQuery.value = '';
  selectedProject.value = '';
  selectedCategory.value = '';
}
</script>
