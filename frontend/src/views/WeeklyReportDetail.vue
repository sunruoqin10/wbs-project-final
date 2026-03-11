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
import type { WeeklyReport, WeeklyReportComment as Comment } from '@/types';
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
const newComment = ref('');
const approvalModalOpen = ref(false);

const reportId = computed(() => route.params.id as string);
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
  } catch (error) {
    console.error('Failed to load report data:', error);
    comments.value = [];
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
