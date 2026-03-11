<template>
  <Card class="cursor-pointer transition-all hover:shadow-md" @click="$emit('click', report)">
    <div class="flex items-start justify-between">
      <div class="flex-1">
        <div class="flex items-center gap-2 mb-2">
          <div class="h-3 w-3 flex-shrink-0 rounded-full" :style="{ backgroundColor: getStatusColor(report.status) }"></div>
          <h3 class="text-lg font-semibold text-secondary-900">{{ getReportTitle(report) }}</h3>
        </div>
        <p class="mb-2 text-sm text-secondary-600 line-clamp-2">{{ report.completedWork }}</p>
        <div class="flex items-center gap-2">
          <Badge :variant="getStatusVariant(report.status)">
            {{ getStatusLabel(report.status) }}
          </Badge>
          <span v-if="report.projectId" class="text-xs text-secondary-500">
            {{ getProjectName(report.projectId) }}
          </span>
        </div>
      </div>
      <div class="flex flex-col gap-1">
        <button
          v-if="canEdit"
          @click.stop="$emit('edit', report)"
          class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-primary-600"
          :title="$t('common.edit')"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 0L11.828 15H9v-3.172l16.172-16.172a2 2 0 00-2.828 0zM15 11V8a2 2 0 012-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v3.172L2.172 16.172a2 2 0 01.414 0L9 19.172V21h4V11z" />
          </svg>
        </button>
        <button
          v-if="canDelete"
          @click.stop="$emit('delete', report)"
          class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-danger-600"
          :title="$t('common.delete')"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
    </div>
    <div class="mt-3 flex items-center justify-between text-sm text-secondary-500">
      <span class="flex items-center gap-1">
        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0m0 0a4 4 0 011 8 0m-4 4v8m-4-4v8M8 7a4 4 0 11-8 0m0 0a4 4 0 011 8 0m-4 4v8m-4-4v8" />
        </svg>
        {{ formatWeekRange(report) }}
      </span>
      <span>{{ formatDate(report.createdAt) }}</span>
    </div>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import Card from '@/components/common/Card.vue';
import Badge from '@/components/common/Badge.vue';
import type { WeeklyReport } from '@/types';
import { useProjectStore } from '@/stores/project';
import { usePermissionStore } from '@/stores/permission';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const { t } = useI18n();
const projectStore = useProjectStore();
const permissionStore = usePermissionStore();

const props = defineProps<{
  report: WeeklyReport;
}>();

const emit = defineEmits<{
  click: [report: WeeklyReport];
  edit: [report: WeeklyReport];
  delete: [report: WeeklyReport];
}>();

const canEdit = computed(() => {
  if (permissionStore.currentRole === 'admin') return true;
  if (permissionStore.currentRole === 'project-manager') {
    return false;
  }
  return permissionStore.currentUserId === props.report.userId;
});

const canDelete = computed(() => {
  if (permissionStore.currentRole === 'admin') return true;
  return permissionStore.currentUserId === props.report.userId;
});

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: '#9ca3af',
    submitted: '#3b82f6',
    approved: '#10b981',
    rejected: '#ef4444'
  };
  return colors[status] || '#6b7280';
};

const getStatusVariant = (status: string) => {
  const variants: Record<string, string> = {
    draft: 'info',
    submitted: 'primary',
    approved: 'success',
    rejected: 'danger'
  };
  return variants[status] || 'info';
};

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    draft: t('weeklyReports.statuses.draft'),
    submitted: t('weeklyReports.statuses.submitted'),
    approved: t('weeklyReports.statuses.approved'),
    rejected: t('weeklyReports.statuses.rejected')
  };
  return labels[status] || status;
};

const getProjectName = (projectId: string) => {
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.name || t('common.unknown');
};

const getReportTitle = (report: WeeklyReport) => {
  if (report.projectId) {
    const project = projectStore.projects.find(p => p.id === report.projectId);
    if (project) {
      return `${project.name} - ${t('weeklyReports.title')}`;
    }
  }
  return t('weeklyReports.title');
};

const formatWeekRange = (report: WeeklyReport) => {
  if (!report.weekStart || !report.weekEnd) return '-';
  return `${dayjs(report.weekStart).format('MM/DD')} - ${dayjs(report.weekEnd).format('MM/DD')}`;
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD');
};
</script>
