<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('routes.dashboard') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('dashboard.welcome') }}，{{ currentUser?.name }}！</p>
        </div>
      </div>

      <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见) -->
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

      <!-- Stats Cards -->
      <!-- 注意:不要加 mode="out-in",否则旧 div 的 200ms leave 动画期间
           新的图表容器还没被创建,ref 仍指向旧 div,watch 里 init 出来的
           图表会挂到旧 div 上,等 leave 结束旧 div 被销毁后,新 div 上就没有图表了。 -->
      <Transition name="content-fade">
        <div :key="`${selectedDeptCode}-${includeSubDepts}`" class="space-y-6">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-primary-100 p-3">
              <svg class="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.totalProjects') }}</p>
              <p class="text-2xl font-semibold text-secondary-900">{{ statistics.totalProjects }}</p>
            </div>
          </div>
        </Card>

        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-accent-100 p-3">
              <svg class="h-6 w-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.activeProjects') }}</p>
              <p class="text-2xl font-semibold text-secondary-900">{{ statistics.activeProjects }}</p>
            </div>
          </div>
        </Card>

        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-info-100 p-3">
              <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.totalTasks') }}</p>
              <p class="text-2xl font-semibold text-secondary-900">{{ statistics.totalTasks }}</p>
            </div>
          </div>
        </Card>

        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-warning-100 p-3">
              <svg class="h-6 w-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.totalMembers') }}</p>
              <p class="text-2xl font-semibold text-secondary-900">{{ statistics.totalMembers }}</p>
            </div>
          </div>
        </Card>
      </div>

      <!-- 预期 vs 实际进度对比 -->
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <Card>
          <div class="text-center">
            <p class="text-sm font-medium text-secondary-600">预期完成率</p>
            <p class="mt-2 text-3xl font-bold" :class="planVsActualClass">{{ planVsActual.plannedPercent }}%</p>
            <p class="mt-1 text-xs text-secondary-400">基于计划日期推算</p>
          </div>
        </Card>
        <Card>
          <div class="text-center">
            <p class="text-sm font-medium text-secondary-600">实际完成率</p>
            <p class="mt-2 text-3xl font-bold text-primary-600">{{ planVsActual.actualPercent }}%</p>
            <p class="mt-1 text-xs text-secondary-400">已完成 / 总任务 · {{ planVsActual.completedTasks }}/{{ planVsActual.totalLeafTasks }}</p>
          </div>
        </Card>
        <Card>
          <div class="text-center">
            <p class="text-sm font-medium text-secondary-600">偏差</p>
            <p class="mt-2 text-3xl font-bold" :class="planVsActualClass">
              {{ planVsActual.deviation >= 0 ? '+' : '' }}{{ planVsActual.deviation }}%
            </p>
            <p class="mt-1 text-xs" :class="planVsActual.deviation >= 0 ? 'text-success-600' : 'text-warning-600'">
              {{ planVsActual.deviation >= 0 ? '🟢 进度超前' : planVsActual.deviation >= -10 ? '🟡 轻微落后' : '🔴 明显落后' }}
            </p>
          </div>
        </Card>
      </div>

      <!-- Charts and Lists -->
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- Task Status Distribution -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('dashboard.charts.taskStatusDistribution') }}</h3>
          </template>
          <div class="h-64" ref="taskChartRef"></div>
        </Card>

        <!-- Project Progress -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('dashboard.charts.projectProgressOverview') }}</h3>
          </template>
          <div class="h-64" ref="projectChartRef"></div>
        </Card>
      </div>

      <!-- Recent Projects -->
      <Card>
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('dashboard.recentProjects.title') }}</h3>
            <router-link to="/projects" class="text-sm text-primary-600 hover:text-primary-700">
              {{ $t('dashboard.recentProjects.viewAll') }}
            </router-link>
          </div>
        </template>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-secondary-200">
            <thead class="bg-secondary-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('dashboard.recentProjects.projectName') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('dashboard.recentProjects.status') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('dashboard.recentProjects.progress') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('dashboard.recentProjects.deadline') }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-200 bg-white">
              <tr v-for="project in recentProjects" :key="project.id" class="hover:bg-secondary-50">
                <td class="whitespace-nowrap px-6 py-4">
                  <div class="flex items-center">
                    <div class="h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: project.color || '#3b82f6' }"></div>
                    <div class="ml-3">
                      <router-link
                        :to="`/projects/${project.id}`"
                        class="text-sm font-medium text-secondary-900 hover:text-primary-600"
                      >
                        {{ project.name }}
                      </router-link>
                    </div>
                  </div>
                </td>
                <td class="whitespace-nowrap px-6 py-4">
                  <Badge :variant="getStatusVariant(project.status)">{{ getStatusLabel(project.status) }}</Badge>
                </td>
                <td class="px-6 py-4">
                  <ProgressBar :value="project.progress" />
                </td>
                <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-600">
                  {{ formattedDate(project.endDate) }}
                </td>
              </tr>
            </tbody>
          </table>
          <div
            v-if="recentProjects.length === 0"
            class="py-8 text-center text-sm text-secondary-400"
          >
            {{ $t('dashboard.departmentFilter.emptyHint') }}
          </div>
        </div>
      </Card>

      <!-- Upcoming Tasks -->
      <Card>
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">{{ $t('dashboard.upcomingTasks.title') }}</h3>
        </template>
        <div class="space-y-3">
          <div
            v-for="task in upcomingTasks"
            :key="task.id"
            class="rounded-lg border border-secondary-200 p-4 hover:bg-secondary-50"
          >
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <p class="font-medium text-secondary-900">{{ task.title }}</p>
                <p class="mt-1 text-sm text-secondary-600">{{ getProjectName(task.projectId) }}</p>
              </div>
              <Badge :variant="getPriorityVariant(task.priority)" class="ml-3">
                {{ getPriorityLabel(task.priority) }}
              </Badge>
            </div>

            <div class="mt-3 grid grid-cols-2 gap-4 sm:grid-cols-4">
              <!-- 开始时间 -->
              <div class="flex items-center gap-2">
                <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <div>
                  <p class="text-xs text-secondary-500">{{ $t('dashboard.upcomingTasks.startTime') }}</p>
                  <p class="text-sm font-medium text-secondary-900">{{ formattedDate(task.startDate) }}</p>
                </div>
              </div>

              <!-- 结束时间 -->
              <div class="flex items-center gap-2">
                <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div>
                  <p class="text-xs text-secondary-500">{{ $t('dashboard.upcomingTasks.endTime') }}</p>
                  <p class="text-sm font-medium text-secondary-900">{{ formattedDate(task.endDate) }}</p>
                </div>
              </div>

              <!-- 负责人 -->
              <div class="flex items-center gap-2">
                <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <div>
                  <p class="text-xs text-secondary-500">{{ $t('dashboard.upcomingTasks.assignee') }}</p>
                  <p class="text-sm font-medium text-secondary-900">{{ getAssigneeName(task.assigneeId) }}</p>
                </div>
              </div>

              <!-- 剩余天数 -->
              <div class="flex items-center gap-2">
                <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div>
                  <p class="text-xs text-secondary-500">{{ $t('dashboard.upcomingTasks.remainingTime') }}</p>
                  <span :class="getDaysRemainingClass(task.endDate)" class="text-xs font-medium">
                    {{ getDaysRemainingLabel(task.endDate) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Card>
        </div>
      </Transition>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useOrgStore } from '@/stores/org';
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import type { OrgNode } from '@/types';
import dayjs from 'dayjs';

const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const orgStore = useOrgStore();

// 存储图表实例用于 resize
let taskChart: echarts.ECharts | null = null;
let projectChart: echarts.ECharts | null = null;

const taskChartRef = ref<HTMLElement>();
const projectChartRef = ref<HTMLElement>();

const currentUser = computed(() => userStore.currentUser);

// ============ 部门过滤(admin 专用,2026-06-17 新增) ============ //
const selectedDeptCode = ref<string | null>(currentUser.value?.deptCode ?? null);
const includeSubDepts = ref(false);
const switching = ref(false);
let switchTimer: number | null = null;

function onDeptChange(newCode: string | null) {
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

/** 部门二次过滤后的项目列表(基于现有 userProjects) */
const deptFilteredProjects = computed(() => {
  return userProjects.value.filter(p =>
    effectiveDeptCodes.value.has(p.deptCode ?? null)
  );
});

/** 部门二次过滤后的任务列表:只保留属于 deptFilteredProjects 项目的任务,
 *  用于驱动 statistics / planVsActual / upcomingTasks 等下游 computed,
 *  保证切换部门时任务相关数据同步刷新。 */
const deptFilteredTasks = computed(() => {
  const projectIds = new Set(deptFilteredProjects.value.map(p => p.id));
  return taskStore.tasks.filter(t => projectIds.has(t.projectId));
});

// 根据角色过滤项目:dept-pm 看所管部门 + 自己创建 / 参加的项目;member 看参与项目
// 2026-06-12:与后端 ProjectService.getAllProjectsForUser 的数据范围对齐——
// admin 全部;dept-pm 看所管部门;创建者 / owner / member 一律可看
const userProjects = computed(() => {
  if (!currentUser.value) return [];
  if (currentUser.value.role === 'admin' || currentUser.value.role === 'project-manager') {
    return projectStore.projects;
  }
  const userId = currentUser.value.id;
  const role = currentUser.value.role;
  const managedCodes = permissionStore.managedDeptCodes;
  return projectStore.projects.filter(p => {
    if (p.ownerId === userId) return true;
    if (p.createdBy === userId) return true;
    if (p.memberIds && p.memberIds.includes(userId)) return true;
    if (role === 'dept-project-manager' && p.deptCode && managedCodes.includes(p.deptCode)) {
      return true;
    }
    return false;
  });
});

// 根据角色过滤任务：只显示用户参与项目的任务
// 2026-06-17: 已不再使用,任务相关数据全部走 deptFilteredTasks(基于部门过滤后的项目列表)

const recentProjects = computed(() => deptFilteredProjects.value.slice(0, 5));
const upcomingTasks = computed(() => {
  // 只统计叶子任务（没有子任务的任务）
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));

  return leafTasks
    .filter(t => t.status !== 'done')
    .sort((a, b) => dayjs(a.endDate).valueOf() - dayjs(b.endDate).valueOf())
    .slice(0, 5);
});

// 从真实数据计算统计数据
const statistics = computed(() => {
  const projects = deptFilteredProjects.value;
  const totalProjects = projects.length;
  const activeProjects = projects.filter(p => p.status === 'active').length;
  const completedProjects = projects.filter(p => p.status === 'completed').length;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));

  const totalTasks = leafTasks.length;
  const completedTasks = leafTasks.filter(t => t.status === 'done').length;
  const inProgressTasks = leafTasks.filter(t => t.status === 'in-progress').length;
  const todoTasks = leafTasks.filter(t => t.status === 'todo').length;

  // 统计参与项目的总成员数
  const memberSet = new Set<string>();
  projects.forEach(p => {
    memberSet.add(p.ownerId);
    if (p.memberIds) {
      p.memberIds.forEach(id => memberSet.add(id));
    }
  });
  const totalMembers = memberSet.size;

  return {
    totalProjects,
    activeProjects,
    completedProjects,
    totalTasks,
    completedTasks,
    inProgressTasks,
    todoTasks,
    totalMembers
  };
});

// 预期进度 vs 实际进度
const planVsActual = computed(() => {
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id) && t.startDate && t.endDate);

  const totalLeafTasks = leafTasks.length;
  const completedTasks = leafTasks.filter(t => t.status === 'done').length;
  const actualPercent = totalLeafTasks > 0 ? Math.round((completedTasks / totalLeafTasks) * 100) : 0;

  // 预期完成率：基于计划日期的进度推算
  const today = dayjs().startOf('day');
  let totalPlannedProgress = 0;
  leafTasks.forEach(t => {
    const start = dayjs(t.startDate).startOf('day');
    const end = dayjs(t.endDate).startOf('day');
    const totalDays = end.diff(start, 'day');
    if (totalDays <= 0) {
      // 当天任务，如果已完成则100%，否则0%
      totalPlannedProgress += t.status === 'done' ? 1 : 0;
    } else {
      const elapsed = Math.min(today.diff(start, 'day'), totalDays);
      const plannedPercent = Math.max(0, Math.min(1, elapsed / totalDays));
      totalPlannedProgress += plannedPercent;
    }
  });
  const plannedPercent = totalLeafTasks > 0 ? Math.round((totalPlannedProgress / totalLeafTasks) * 100) : 0;

  const deviation = actualPercent - plannedPercent;

  return { plannedPercent, actualPercent, deviation, completedTasks, totalLeafTasks };
});

const planVsActualClass = computed(() => {
  const d = planVsActual.value.deviation;
  if (d >= 0) return 'text-success-600';
  if (d >= -10) return 'text-warning-600';
  return 'text-danger-600';
});

const formattedDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD');
};

const getStatusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    planning: 'projectStatus.planning',
    active: 'projectStatus.active',
    completed: 'projectStatus.completed',
    'on-hold': 'projectStatus.onHold'
  };
  const key = labelMap[status];
  return key ? t(key) : status;
};

const getStatusVariant = (status: string) => {
  const variants: Record<string, string> = {
    planning: 'info',
    active: 'primary',
    completed: 'success',
    'on-hold': 'warning'
  };
  return variants[status] || 'default';
};

const getPriorityLabel = (priority: string) => {
  const labelMap: Record<string, string> = {
    low: 'priorities.low',
    medium: 'priorities.medium',
    high: 'priorities.high',
    urgent: 'priorities.urgent'
  };
  const key = labelMap[priority];
  return key ? t(key) : priority;
};

const getPriorityVariant = (priority: string) => {
  const variants: Record<string, string> = {
    low: 'default',
    medium: 'info',
    high: 'warning',
    urgent: 'danger'
  };
  return variants[priority] || 'default';
};

// 计算距离结束日期的剩余天数
const getDaysRemaining = (endDate: string): number => {
  const today = dayjs().startOf('day');
  const end = dayjs(endDate).startOf('day');
  return end.diff(today, 'day');
};

// 获取剩余天数标签
const getDaysRemainingLabel = (endDate: string): string => {
  const days = getDaysRemaining(endDate);

  if (days < 0) {
    return t('dashboard.upcomingTasks.daysRemaining.overdue', { days: Math.abs(days) });
  } else if (days === 0) {
    return t('dashboard.upcomingTasks.daysRemaining.today');
  } else if (days === 1) {
    return t('dashboard.upcomingTasks.daysRemaining.tomorrow');
  } else {
    return t('dashboard.upcomingTasks.daysRemaining.days', { days });
  }
};

// 获取剩余天数的样式类
const getDaysRemainingClass = (endDate: string): string => {
  const days = getDaysRemaining(endDate);

  if (days < 0) {
    return 'text-danger-600';
  } else if (days === 0) {
    return 'text-danger-600';
  } else if (days <= 3) {
    return 'text-warning-600';
  } else if (days <= 7) {
    return 'text-info-600';
  } else {
    return 'text-secondary-600';
  }
};

// 获取项目名称
const getProjectName = (projectId: string): string => {
  const project = projectStore.projectById(projectId);
  return project?.name || projectId;
};

// 获取负责人名称
const getAssigneeName = (assigneeId: string | undefined): string => {
  if (!assigneeId) return t('dashboard.upcomingTasks.unassigned');
  const user = userStore.users.find(u => u.id === assigneeId);
  return user?.name || t('dashboard.upcomingTasks.unassigned');
};

const initTaskChart = () => {
  if (!taskChartRef.value) return;

  // 销毁已存在的图表实例
  if (taskChart) {
    taskChart.dispose();
  }

  taskChart = echarts.init(taskChartRef.value);

  // 检查是否有任务数据
  if (statistics.value.totalTasks === 0) {
    // 显示空状态
    const option = {
      title: {
        text: t('dashboard.charts.noTaskData'),
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#94a3b8',
          fontSize: 14
        }
      }
    };
    taskChart.setOption(option);
    return;
  }

  // 准备图表数据
  const chartData = [
    { value: statistics.value.todoTasks, name: t('taskStatus.todo'), itemStyle: { color: '#94a3b8' } },
    { value: statistics.value.inProgressTasks, name: t('taskStatus.inProgress'), itemStyle: { color: '#3b82f6' } },
    { value: statistics.value.completedTasks, name: t('taskStatus.done'), itemStyle: { color: '#10b981' } }
  ];

  // 计算总数用于百分比显示
  const totalTasks = chartData.reduce((sum, item) => sum + item.value, 0);

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const percent = totalTasks > 0 ? Math.round((params.value / totalTasks) * 100) : 0;
        return `${params.name}: ${params.value} (${percent}%)`;
      }
    },
    legend: {
      bottom: '0%',
      left: 'center',
      formatter: (name: string) => {
        const item = chartData.find(d => d.name === name);
        if (item && item.value > 0 && totalTasks > 0) {
          const percent = Math.round((item.value / totalTasks) * 100);
          return `${name} ${percent}%`;
        }
        return name;
      }
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: chartData
      }
    ]
  };
  taskChart.setOption(option);
};

const initProjectChart = () => {
  if (!projectChartRef.value) return;

  // 销毁已存在的图表实例
  if (projectChart) {
    projectChart.dispose();
  }

  projectChart = echarts.init(projectChartRef.value);

  const chartProjects = deptFilteredProjects.value.slice(0, 5);

  // 检查是否有项目数据
  if (chartProjects.length === 0) {
    // 显示空状态
    const option = {
      title: {
        text: t('dashboard.charts.noProjectData'),
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#94a3b8',
          fontSize: 14
        }
      }
    };
    projectChart.setOption(option);
    return;
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const project = chartProjects[param.dataIndex];
        return `${project.name}<br/>${t('dashboard.recentProjects.progress')}: ${project.progress}%`;
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartProjects.map(p => p.name.length > 6 ? p.name.substring(0, 6) + '...' : p.name),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: t('dashboard.recentProjects.progress'),
        type: 'bar',
        data: chartProjects.map(p => ({
          value: p.progress || 0,
          itemStyle: { color: p.color || '#3b82f6' }
        })),
        barWidth: '60%',
        label: {
          show: true,
          position: 'top',
          formatter: '{c}%'
        }
      }
    ]
  };
  projectChart.setOption(option);
};

onMounted(async () => {
  // 加载数据
  await Promise.all([
    projectStore.loadProjects(),
    taskStore.loadTasks(),
    userStore.loadUsers()
  ]);

  // 初始化图表
  await nextTick();
  initTaskChart();
  initProjectChart();

  // 添加窗口大小变化监听
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize);

  // 销毁图表实例
  if (taskChart) {
    taskChart.dispose();
    taskChart = null;
  }
  if (projectChart) {
    projectChart.dispose();
    projectChart = null;
  }
});

// 处理窗口大小变化
const handleResize = () => {
  if (taskChart) {
    taskChart.resize();
  }
  if (projectChart) {
    projectChart.resize();
  }
};

// 监听数据变化，重新渲染图表
watch([() => projectStore.loaded, () => taskStore.loaded], async ([projectsLoaded, tasksLoaded]) => {
  if (projectsLoaded && tasksLoaded) {
    await nextTick();
    initTaskChart();
    initProjectChart();
  }
}, { immediate: false });

// 2026-06-17: 切换部门 / 含子部门勾选时,部门过滤后的 statistics / 图表数据已经变化,
// 但 echarts 是命令式渲染,需要主动重新调用 init 重绘图表。
// <Transition> 会随 key 变化销毁并重建图表容器,所以:
//  1) 不能给 <Transition> 加 mode="out-in",否则 leave 期间 ref 仍指向旧容器,
//     init 会把图表挂到旧容器上,新容器出现后就没有图表了;
//  2) 用 flush: 'post' 让 watch 在 DOM patch 之后再触发,此时新容器已挂载、
//     ref 已更新,dispose 检查能识别出旧实例并清掉,然后 init 到新容器上。
watch([selectedDeptCode, includeSubDepts], async () => {
  await nextTick();
  if (taskChart && (!taskChartRef.value || taskChart.getDom() !== taskChartRef.value)) {
    taskChart.dispose();
    taskChart = null;
  }
  if (projectChart && (!projectChartRef.value || projectChart.getDom() !== projectChartRef.value)) {
    projectChart.dispose();
    projectChart = null;
  }
  initTaskChart();
  initProjectChart();
}, { flush: 'post' });
</script>

<style scoped>
/* 部门筛选行切换遮罩淡入淡出(2026-06-17) */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 100ms ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 部门切换时下游内容(统计/图表/列表)淡入淡出 */
.content-fade-enter-active,
.content-fade-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.content-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.content-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
