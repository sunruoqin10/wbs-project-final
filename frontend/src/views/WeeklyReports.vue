<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('routes.weeklyReports') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('weeklyReports.subtitle') }}</p>
        </div>
        <Button variant="primary" @click="createNewReport">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('weeklyReports.createReport') }}
        </Button>
      </div>

      <Card class="p-4">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex-1 min-w-[200px]">
            <Input
              v-model="searchQuery"
              :placeholder="$t('weeklyReports.searchPlaceholder')"
              type="text"
            >
              <template #prefix>
                <svg class="h-5 w-5 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </template>
            </Input>
          </div>

          <div class="flex items-center gap-2">
            <span class="text-sm font-medium text-secondary-700">{{ $t('common.status') }}:</span>
            <div class="flex gap-2">
              <button
                v-for="status in statusOptions"
                :key="status.value"
                @click="toggleStatus(status.value)"
                :class="[
                  'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
                  selectedStatuses.includes(status.value)
                    ? 'bg-primary-600 text-white'
                    : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
                ]"
              >
                {{ status.label }}
              </button>
            </div>
          </div>

          <div class="flex items-center gap-1 rounded-lg bg-secondary-100 p-1">
            <button
              @click="setViewMode('card')"
              :class="[
                'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
                viewMode === 'card'
                  ? 'bg-white text-primary-600 shadow-sm'
                  : 'text-secondary-600 hover:text-secondary-900'
              ]"
              :title="$t('weeklyReports.viewModes.card')"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
              <span class="hidden sm:inline">{{ $t('weeklyReports.viewModes.card') }}</span>
            </button>
            <button
              @click="setViewMode('list')"
              :class="[
                'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
                viewMode === 'list'
                  ? 'bg-white text-primary-600 shadow-sm'
                  : 'text-secondary-600 hover:text-secondary-900'
              ]"
              :title="$t('weeklyReports.viewModes.list')"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
              </svg>
              <span class="hidden sm:inline">{{ $t('weeklyReports.viewModes.list') }}</span>
            </button>
          </div>

          <Button
            v-if="selectedStatuses.length > 0 || searchQuery"
            variant="ghost"
            size="sm"
            @click="clearFilters"
          >
            {{ $t('weeklyReports.clearFilters') }}
          </Button>
        </div>
      </Card>

      <Transition name="fade" mode="out-in">
        <div
          v-if="viewMode === 'card'"
          key="card-view"
          class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3"
        >
          <WeeklyReportCard
            v-for="report in filteredReports"
            :key="report.id"
            :report="report"
            @click="goToReportDetail(report.id)"
            @edit="editReport"
            @delete="confirmDelete"
          />
        </div>

        <div
          v-else
          key="list-view"
          class="space-y-3"
        >
          <div class="hidden rounded-lg bg-secondary-50 px-4 py-3 text-sm font-medium text-secondary-600 md:grid md:grid-cols-12 md:gap-4">
            <div class="col-span-4">{{ $t('weeklyReports.listView.project') }}</div>
            <div class="col-span-2">{{ $t('weeklyReports.listView.week') }}</div>
            <div class="col-span-2">{{ $t('weeklyReports.listView.status') }}</div>
            <div class="col-span-2">{{ $t('weeklyReports.listView.submitter') }}</div>
            <div class="col-span-1">{{ $t('weeklyReports.listView.date') }}</div>
            <div class="col-span-1 text-right">{{ $t('weeklyReports.listView.actions') }}</div>
          </div>

          <div
            v-for="report in filteredReports"
            :key="report.id"
            class="group cursor-pointer rounded-lg border border-secondary-200 bg-white p-4 transition-all hover:border-primary-300 hover:shadow-md md:grid md:grid-cols-12 md:gap-4 md:p-3"
            @click="goToReportDetail(report.id)"
          >
            <div class="col-span-4 flex items-center gap-3">
              <div class="h-3 w-3 flex-shrink-0 rounded-full" :style="{ backgroundColor: getProjectColor(report.projectId) }"></div>
              <div class="min-w-0 flex-1">
                <div class="truncate font-medium text-secondary-900">{{ getProjectName(report.projectId) }}</div>
                <div class="truncate text-xs text-secondary-500 md:hidden">{{ formatWeekRange(report) }}</div>
              </div>
            </div>

            <div class="col-span-2 mt-2 flex items-center md:mt-0">
              <span class="text-sm text-secondary-600">{{ formatWeekRange(report) }}</span>
            </div>

            <div class="col-span-2 mt-2 flex items-center md:mt-0">
              <Badge :variant="getStatusVariant(report.status)">{{ getStatusLabel(report.status) }}</Badge>
            </div>

            <div class="col-span-2 mt-2 flex items-center md:mt-0">
              <div class="flex items-center gap-2">
                <img
                  :src="getUserAvatar(report.userId)"
                  :alt="getUserName(report.userId)"
                  class="h-7 w-7 rounded-full"
                />
                <span class="truncate text-sm text-secondary-700">{{ getUserName(report.userId) }}</span>
              </div>
            </div>

            <div class="col-span-1 mt-2 flex items-center text-sm text-secondary-600 md:mt-0">
              {{ formatDate(report.createdAt) }}
            </div>

            <div class="col-span-1 mt-3 flex items-center justify-end gap-2 md:mt-0">
              <button
                v-if="canEditReport(report)"
                @click.stop="editReport(report)"
                class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-primary-600"
                :title="$t('common.edit')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button
                v-if="canDeleteReport(report)"
                @click.stop="confirmDelete(report)"
                class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-danger-600"
                :title="$t('common.delete')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <Card v-if="filteredReports.length === 0" class="p-12">
        <div class="text-center">
          <svg class="mx-auto h-24 w-24 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-secondary-900">{{ $t('weeklyReports.emptyState.title') }}</h3>
          <p class="mt-2 text-sm text-secondary-600">
            {{ searchQuery || selectedStatuses.length > 0 ? $t('weeklyReports.emptyState.noResults') : $t('weeklyReports.emptyState.createFirst') }}
          </p>
          <Button v-if="!searchQuery && selectedStatuses.length === 0" variant="primary" class="mt-4" @click="createNewReport">
            {{ $t('weeklyReports.emptyState.createButton') }}
          </Button>
        </div>
      </Card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import WeeklyReportCard from '@/components/weeklyReport/WeeklyReportCard.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Badge from '@/components/common/Badge.vue';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import type { WeeklyReport } from '@/types';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const router = useRouter();
const { t } = useI18n();
const weeklyReportStore = useWeeklyReportStore();
const projectStore = useProjectStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const searchQuery = ref('');
const selectedStatuses = ref<string[]>([]);

const STORAGE_KEY = 'wbs-weekly-report-view-mode';

const viewMode = ref<'card' | 'list'>(
  (localStorage.getItem(STORAGE_KEY) as 'card' | 'list') || 'card'
);

const setViewMode = (mode: 'card' | 'list') => {
  viewMode.value = mode;
  localStorage.setItem(STORAGE_KEY, mode);
};

onMounted(async () => {
  await Promise.all([
    weeklyReportStore.loadReports(),
    userStore.loadUsers()
  ]);
});

const statusOptions = computed(() => [
  { label: t('weeklyReports.statuses.all'), value: '' },
  { label: t('weeklyReports.statuses.draft'), value: 'draft' },
  { label: t('weeklyReports.statuses.submitted'), value: 'submitted' },
  { label: t('weeklyReports.statuses.approved'), value: 'approved' },
  { label: t('weeklyReports.statuses.rejected'), value: 'rejected' }
]);

const filteredReports = computed(() => {
  let result = weeklyReportStore.reports;

  console.log('周报列表调试信息:');
  console.log('- 所有周报数量:', weeklyReportStore.reports.length);
  console.log('- 所有周报数据:', weeklyReportStore.reports);
  console.log('- 当前用户ID:', userStore.currentUser?.id || userStore.currentUserId);
  console.log('- 当前用户角色:', permissionStore.currentRole);
  console.log('- 选中的状态:', selectedStatuses.value);

  // 权限过滤：只对已登录的非管理员用户进行过滤
  if (permissionStore.currentRole !== 'admin' && permissionStore.currentRole !== 'project-manager') {
    const currentUserId = userStore.currentUser?.id || userStore.currentUserId;
    console.log('- 权限过滤: 当前用户不是管理员，需要过滤周报');
    console.log('- currentUserId:', currentUserId);

    if (currentUserId) {
      const beforeFilter = result.length;
      result = result.filter(report => {
        const match = report.userId === currentUserId;
        console.log(`- 周报 ${report.id}: userId=${report.userId}, 匹配=${match}`);
        return match;
      });
      console.log('- 权限过滤前:', beforeFilter, '过滤后:', result.length);
    } else {
      console.log('- 警告: currentUserId 为空，无法进行权限过滤，显示所有数据');
    }
  } else {
    console.log('- 权限过滤: 当前用户是管理员或项目经理，显示所有周报');
  }

  if (selectedStatuses.value.length > 0) {
    const beforeFilter = result.length;
    result = result.filter(r => selectedStatuses.value.includes(r.status));
    console.log('- 状态过滤前:', beforeFilter, '过滤后:', result.length);
  }

  if (searchQuery.value) {
    const search = searchQuery.value.toLowerCase();
    result = result.filter(r => {
      const projectName = getProjectName(r.projectId).toLowerCase();
      const userName = getUserName(r.userId).toLowerCase();
      const weekRange = formatWeekRange(r).toLowerCase();
      return projectName.includes(search) || userName.includes(search) || weekRange.includes(search);
    });
  }

  console.log('- 最终结果数量:', result.length);

  return result.sort((a, b) => {
    return dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf();
  });
});

const toggleStatus = (status: string) => {
  if (status === '') {
    selectedStatuses.value = [];
  } else {
    const index = selectedStatuses.value.indexOf(status);
    if (index === -1) {
      selectedStatuses.value.push(status);
    } else {
      selectedStatuses.value.splice(index, 1);
    }
  }
};

const clearFilters = () => {
  searchQuery.value = '';
  selectedStatuses.value = [];
};

const createNewReport = () => {
  router.push('/weekly-reports/new');
};

const editReport = (report: WeeklyReport) => {
  router.push(`/weekly-reports/${report.id}/edit`);
};

const confirmDelete = async (report: WeeklyReport) => {
  if (confirm(t('weeklyReports.confirmDelete'))) {
    await weeklyReportStore.deleteReport(report.id);
  }
};

const goToReportDetail = (reportId: string) => {
  router.push(`/weekly-reports/${reportId}`);
};

const getProjectName = (projectId: string): string => {
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.name || projectId;
};

const getProjectColor = (projectId: string): string => {
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.color || '#3b82f6';
};

const getUserName = (userId: string): string => {
  const user = userStore.userById(userId);
  return user?.name || userId;
};

const getUserAvatar = (userId: string): string => {
  const user = userStore.userById(userId);
  return user?.avatar || '';
};

const formatWeekRange = (report: WeeklyReport): string => {
  if (!report.weekStart || !report.weekEnd) return '-';
  return `${dayjs(report.weekStart).format('MM/DD')} - ${dayjs(report.weekEnd).format('MM/DD')}`;
};

const formatDate = (date: string): string => {
  return dayjs(date).format('MM/DD');
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

const canEditReport = (report: WeeklyReport): boolean => {
  const currentUserId = userStore.currentUserId;
  if (permissionStore.currentRole === 'admin') return true;
  if (report.userId === currentUserId && report.status === 'draft') return true;
  return false;
};

const canDeleteReport = (report: WeeklyReport): boolean => {
  const currentUserId = userStore.currentUserId;
  if (permissionStore.currentRole === 'admin') return true;
  if (report.userId === currentUserId && report.status === 'draft') return true;
  return false;
};
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
