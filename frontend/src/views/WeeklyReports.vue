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

      <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见,与 Dashboard / ProjectList / DocumentManagement 保持一致) -->
      <Card v-if="currentUser?.role === 'admin'" class="relative">
        <div class="flex items-center gap-4">
          <span class="text-sm font-medium text-secondary-600">
            {{ $t('dashboard.departmentFilter.label') }}
          </span>
          <OrgTreeSelect
            v-model="selectedDeptCode"
            @update:modelValue="onDeptChange"
          />
          <label
            class="flex items-center gap-2 text-sm text-secondary-600"
            :class="{ 'cursor-not-allowed opacity-50': isLeaf }"
            :title="isLeaf ? $t('dashboard.departmentFilter.leafHint') : ''"
          >
            <input
              type="checkbox"
              v-model="includeSubDepts"
              :disabled="isLeaf"
              class="h-4 w-4 rounded border-secondary-300 text-primary-600
                     focus:ring-primary-500 disabled:opacity-50"
            />
            <span>{{ $t('dashboard.departmentFilter.includeSubDepts') }}</span>
          </label>
        </div>
        <!-- deptMissing 提示:当 admin 没有 deptCode 时显示 -->
        <p
          v-if="currentUser && !currentUser.deptCode"
          class="mt-2 text-xs text-warning-600"
        >
          {{ $t('dashboard.departmentFilter.deptMissing') }}
        </p>
        <!-- 切换中遮罩 -->
        <Transition name="fade">
          <div
            v-if="switching"
            class="pointer-events-none absolute inset-0 flex items-center
                   justify-center rounded-lg bg-white/60 backdrop-blur-sm"
          >
            <svg
              class="h-6 w-6 animate-spin text-primary-600"
              fill="none" viewBox="0 0 24 24"
            >
              <circle
                class="opacity-25" cx="12" cy="12" r="10"
                stroke="currentColor" stroke-width="4"
              />
              <path
                class="opacity-75" fill="currentColor"
                d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              />
            </svg>
          </div>
        </Transition>
      </Card>

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
                <div class="h-7 w-7 rounded-full flex items-center justify-center bg-primary-100 text-primary-600 font-semibold text-xs">
                  {{ getUserName(report?.userId || '').charAt(0).toUpperCase() }}
                </div>
                <span class="truncate text-sm text-secondary-700">{{ getUserName(report?.userId || '') }}</span>
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
            {{ searchQuery || selectedStatuses.length > 0
                ? $t('weeklyReports.emptyState.noResults')
                : isDeptFilterActive
                  ? $t('dashboard.departmentFilter.emptyHint')
                  : $t('weeklyReports.emptyState.createFirst') }}
          </p>
          <!-- 2026-06-17: 部门过滤激活时,不显示「新建周报」按钮,避免给"未分配部门"以外的误操作引导 -->
          <Button
            v-if="!searchQuery && selectedStatuses.length === 0 && !isDeptFilterActive"
            variant="primary"
            class="mt-4"
            @click="createNewReport"
          >
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
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useOrgStore } from '@/stores/org';
import type { WeeklyReport, OrgNode } from '@/types';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const router = useRouter();
const { t } = useI18n();
const weeklyReportStore = useWeeklyReportStore();
const projectStore = useProjectStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const orgStore = useOrgStore();

const currentUser = computed(() => userStore.currentUser);

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

// ============ 部门过滤(admin 专用,与 Dashboard / ProjectList 保持一致,2026-06-17 新增) ============ //
const selectedDeptCode = ref<string | null>(currentUser.value?.deptCode ?? null);
const includeSubDepts = ref(false);
const switching = ref(false);
let switchTimer: number | null = null;

function onDeptChange(newCode: string | null) {
  // 切换部门时给一点 loading 反馈,避免依赖项在 await 中抖动
  switching.value = true;
  if (switchTimer) window.clearTimeout(switchTimer);
  switchTimer = window.setTimeout(() => {
    selectedDeptCode.value = newCode;
    switching.value = false;
  }, 100);
}

/** 工具:从 org 树中找到目标 code 节点,DFS 收集其所有后代 code */
function collectDescendants(root: OrgNode, targetCode: string): string[] {
  const result: string[] = [];
  function find(node: OrgNode): OrgNode | null {
    if (node.code === targetCode) return node;
    for (const c of node.children || []) {
      const hit = find(c);
      if (hit) return hit;
    }
    return null;
  }
  const target = find(root);
  if (!target) return result;
  function walk(n: OrgNode) {
    for (const c of n.children || []) {
      if (c.code) result.push(c.code);
      walk(c);
    }
  }
  walk(target);
  return result;
}

/** 工具:判断指定 code 在树中是否有子节点(用于「含子部门」checkbox enabled) */
function hasChildrenInTree(root: OrgNode | null, code: string): boolean {
  if (!root) return false;
  function dfs(node: OrgNode): boolean {
    if (node.code === code) return (node.children || []).length > 0;
    for (const c of node.children || []) if (dfs(c)) return true;
    return false;
  }
  return dfs(root);
}

const isLeaf = computed(() => {
  if (selectedDeptCode.value === null) return true;
  return !hasChildrenInTree(orgStore.tree, selectedDeptCode.value);
});

const effectiveDeptCodes = computed<Set<string | null>>(() => {
  if (selectedDeptCode.value === null) return new Set([null]);
  const codes = new Set<string | null>([selectedDeptCode.value]);
  if (includeSubDepts.value && orgStore.tree) {
    collectDescendants(orgStore.tree, selectedDeptCode.value).forEach(c => codes.add(c));
  }
  return codes;
});

/** 部门过滤后的项目 ID 集合,用于在 filteredReports 阶段裁剪周报。
 *  仅 admin 角色 + 已选部门时,过滤才会生效;否则视为不过滤。 */
const deptFilteredProjectIds = computed<Set<string> | null>(() => {
  if (currentUser.value?.role !== 'admin') return null;
  const ids = new Set<string>();
  for (const p of projectStore.projects) {
    if (effectiveDeptCodes.value.has(p.deptCode ?? null)) {
      ids.add(p.id);
    }
  }
  return ids;
});

/** 是否激活了部门过滤(2026-06-17):admin + 已选部门(非 null)。
 *  用于空结果状态文案 / 隐藏「新建周报」按钮等 UI 提示。 */
const isDeptFilterActive = computed(() => {
  return currentUser.value?.role === 'admin' && selectedDeptCode.value !== null;
});

onMounted(async () => {
  // 2026-06-16: 补 loadProjects — 项目名称依赖 projectStore.projects
  // 登录后 setCurrentUser 会清空 projects,若本页不重新拉,getProjectName 全部走到 common.unknown 兜底
  // 2026-06-17: 部门过滤需要 orgStore.tree,这里一起加载
  await Promise.all([
    weeklyReportStore.loadReports(),
    userStore.loadUsers(),
    projectStore.loadProjects(),
    orgStore.loadTree()
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
  // 2026-06-14 周报 4 角色对齐:后端已按 getAccessibleWeeklyReportUserIds 过滤,
  // 前端不再做二次角色 filter,只保留 UI 层的 status / keyword 筛选。
  // 2026-06-17: 新增部门维度(仅 admin 生效),按 report.projectId 是否命中
  // deptFilteredProjectIds 裁剪。这里走 projectId 而不是 userId,
  // 避免误把同部门之外的成员撰写的周报挡掉——周报归属是「项目」,不是「人」。
  let result = weeklyReportStore.reports;
  const deptIds = deptFilteredProjectIds.value;
  if (deptIds) {
    result = result.filter(r => r.projectId != null && deptIds.has(r.projectId));
  }

  if (selectedStatuses.value.length > 0) {
    result = result.filter(r => selectedStatuses.value.includes(r.status));
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
    try {
      await weeklyReportStore.deleteReport(report.id);
      await weeklyReportStore.loadReports();
    } catch (error) {
      console.error('删除周报失败:', error);
      alert(t('weeklyReports.messages.deleteFailed'));
    }
  }
};

const goToReportDetail = (reportId: string) => {
  router.push(`/weekly-reports/${reportId}`);
};

const getProjectName = (projectId: string | undefined): string => {
  if (!projectId) return '-';
  const project = projectStore.projects.find(p => p.id === projectId);
  // 2026-06-16: 找不到时回退显示 ID,便于排查(原 t('common.unknown') 过于隐晦)
  return project?.name || projectId;
};

const getProjectColor = (projectId: string | undefined): string => {
  if (!projectId) return '#3b82f6';
  const project = projectStore.projects.find(p => p.id === projectId);
  return project?.color || '#3b82f6';
};

const getUserName = (userId: string | undefined | null): string => {
  if (!userId) return '-';
  const user = userStore.userById(userId);
  // 2026-06-16: 找不到时回退显示 ID
  return user?.name || userId;
};

const formatWeekRange = (report: WeeklyReport): string => {
  if (!report?.weekStart || !report?.weekEnd) return '-';
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
  return permissionStore.canEditWeeklyReport(report);
};

const canDeleteReport = (report: WeeklyReport): boolean => {
  return permissionStore.canDeleteWeeklyReport(report);
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
