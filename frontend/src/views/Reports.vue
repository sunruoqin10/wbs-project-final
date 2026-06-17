<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">{{ $t('reports.title') }}</h1>
        <p class="mt-1 text-sm text-secondary-600">{{ $t('reports.subtitle') }}</p>
      </div>

      <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见,与 Dashboard 对齐) -->
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

      <!-- 注意:不要给 <Transition> 加 mode="out-in",否则旧 div 的 200ms leave 动画期间
           新的图表容器还没被创建,ref 仍指向旧 div,watch 里 init 出来的
           图表会挂到旧 div 上,等 leave 结束旧 div 被销毁后,新 div 上就没有图表了。 -->
      <Transition name="content-fade">
        <div :key="`${selectedDeptCode}-${includeSubDepts}`" class="space-y-6">
          <!-- Summary Stats -->
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
                  <p class="text-sm font-medium text-secondary-600">{{ $t('reports.stats.totalProjects') }}</p>
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
                  <p class="text-sm font-medium text-secondary-600">{{ $t('reports.stats.completionRate') }}</p>
                  <p class="text-2xl font-semibold text-secondary-900">{{ completionRate }}%</p>
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
                  <p class="text-sm font-medium text-secondary-600">{{ $t('reports.stats.totalTasks') }}</p>
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
                      d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('reports.stats.inProgressTasks') }}</p>
                  <p class="text-2xl font-semibold text-secondary-900">{{ statistics.inProgressTasks }}</p>
                </div>
              </div>
            </Card>
          </div>

          <!-- Charts -->
          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <!-- Project Status Distribution -->
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('reports.charts.projectStatusDistribution') }}</h3>
              </template>
              <div class="h-80" ref="projectStatusChartRef"></div>
            </Card>

            <!-- Task Priority Distribution -->
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('reports.charts.taskPriorityDistribution') }}</h3>
              </template>
              <div class="h-80" ref="taskPriorityChartRef"></div>
            </Card>

            <!-- Project Completion Progress -->
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('reports.charts.projectCompletionProgress') }}</h3>
              </template>
              <div class="h-80" ref="projectProgressChartRef"></div>
            </Card>

            <!-- Team Performance -->
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('reports.charts.teamPerformance') }}</h3>
              </template>
              <div class="h-80" ref="teamPerformanceChartRef"></div>
            </Card>
          </div>

          <!-- Export Options -->
          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ $t('reports.export.title') }}</h3>
            </template>
            <div class="flex justify-start">
              <button @click="exportComprehensive" :disabled="isExporting" class="flex items-center justify-center gap-2 rounded-lg bg-primary-50 px-8 py-4 transition-colors hover:bg-primary-100 disabled:opacity-50 disabled:cursor-not-allowed">
                <svg v-if="!isExporting" class="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <svg v-else class="h-6 w-6 text-primary-600 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <div class="text-left">
                  <div class="font-medium text-primary-900">{{ isExporting ? '正在导出...' : $t('reports.export.comprehensiveReport') }}</div>
                  <div class="text-xs text-primary-600">{{ isExporting ? '请稍候' : $t('reports.export.comprehensiveReportDesc') }}</div>
                </div>
              </button>

              <button @click="exportGantt" :disabled="isExporting" class="ml-4 flex items-center justify-center gap-2 rounded-lg border border-primary-200 bg-white px-8 py-4 transition-colors hover:bg-primary-50 disabled:opacity-50 disabled:cursor-not-allowed">
                <svg v-if="!isExporting" class="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <svg v-else class="h-6 w-6 text-primary-600 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <div class="text-left">
                  <div class="font-medium text-primary-900">{{ isExporting ? '正在导出...' : '导出甘特图' }}</div>
                  <div class="text-xs text-primary-600">{{ isExporting ? '请稍候' : '每个项目一个工作表' }}</div>
                </div>
              </button>
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
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useOrgStore } from '@/stores/org';
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import type { OrgNode } from '@/types';
import { exportToExcelNamespace } from '@/utils/export';

const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const orgStore = useOrgStore();

// 图表容器 ref
const projectStatusChartRef = ref<HTMLElement>();
const taskPriorityChartRef = ref<HTMLElement>();
const projectProgressChartRef = ref<HTMLElement>();
const teamPerformanceChartRef = ref<HTMLElement>();

// 图表实例(2026-06-17: 用于 dispose / resize / watch 重绘)
let projectStatusChart: echarts.ECharts | null = null;
let taskPriorityChart: echarts.ECharts | null = null;
let projectProgressChart: echarts.ECharts | null = null;
let teamPerformanceChart: echarts.ECharts | null = null;

const isExporting = ref(false);

// 根据数据范围过滤项目:统一走 permissionStore.canViewProject
// (admin / creator / owner / member / dept-pm 五档,与后端 PermissionService.canViewProject 对齐)
const userProjects = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return projectStore.projects.filter(p => permissionStore.canViewProject(p.id));
});

// ============ 部门过滤(admin 专用,2026-06-17 新增,与 Dashboard 对齐) ============ //
const currentUser = computed(() => userStore.currentUser);
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

/** 部门二次过滤后的任务列表:只保留属于 deptFilteredProjects 项目的任务 */
const deptFilteredTasks = computed(() => {
  const projectIds = new Set(deptFilteredProjects.value.map(p => p.id));
  return taskStore.tasks.filter(t => projectIds.has(t.projectId));
});

// 导出用用户子集:"出现在可见范围内"的所有用户
//   = 可见项目的 owner ∪ 可见任务的 assignee
// 理由:exportComprehensive 内部按 data.users.find(ownerId) 查项目负责人名称
//      (export.ts L141-156),若 owner 不在此子集,owner 单元格会落到 '未分配' fallback
const usersInScope = computed(() => {
  const ownerIds = new Set(deptFilteredProjects.value.map(p => p.ownerId));
  const assigneeIds = new Set(deptFilteredTasks.value.map(t => t.assigneeId).filter(Boolean));
  const scopedIds = new Set([...ownerIds, ...assigneeIds]);
  return userStore.users.filter(u => scopedIds.has(u.id));
});

// 从真实数据计算统计数据
const statistics = computed(() => {
  const projects = deptFilteredProjects.value;
  const totalProjects = projects.length;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));

  const totalTasks = leafTasks.length;
  const completedTasks = leafTasks.filter(t => t.status === 'done').length;
  const inProgressTasks = leafTasks.filter(t => t.status === 'in-progress').length;

  return {
    totalProjects,
    totalTasks,
    completedTasks,
    inProgressTasks
  };
});

const completionRate = computed(() => {
  if (statistics.value.totalTasks === 0) return 0;
  return Math.round((statistics.value.completedTasks / statistics.value.totalTasks) * 100);
});

const initProjectStatusChart = () => {
  if (!projectStatusChartRef.value) return;

  if (projectStatusChart) projectStatusChart.dispose();
  const chart = echarts.init(projectStatusChartRef.value);
  projectStatusChart = chart;

  const projects = deptFilteredProjects.value;
  const statusData = [
    { value: projects.filter(p => p.status === 'planning').length, name: t('reports.statuses.planning'), itemStyle: { color: '#0891b2' } },
    { value: projects.filter(p => p.status === 'active').length, name: t('reports.statuses.active'), itemStyle: { color: '#3b82f6' } },
    { value: projects.filter(p => p.status === 'completed').length, name: t('reports.statuses.completed'), itemStyle: { color: '#10b981' } },
    { value: projects.filter(p => p.status === 'on-hold').length, name: t('reports.statuses.onHold'), itemStyle: { color: '#f59e0b' } }
  ];

  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: '0%',
      left: 'center'
    },
    series: [
      {
        name: t('reports.charts.projectStatus'),
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: statusData
      }
    ]
  };

  chart.setOption(option);
};

const initTaskPriorityChart = () => {
  if (!taskPriorityChartRef.value) return;

  if (taskPriorityChart) taskPriorityChart.dispose();
  const chart = echarts.init(taskPriorityChartRef.value);
  taskPriorityChart = chart;

  // 只统计叶子任务（没有子任务的任务）
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));

  const priorityData = [
    { value: leafTasks.filter(t => t.priority === 'low').length, name: t('reports.priorities.low'), itemStyle: { color: '#64748b' } },
    { value: leafTasks.filter(t => t.priority === 'medium').length, name: t('reports.priorities.medium'), itemStyle: { color: '#0891b2' } },
    { value: leafTasks.filter(t => t.priority === 'high').length, name: t('reports.priorities.high'), itemStyle: { color: '#d97706' } },
    { value: leafTasks.filter(t => t.priority === 'urgent').length, name: t('reports.priorities.urgent'), itemStyle: { color: '#dc2626' } }
  ];

  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: '0%',
      left: 'center'
    },
    series: [
      {
        name: t('reports.charts.taskPriority'),
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: priorityData
      }
    ]
  };

  chart.setOption(option);
};

const initProjectProgressChart = () => {
  if (!projectProgressChartRef.value) return;

  if (projectProgressChart) projectProgressChart.dispose();
  const chart = echarts.init(projectProgressChartRef.value);
  projectProgressChart = chart;

  const projects = deptFilteredProjects.value;

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
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
      data: projects.map(p => p.name.length > 6 ? p.name.substring(0, 6) + '...' : p.name),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      max: 100,
      name: `${t('reports.charts.progress')}(%`
    },
    series: [
      {
        name: t('reports.charts.progress'),
        type: 'bar',
        data: projects.map(p => ({
          value: p.progress,
          itemStyle: { color: p.color }
        })),
        barWidth: '60%'
      }
    ]
  };

  chart.setOption(option);
};

const initTeamPerformanceChart = () => {
  if (!teamPerformanceChartRef.value) return;

  if (teamPerformanceChart) teamPerformanceChart.dispose();
  const chart = echarts.init(teamPerformanceChartRef.value);
  teamPerformanceChart = chart;

  // 只统计叶子任务（没有子任务的任务）
  const tasks = deptFilteredTasks.value;
  const allTaskIds = new Set(tasks.map(t => t.id));
  const parentTaskIds = new Set(tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = tasks.filter(t => leafTaskIds.has(t.id));

  // 只统计有项目任务的成员
  const userData = userStore.users
    .map(user => ({
      name: user.name,
      completed: leafTasks.filter(t => t.assigneeId === user.id && t.status === 'done').length,
      inProgress: leafTasks.filter(t => t.assigneeId === user.id && t.status === 'in-progress').length
    }))
    .filter(u => u.completed > 0 || u.inProgress > 0);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: [t('reports.charts.completed'), t('reports.charts.inProgress')]
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: userData.map(u => u.name)
    },
    yAxis: {
      type: 'value',
      name: t('reports.charts.taskCount')
    },
    series: [
      {
        name: t('reports.charts.completed'),
        type: 'bar',
        stack: 'total',
        data: userData.map(u => u.completed),
        itemStyle: { color: '#10b981' }
      },
      {
        name: t('reports.charts.inProgress'),
        type: 'bar',
        stack: 'total',
        data: userData.map(u => u.inProgress),
        itemStyle: { color: '#3b82f6' }
      }
    ]
  };

  chart.setOption(option);
};

const initAllCharts = () => {
  initProjectStatusChart();
  initTaskPriorityChart();
  initProjectProgressChart();
  initTeamPerformanceChart();
};

onMounted(async () => {
  // 加载数据
  await Promise.all([
    projectStore.loadProjects(),
    taskStore.loadTasks(),
    userStore.loadUsers(),
    orgStore.loadTree()
  ]);

  // 初始化图表
  await nextTick();
  initAllCharts();

  // 添加窗口大小变化监听
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize);

  // 销毁图表实例
  if (projectStatusChart) projectStatusChart.dispose();
  if (taskPriorityChart) taskPriorityChart.dispose();
  if (projectProgressChart) projectProgressChart.dispose();
  if (teamPerformanceChart) teamPerformanceChart.dispose();
  projectStatusChart = null;
  taskPriorityChart = null;
  projectProgressChart = null;
  teamPerformanceChart = null;
});

// 处理窗口大小变化
const handleResize = () => {
  projectStatusChart?.resize();
  taskPriorityChart?.resize();
  projectProgressChart?.resize();
  teamPerformanceChart?.resize();
};

// 2026-06-17: 切换部门 / 含子部门勾选时,部门过滤后的 statistics / 图表数据已经变化,
// 但 echarts 是命令式渲染,需要主动重新调用 init 重绘图表。
// <Transition> 会随 key 变化销毁并重建图表容器,所以:
//  1) 不能给 <Transition> 加 mode="out-in",否则 leave 期间 ref 仍指向旧容器,
//     init 会把图表挂到旧容器上,新容器出现后就没有图表了;
//  2) 用 flush: 'post' 让 watch 在 DOM patch 之后再触发,此时新容器已挂载、
//     ref 已更新,dispose 检查能识别出旧实例并清掉,然后 init 到新容器上。
watch([selectedDeptCode, includeSubDepts], async () => {
  await nextTick();
  if (projectStatusChart && (!projectStatusChartRef.value || projectStatusChart.getDom() !== projectStatusChartRef.value)) {
    projectStatusChart.dispose();
    projectStatusChart = null;
  }
  if (taskPriorityChart && (!taskPriorityChartRef.value || taskPriorityChart.getDom() !== taskPriorityChartRef.value)) {
    taskPriorityChart.dispose();
    taskPriorityChart = null;
  }
  if (projectProgressChart && (!projectProgressChartRef.value || projectProgressChart.getDom() !== projectProgressChartRef.value)) {
    projectProgressChart.dispose();
    projectProgressChart = null;
  }
  if (teamPerformanceChart && (!teamPerformanceChartRef.value || teamPerformanceChart.getDom() !== teamPerformanceChartRef.value)) {
    teamPerformanceChart.dispose();
    teamPerformanceChart = null;
  }
  initAllCharts();
}, { flush: 'post' });

// Export function
const exportComprehensive = async () => {
  const projects = deptFilteredProjects.value;
  const tasks = deptFilteredTasks.value;
  if (projects.length === 0) {
    alert(t('reports.messages.noData'));
    return;
  }

  if (isExporting.value) return;
  isExporting.value = true;

  try {
    // 使用 setTimeout 让 UI 有机会更新
    await new Promise(resolve => setTimeout(resolve, 50));

    // 导出综合 Excel 报表
    exportToExcelNamespace.comprehensive(
      {
        projects,
        tasks: tasks,
        users: usersInScope.value,
        stats: statistics.value
      },
      `${t('reports.export.comprehensiveReport')}_${getTimestamp()}.xlsx`
    );
    alert(`${t('reports.export.comprehensiveReport')}${t('reports.messages.exportSuccess')}`);
  } catch (error) {
    console.error('Export failed:', error);
    alert(t('reports.messages.exportFailed'));
  } finally {
    isExporting.value = false;
  }
};

// Export Gantt function
const exportGantt = async () => {
  const projects = deptFilteredProjects.value;
  const tasks = deptFilteredTasks.value;
  if (projects.length === 0) {
    alert(t('reports.messages.noData'));
    return;
  }

  if (isExporting.value) return;
  isExporting.value = true;

  try {
    // 使用 setTimeout 让 UI 有机会更新
    await new Promise(resolve => setTimeout(resolve, 50));

    // 导出甘特图 Excel
    await exportToExcelNamespace.gantt(
      {
        projects,
        tasks: tasks,
        users: usersInScope.value
      },
      `甘特图_${getTimestamp()}.xlsx`
    );
    alert('甘特图导出成功！');
  } catch (error) {
    console.error('Export failed:', error);
    alert('导出失败！');
  } finally {
    isExporting.value = false;
  }
};

// Helper function to generate timestamp
const getTimestamp = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hour = String(now.getHours()).padStart(2, '0');
  const minute = String(now.getMinutes()).padStart(2, '0');
  return `${year}${month}${day}_${hour}${minute}`;
};
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