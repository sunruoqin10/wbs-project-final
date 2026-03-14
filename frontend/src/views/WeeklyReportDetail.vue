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
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('weeklyReports.detail.title') }}</h1>
        </div>
        <div class="flex items-center gap-2">
          <Button v-if="canEdit" variant="secondary" @click="editReport">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            {{ $t('common.edit') }}
          </Button>
          <Button v-if="canApprove && report?.status === 'submitted'" variant="primary" @click="openApprovalModal">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ $t('weeklyReports.detail.approve') }}
          </Button>
        </div>
      </div>

      <div v-if="report">
        <Card class="p-6">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-4">
                <div class="h-4 w-4 rounded-full" :style="{ backgroundColor: getProjectColor(report.projectId) }"></div>
                <h2 class="text-xl font-semibold text-secondary-900">{{ getProjectName(report.projectId) }}</h2>
                <Badge :variant="getStatusVariant(report.status)">{{ getStatusLabel(report.status) }}</Badge>
              </div>

              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span class="text-secondary-600">{{ $t('weeklyReports.detail.week') }}:</span>
                  <span class="ml-2 font-medium text-secondary-900">{{ formatWeekRange(report) }}</span>
                </div>
                <div>
                  <span class="text-secondary-600">{{ $t('weeklyReports.detail.submitter') }}:</span>
                  <span class="ml-2 font-medium text-secondary-900">{{ getUserName(report.userId) }}</span>
                </div>
                <div>
                  <span class="text-secondary-600">{{ $t('weeklyReports.detail.submitTime') }}:</span>
                  <span class="ml-2 font-medium text-secondary-900">{{ formatDateTime(report.submitTime) || '-' }}</span>
                </div>
                <div v-if="report.approveTime">
                  <span class="text-secondary-600">{{ $t('weeklyReports.detail.approveTime') }}:</span>
                  <span class="ml-2 font-medium text-secondary-900">{{ formatDateTime(report.approveTime) }}</span>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-3">
              <div class="h-12 w-12 rounded-full flex items-center justify-center bg-primary-100 text-primary-600 font-semibold">
                {{ getUserName(report.userId).charAt(0).toUpperCase() }}
              </div>
            </div>
          </div>
        </Card>

        <Card class="mt-6 p-6">
          <h3 class="text-lg font-semibold text-secondary-900 mb-4">{{ $t('weeklyReports.detail.completedWork') }}</h3>
          <div class="prose prose-sm max-w-none text-secondary-700 whitespace-pre-wrap">
            {{ report.completedWork || $t('weeklyReports.detail.noContent') }}
          </div>
        </Card>

        <Card class="mt-6 p-6">
          <h3 class="text-lg font-semibold text-secondary-900 mb-4">{{ $t('weeklyReports.detail.nextWeekPlan') }}</h3>
          <div class="prose prose-sm max-w-none text-secondary-700 whitespace-pre-wrap">
            {{ report.nextWeekPlan || $t('weeklyReports.detail.noContent') }}
          </div>
        </Card>

        <Card v-if="report.problems" class="mt-6 p-6">
          <h3 class="text-lg font-semibold text-secondary-900 mb-4">{{ $t('weeklyReports.detail.problems') }}</h3>
          <div class="prose prose-sm max-w-none text-secondary-700 whitespace-pre-wrap">
            {{ report.problems }}
          </div>
        </Card>

        <Card v-if="report.approveComment" class="mt-6 p-6 bg-yellow-50 border-yellow-200">
          <h3 class="text-lg font-semibold text-secondary-900 mb-4">
            {{ report.status === 'approved' ? $t('weeklyReports.detail.approvalComment') : $t('weeklyReports.detail.rejectionComment') }}
          </h3>
          <div class="prose prose-sm max-w-none text-secondary-700 whitespace-pre-wrap">
            {{ report.approveComment }}
          </div>
          <div v-if="report.approverId" class="mt-4 text-sm text-secondary-600">
            {{ $t('weeklyReports.detail.approver') }}: {{ getUserName(report.approverId) }}
          </div>
        </Card>

        <Card class="mt-6 p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('weeklyReports.documents.title') }}</h3>
            <label v-if="canUploadDocument" class="cursor-pointer">
              <input
                type="file"
                class="hidden"
                multiple
                @change="handleFileUpload"
                :disabled="uploading"
              />
              <Button variant="primary" size="sm" :disabled="uploading">
                <svg v-if="!uploading" class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                <svg v-else class="mr-2 h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ uploading ? '上传中...' : $t('weeklyReports.documents.upload') }}
              </Button>
            </label>
          </div>

          <div v-if="documents.length === 0" class="text-center py-8 text-secondary-500 text-sm">
            {{ $t('weeklyReports.documents.noDocuments') }}
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="doc in documents"
              :key="doc.id"
              class="group relative rounded-lg border border-secondary-200 p-4 hover:border-primary-500 hover:shadow-md transition-all"
            >
              <div class="flex items-start gap-3">
                <div class="flex-shrink-0">
                  <div
                    v-if="doc.fileType?.startsWith('image/')"
                    class="h-16 w-16 rounded-lg bg-secondary-100 flex items-center justify-center overflow-hidden"
                  >
                    <img
                      :src="`${apiBaseUrl}/documents/${doc.id}/preview`"
                      :alt="doc.name"
                      class="h-full w-full object-cover"
                    />
                  </div>
                  <div
                    v-else
                    class="h-16 w-16 rounded-lg bg-secondary-100 flex items-center justify-center text-secondary-400"
                  >
                    <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                </div>

                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-secondary-900 truncate" :title="doc.name">
                    {{ doc.name }}
                  </p>
                  <p class="text-xs text-secondary-500 mt-1">
                    {{ formatFileSize(doc.fileSize) }}
                  </p>
                </div>
              </div>

              <div class="mt-3 flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  v-if="canPreview(doc.fileType)"
                  @click="openPreview(doc)"
                  class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600 hover:text-primary-600 transition-colors"
                  title="预览"
                >
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                </button>
                <button
                  @click="downloadDocument(doc)"
                  class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600 hover:text-success-600 transition-colors"
                  title="下载"
                >
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </button>
                <button
                  v-if="canUploadDocument && (doc.uploadedBy === currentUserId || permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager')"
                  @click="deleteDocument(doc)"
                  class="p-1.5 rounded hover:bg-secondary-100 text-secondary-600 hover:text-danger-600 transition-colors"
                  title="删除"
                >
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </Card>

        <Card class="mt-6 p-6">
          <h3 class="text-lg font-semibold text-secondary-900 mb-4">{{ $t('weeklyReports.comments.title') }}</h3>
          
          <WeeklyReportComment
            :comments="comments"
            @delete="deleteComment"
          />

          <div class="mt-6 pt-6 border-t border-secondary-200">
            <div class="flex gap-3">
              <div class="h-10 w-10 rounded-full flex items-center justify-center bg-primary-100 text-primary-600 font-semibold flex-shrink-0">
                {{ getUserName(currentUser?.id || '').charAt(0).toUpperCase() }}
              </div>
              <div class="flex-1">
                <textarea
                  v-model="newComment"
                  :placeholder="$t('weeklyReports.comments.addComment')"
                  rows="3"
                  class="w-full rounded-lg border border-secondary-300 px-4 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
                  @keydown.ctrl.enter="submitComment"
                ></textarea>
                <div class="mt-2 flex justify-end">
                  <Button variant="primary" size="sm" @click="submitComment" :disabled="!newComment.trim()">
                    {{ $t('weeklyReports.comments.submit') }}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <div v-else class="text-center py-12 text-secondary-600">
        {{ $t('weeklyReports.detail.loading') }}
      </div>
    </div>

    <ApprovalModal
      :open="approvalModalOpen"
      @close="closeApprovalModal"
      @submit="handleApproval"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import WeeklyReportComment from '@/components/weeklyReport/WeeklyReportComment.vue';
import ApprovalModal from '@/components/weeklyReport/ApprovalModal.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import type { WeeklyReport, WeeklyReportComment as Comment, Document } from '@/types';
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
const permissionStore = usePermissionStore();

const report = ref<WeeklyReport | null>(null);
const comments = ref<Comment[]>([]);
const documents = ref<Document[]>([]);
const newComment = ref('');
const approvalModalOpen = ref(false);

const reportId = computed(() => route.params.id as string);
const apiBaseUrl = computed(() => import.meta.env.VITE_API_BASE_URL || '/api');
const currentUserId = computed(() => userStore.currentUserId);
const currentUser = computed(() => {
  if (!currentUserId.value) return null;
  return userStore.userById(currentUserId.value);
});

const canEdit = computed(() => {
  if (!report.value) return false;
  if (permissionStore.currentRole === 'admin') return true;
  if (report.value.userId === currentUserId.value && report.value.status === 'draft') return true;
  return false;
});

const canApprove = computed(() => {
  if (permissionStore.currentRole === 'admin') return true;
  if (permissionStore.currentRole === 'project-manager') return true;
  return false;
});

onMounted(async () => {
  await loadData();
});

const loadData = async () => {
  try {
    await userStore.loadUsers();
    await projectStore.loadProjects();
    await weeklyReportStore.loadReports();
    report.value = await weeklyReportStore.loadReportById(reportId.value);
    const loadedComments = await weeklyReportStore.loadComments(reportId.value);
    comments.value = loadedComments || [];
    console.log('[详情页] 加载评论成功:', comments.value);
    await loadDocuments();
  } catch (error) {
    console.error('Failed to load report data:', error);
    comments.value = [];
  }
};

const loadDocuments = async () => {
  if (!reportId.value) return;
  try {
    documents.value = await apiService.getReportDocuments(reportId.value);
    console.log('[详情页] 加载文档成功:', documents.value);
  } catch (error) {
    console.error('Failed to load documents:', error);
    documents.value = [];
  }
};

const goBack = () => {
  router.back();
};

const editReport = () => {
  router.push(`/weekly-reports/${reportId.value}/edit`);
};

const openApprovalModal = () => {
  approvalModalOpen.value = true;
};

const closeApprovalModal = () => {
  approvalModalOpen.value = false;
};

const handleApproval = async (approved: boolean, comment: string) => {
  try {
    await weeklyReportStore.approveReport(reportId.value, approved, comment);
    await loadData();
    closeApprovalModal();
    alert(approved ? t('messages.success.approve') : t('messages.success.reject'));
  } catch (error: any) {
    const errorMessage = error?.message || error?.response?.data?.message || t('messages.error.approve');
    alert(errorMessage);
  }
};

const submitComment = async () => {
  if (!newComment.value.trim()) return;
  
  try {
    const addedComment = await weeklyReportStore.addComment(reportId.value, newComment.value);
    if (addedComment) {
      comments.value.unshift(addedComment);
      console.log('[详情页] 添加评论成功:', addedComment);
    } else {
      await loadData();
    }
    newComment.value = '';
  } catch (error) {
    console.error('Failed to submit comment:', error);
    await loadData();
  }
};

const deleteComment = async (commentId: string) => {
  await weeklyReportStore.deleteComment(commentId);
  await loadData();
};

const handleFileUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;

  uploading.value = true;
  try {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const formData = new FormData();
      formData.append('file', file);
      formData.append('name', file.name);
      formData.append('category', 'other');
      formData.append('reportId', reportId.value);

      const uploadedDoc = await apiService.uploadDocument(formData);
      documents.value.unshift(uploadedDoc);
    }
    input.value = '';
    console.log('[详情页] 文档上传成功');
  } catch (error) {
    console.error('Failed to upload document:', error);
    alert('文档上传失败');
  } finally {
    uploading.value = false;
  }
};

const downloadDocument = async (doc: Document) => {
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
    alert('文档下载失败');
  }
};

const deleteDocument = async (doc: Document) => {
  if (!confirm('确定要删除这个文档吗？')) return;
  try {
    await apiService.deleteDocument(doc.id);
    documents.value = documents.value.filter(d => d.id !== doc.id);
    console.log('[详情页] 文档删除成功');
  } catch (error) {
    console.error('Failed to delete document:', error);
    alert('文档删除失败');
  }
};

const canPreview = (fileType: string): boolean => {
  if (!fileType) return false;
  return fileType.startsWith('image/') || fileType === 'application/pdf';
};

const openPreview = (doc: Document) => {
  window.open(`${apiBaseUrl.value}/documents/${doc.id}/preview`, '_blank');
};

const formatFileSize = (bytes: number): string => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

const getProjectName = (projectId: string): string => {
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.name || t('common.unknown');
};

const getProjectColor = (projectId: string): string => {
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.color || '#3b82f6';
};

const getUserName = (userId: string | undefined | null): string => {
  if (!userId) return t('common.unknown');
  const user = userStore.userById(userId);
  return user?.name || t('common.unknown');
};

const formatWeekRange = (report: WeeklyReport | undefined | null): string => {
  if (!report?.weekStart || !report?.weekEnd) return '-';
  return `${dayjs(report.weekStart).format('MM/DD')} - ${dayjs(report.weekEnd).format('MM/DD')}`;
};

const formatDateTime = (dateTime: string | null): string => {
  if (!dateTime) return '';
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm');
};

const getStatusLabel = (status: string): string => {
  return t(`weeklyReports.statuses.${status}`);
};

const getStatusVariant = (status: string): string => {
  const variants: Record<string, string> = {
    draft: 'info',
    submitted: 'primary',
    approved: 'success',
    rejected: 'danger'
  };
  return variants[status] || 'info';
};
</script>
