<template>
  <MainLayout>
    <div class="max-w-4xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <Button variant="ghost" @click="goBack">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            {{ $t('common.back') }}
          </Button>
          <h1 class="text-2xl font-bold text-secondary-900">
            {{ isEditing ? $t('weeklyReports.form.editTitle') : $t('weeklyReports.form.createTitle') }}
          </h1>
        </div>
      </div>

      <Card class="p-6">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="mb-2 block text-sm font-medium text-secondary-700">
                {{ $t('weeklyReports.form.project') }} *
              </label>
              <select
                v-model="formData.projectId"
                :disabled="isEditing"
                :class="[
                  'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 transition-colors',
                  isEditing
                    ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                    : 'border-secondary-200 bg-white'
                ]"
                required
              >
                <option value="">{{ $t('weeklyReports.form.selectProject') }}</option>
                <option v-for="project in availableProjects" :key="project.id" :value="project.id">
                  {{ project.name }}
                </option>
              </select>
            </div>

            <div>
              <label class="mb-2 block text-sm font-medium text-secondary-700">
                {{ $t('weeklyReports.form.week') }} *
              </label>
              <div class="flex gap-2">
                <input
                  v-model="formData.weekStart"
                  type="date"
                  :disabled="isEditing"
                  :class="[
                    'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
                    isEditing
                      ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                      : 'border-secondary-200'
                  ]"
                  required
                  @change="updateWeekEnd"
                />
                <span class="flex items-center text-secondary-500">-</span>
                <input
                  v-model="formData.weekEnd"
                  type="date"
                  :disabled="isEditing"
                  :class="[
                    'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
                    isEditing
                      ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                      : 'border-secondary-200'
                  ]"
                  required
                />
              </div>
              <p class="mt-1 text-xs text-secondary-500">{{ $t('weeklyReports.form.weekHint') }}</p>
            </div>
          </div>

          <div v-if="isEditing" class="rounded-lg bg-info-50 border border-info-200 px-3 py-2">
            <div class="flex gap-2 items-start">
              <svg class="h-4 w-4 text-info-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div class="text-xs text-info-800 leading-relaxed">
                {{ $t('weeklyReports.form.editHint') }}
              </div>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.completedWork') }} *
            </label>
            <textarea
              v-model="formData.completedWork"
              rows="6"
              :placeholder="$t('weeklyReports.form.completedWorkPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
              required
            ></textarea>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.nextWeekPlan') }} *
            </label>
            <textarea
              v-model="formData.nextWeekPlan"
              rows="6"
              :placeholder="$t('weeklyReports.form.nextWeekPlanPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
              required
            ></textarea>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.problems') }}
            </label>
            <textarea
              v-model="formData.problems"
              rows="4"
              :placeholder="$t('weeklyReports.form.problemsPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
            ></textarea>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.documents') }}
            </label>
            <div class="border-2 border-dashed border-secondary-300 rounded-lg p-6 text-center hover:border-primary-500 transition-colors">
              <input
                ref="fileInputRef"
                type="file"
                class="hidden"
                multiple
                @change="handleFileSelect"
                :disabled="uploading"
              />
              <button
                type="button"
                @click="fileInputRef?.click()"
                :disabled="uploading"
                class="inline-flex items-center gap-2 rounded-lg bg-secondary-100 px-4 py-2 text-sm font-medium text-secondary-700 hover:bg-secondary-200 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                {{ uploading ? '上传中...' : $t('weeklyReports.form.selectFiles') }}
              </button>
              <p class="mt-2 text-xs text-secondary-500">{{ $t('weeklyReports.form.filesHint') }}</p>

              <div v-if="tempDocuments.length > 0" class="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                <div
                  v-for="(doc, index) in tempDocuments"
                  :key="index"
                  class="flex items-center gap-2 rounded-lg border border-secondary-200 bg-secondary-50 p-3"
                >
                  <div class="flex-shrink-0">
                    <div v-if="doc.fileType?.startsWith('image/')" class="h-10 w-10 rounded bg-white flex items-center justify-center overflow-hidden">
                      <img :src="doc.previewUrl" :alt="doc.name" class="h-full w-full object-cover" />
                    </div>
                    <div v-else class="h-10 w-10 rounded bg-white flex items-center justify-center text-secondary-400">
                      <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-secondary-900 truncate">{{ doc.name }}</p>
                    <p class="text-xs text-secondary-500">{{ formatFileSize(doc.size) }}</p>
                  </div>
                  <button
                    type="button"
                    @click="removeTempDocument(index)"
                    class="flex-shrink-0 p-1 rounded hover:bg-secondary-200 text-secondary-400 hover:text-danger-600 transition-colors"
                  >
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="flex gap-3 pt-4 border-t border-secondary-200">
            <Button variant="secondary" @click="goBack">
              {{ $t('common.cancel') }}
            </Button>
            <Button variant="primary" @click="handleSaveDraft" :loading="saving">
              {{ $t('weeklyReports.form.saveDraft') }}
            </Button>
            <Button variant="primary" @click="handleSubmit" :loading="saving">
              {{ $t('weeklyReports.form.submit') }}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import type { WeeklyReport, Document } from '@/types';
import { apiService } from '@/services/api';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const weeklyReportStore = useWeeklyReportStore();
const projectStore = useProjectStore();
const userStore = useUserStore();

const reportId = computed(() => route.params.id as string);
const isEditing = computed(() => !!reportId.value && route.path.includes('/edit'));
const saving = ref(false);
const uploading = ref(false);
const tempDocuments = ref<Document[]>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);

const currentUserId = computed(() => userStore.currentUser?.id || userStore.currentUserId);

const permissionStore = computed(() => ({
  currentRole: userStore.currentUser?.role || 'member'
}));

const availableProjects = computed(() => {
  if (permissionStore.value.currentRole === 'admin' || permissionStore.value.currentRole === 'project-manager') {
    return projectStore.projects;
  }
  return projectStore.projects.filter(p => {
    const isOwner = p.ownerId === currentUserId.value;
    const isMember = p.memberIds?.includes(currentUserId.value) || false;
    return isOwner || isMember;
  });
});

const formData = reactive({
  projectId: '',
  weekStart: '',
  weekEnd: '',
  completedWork: '',
  nextWeekPlan: '',
  problems: ''
});

const resetForm = () => {
  formData.projectId = '';
  formData.weekStart = '';
  formData.weekEnd = '';
  formData.completedWork = '';
  formData.nextWeekPlan = '';
  formData.problems = '';
};

const loadReportData = async () => {
  if (isEditing.value) {
    await weeklyReportStore.loadReports();
    const report = weeklyReportStore.currentReport;
    if (report) {
      formData.projectId = report.projectId;
      formData.weekStart = report.weekStart;
      formData.weekEnd = report.weekEnd;
      formData.completedWork = report.completedWork;
      formData.nextWeekPlan = report.nextWeekPlan;
      formData.problems = report.problems || '';
      await loadExistingDocuments(reportId.value);
    }
  } else {
    resetForm();
    setDefaultWeekRange();
  }
};

const loadExistingDocuments = async (id: string) => {
  try {
    console.log('[表单页] 开始加载现有文档，reportId:', id);
    const existingDocs = await apiService.getReportDocuments(id);
    console.log('[表单页] 加载到的现有文档:', existingDocs);
    existingDocs.forEach(doc => {
      const previewUrl = doc.fileType?.startsWith('image/')
        ? `/documents/${doc.id}/preview`
        : undefined;
      tempDocuments.value.push({
        ...doc,
        previewUrl,
        size: doc.fileSize
      } as any);
    });
    console.log('[表单页] tempDocuments 数量:', tempDocuments.value.length);
  } catch (error) {
    console.error('Failed to load existing documents:', error);
  }
};

const setDefaultWeekRange = () => {
  const today = dayjs();
  const monday = today.startOf('week');
  const sunday = today.endOf('week');
  
  formData.weekStart = monday.format('YYYY-MM-DD');
  formData.weekEnd = sunday.format('YYYY-MM-DD');
};

const updateWeekEnd = () => {
  if (formData.weekStart) {
    const start = dayjs(formData.weekStart);
    const end = start.add(6, 'day');
    formData.weekEnd = end.format('YYYY-MM-DD');
  }
};

const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;

  uploading.value = true;
  try {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const previewUrl = file.type.startsWith('image/')
        ? URL.createObjectURL(file)
        : undefined;

      tempDocuments.value.push({
        id: '',
        name: file.name,
        fileName: file.name,
        category: 'other',
        filePath: '',
        fileSize: file.size,
        fileType: file.type,
        fileExtension: file.name.split('.').pop() || '',
        version: 1,
        uploadedBy: currentUserId.value || '',
        status: 'active',
        downloadCount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        previewUrl,
        size: file.size,
        file
      } as any);
    }
    input.value = '';
  } catch (error) {
    console.error('Failed to process files:', error);
    alert('文件处理失败');
  } finally {
    uploading.value = false;
  }
};

const removeTempDocument = async (index: number) => {
  const doc = tempDocuments.value[index] as any;

  if (doc?.id) {
    try {
      if (!confirm('确定要删除这个文档吗？')) {
        return;
      }
      await apiService.deleteDocument(doc.id);
    } catch (error) {
      console.error('Failed to delete document:', error);
      alert('删除文档失败');
      return;
    }
  } else if (doc?.previewUrl) {
    URL.revokeObjectURL(doc.previewUrl);
  }

  tempDocuments.value.splice(index, 1);
};

const uploadDocuments = async (reportId: string, projectIdOverride?: string): Promise<void> => {
  console.log('[表单页] 开始上传文档，reportId:', reportId, '文档数量:', tempDocuments.value.length);
  console.log('[表单页] 文档列表:', tempDocuments.value.map(d => ({
    id: d.id,
    name: d.name,
    hasFile: !!d.file,
    fileType: d.fileType
  })));

  if (tempDocuments.value.length === 0) {
    console.log('[表单页] 没有文档需要上传');
    return;
  }

  let uploadCount = 0;
  const projectId = projectIdOverride || weeklyReportStore.currentReport?.projectId || formData.projectId;
  console.log('[表单页] 上传文档时获取的 projectId:', projectId);
  
  for (const doc of tempDocuments.value) {
    if (doc.file) {
      console.log('[表单页] 上传文档:', doc.name, 'fileType:', doc.file?.type);
      const uploadFormData = new FormData();
      uploadFormData.append('file', doc.file);
      uploadFormData.append('name', doc.name);
      uploadFormData.append('category', 'other');
      uploadFormData.append('reportId', reportId);
      if (projectId) {
        uploadFormData.append('projectId', projectId);
      }

      console.log('[表单页] FormData 内容:', Object.fromEntries(uploadFormData.entries()));

      try {
        const uploadedDoc = await apiService.uploadDocument(uploadFormData);
        console.log('[表单页] 文档上传成功:', uploadedDoc);
        uploadCount++;
      } catch (error) {
        console.error('[表单页] Failed to upload document:', doc.name, error);
      }
    }
  }

  console.log('[表单页] 上传完成，成功上传:', uploadCount, '个文档');
  tempDocuments.value = [];
};

const formatFileSize = (bytes: number): string => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

const validateForm = (isDraft: boolean = false): boolean => {
  if (!formData.projectId) {
    alert(t('weeklyReports.form.errors.selectProject'));
    return false;
  }

  if (!formData.weekStart || !formData.weekEnd) {
    alert(t('weeklyReports.form.errors.selectWeek'));
    return false;
  }

  if (!isDraft) {
    if (!formData.completedWork.trim()) {
      alert(t('weeklyReports.form.errors.enterCompletedWork'));
      return false;
    }

    if (!formData.nextWeekPlan.trim()) {
      alert(t('weeklyReports.form.errors.enterNextWeekPlan'));
      return false;
    }
  }

  return true;
};

const handleSaveDraft = async () => {
  if (!validateForm(true)) {
    return;
  }

  saving.value = true;

  try {
    const reportData = {
      ...formData,
      userId: currentUserId.value,
      status: 'draft' as const
    };

    if (isEditing.value) {
      await weeklyReportStore.updateReport(reportId.value, reportData);
      await uploadDocuments(reportId.value);
      alert(t('messages.success.save'));
      router.push('/weekly-reports');
    } else {
      const newReport = await weeklyReportStore.createReport(reportData);
      if (newReport) {
        await uploadDocuments(newReport.id, formData.projectId);
        alert(t('messages.success.save'));
        router.push('/weekly-reports');
      }
    }
  } catch (error) {
    console.error('Failed to save draft:', error);
    alert(t('messages.error.save'));
  } finally {
    saving.value = false;
  }
};

const handleSubmit = async () => {
  if (!validateForm(false)) {
    return;
  }

  saving.value = true;

  try {
    const reportData = {
      ...formData,
      userId: currentUserId.value
    };

    if (isEditing.value) {
      await weeklyReportStore.updateReport(reportId.value, reportData);
      await weeklyReportStore.submitReport(reportId.value);
      await uploadDocuments(reportId.value);
      alert(t('messages.success.submit'));
    } else {
      const newReport = await weeklyReportStore.createReport(reportData);
      if (newReport) {
        await uploadDocuments(newReport.id, formData.projectId);
        await weeklyReportStore.submitReport(newReport.id);
        alert(t('messages.success.submit'));
      }
    }
    router.push('/weekly-reports');
  } catch (error) {
    console.error('Failed to submit report:', error);
    alert(t('messages.error.submit'));
  } finally {
    saving.value = false;
  }
};

const goBack = () => {
  router.back();
};

onMounted(async () => {
  await projectStore.loadProjects();
  await loadReportData();
});
</script>
