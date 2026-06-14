<template>
  <Card v-if="logs.length" class="p-6">
    <h3 class="text-lg font-semibold text-secondary-900 mb-4">
      {{ $t('weeklyReports.approvalHistory') }}
    </h3>
    <div class="space-y-4">
      <div
        v-for="row in logs"
        :key="row.id"
        class="flex items-start gap-3"
      >
        <div class="flex-shrink-0">
          <img
            v-if="row.approverAvatar"
            :src="row.approverAvatar"
            class="h-9 w-9 rounded-full object-cover"
            :alt="row.approverName"
          />
          <div
            v-else
            class="h-9 w-9 rounded-full flex items-center justify-center bg-primary-100 text-primary-600 font-semibold text-sm"
          >
            {{ (row.approverName || '?').charAt(0).toUpperCase() }}
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex flex-wrap items-center gap-2 text-sm">
            <span class="font-medium text-secondary-900">{{ row.approverName }}</span>
            <span
              class="rounded px-2 py-0.5 text-xs font-medium"
              :class="roleBadgeClass(row.approverRole)"
            >
              {{ $t('roles.' + String(row.approverRole).replace(/-/g, '_')) }}
            </span>
            <span
              class="rounded px-2 py-0.5 text-xs font-medium"
              :class="row.action === 'approve' ? 'bg-success-100 text-success-700' : 'bg-danger-100 text-danger-700'"
            >
              {{ $t('weeklyReports.action.' + row.action) }}
            </span>
            <time class="text-xs text-secondary-500">{{ formatDateTime(row.createdAt) }}</time>
          </div>
          <p
            v-if="row.comment"
            class="mt-1 text-sm text-secondary-700 whitespace-pre-wrap"
          >
            {{ row.comment }}
          </p>
        </div>
      </div>
    </div>
  </Card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import Card from '@/components/common/Card.vue';
import apiService from '@/services/api';
import type { WeeklyReportApprovalLog } from '@/types';
import { formatDateTime } from '@/utils/format';

const props = defineProps<{
  reportId: string;
}>();

const logs = ref<WeeklyReportApprovalLog[]>([]);

async function load() {
  if (!props.reportId) {
    logs.value = [];
    return;
  }
  try {
    const data = await apiService.getWeeklyReportApprovalLogs(props.reportId);
    logs.value = Array.isArray(data) ? data : [];
  } catch (e) {
    // 静默失败:权限不足 / 周报无审批历史时,直接不渲染
    logs.value = [];
  }
}

// 初次拉取交给父组件的 onMounted 触发;此处只在 reportId 变化时刷新
watch(() => props.reportId, load, { immediate: true });

function roleBadgeClass(role: WeeklyReportApprovalLog['approverRole']): string {
  switch (role) {
    case 'admin':
      return 'bg-secondary-200 text-secondary-800';
    case 'project-manager':
      return 'bg-primary-100 text-primary-700';
    case 'dept-project-manager':
      return 'bg-warning-100 text-warning-700';
    case 'project-owner':
      return 'bg-info-100 text-info-700';
    default:
      return 'bg-secondary-100 text-secondary-700';
  }
}
</script>