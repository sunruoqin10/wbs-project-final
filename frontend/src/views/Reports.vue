<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">{{ $t('reports.title') }}</h1>
        <p class="mt-1 text-sm text-secondary-600">{{ $t('reports.subtitle') }}</p>
      </div>

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

      <!-- 延期统计卡片 -->
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <!-- 延期任务数 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-danger-100 p-3">
              <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">延期任务数</p>
              <p class="text-2xl font-semibold text-danger-600">{{ delayStats.delayedTasks }}</p>
            </div>
          </div>
        </Card>

        <!-- 累计延期天数 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-warning-100 p-3">
              <svg class="h-6 w-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">累计延期天数</p>
              <p class="text-2xl font-semibold text-warning-600">{{ delayStats.totalDelayedDays }}</p>
            </div>
          </div>
        </Card>

        <!-- 延期率 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-info-100 p-3">
              <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">延期率</p>
              <p class="text-2xl font-semibold text-info-600">{{ delayStats.delayRate.toFixed(1) }}%</p>
            </div>
          </div>
        </Card>

        <!-- 严重延期任务 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-accent-100 p-3">
              <svg class="h-6 w-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">严重延期（≥7天）</p>
              <p class="text-2xl font-semibold text-accent-600">{{ delayStats.criticalDelayedTasks }}</p>
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
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <button @click="exportProjectsExcel" class="flex items-center justify-center gap-2 rounded-lg border border-secondary-200 p-4 transition-colors hover:bg-secondary-50">
            <svg class="h-6 w-6 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <div class="text-left">
              <div class="font-medium text-secondary-900">{{ $t('reports.export.projectExcel') }}</div>
              <div class="text-xs text-secondary-500">{{ $t('reports.export.projectExcelDesc') }}</div>
            </div>
          </button>

          <button @click="exportStatisticsExcel" class="flex items-center justify-center gap-2 rounded-lg border border-secondary-200 p-4 transition-colors hover:bg-secondary-50">
            <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <div class="text-left">
              <div class="font-medium text-secondary-900">{{ $t('reports.export.statisticsExcel') }}</div>
              <div class="text-xs text-secondary-500">{{ $t('reports.export.statisticsExcelDesc') }}</div>
            </div>
          </button>

          <button @click="exportComprehensive" class="flex items-center justify-center gap-2 rounded-lg bg-primary-50 p-4 transition-colors hover:bg-primary-100">
            <svg class="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <div class="text-left">
              <div class="font-medium text-primary-900">{{ $t('reports.export.comprehensiveReport') }}</div>
              <div class="text-xs text-primary-600">{{ $t('reports.export.comprehensiveReportDesc') }}</div>
            </div>
          </button>
        </div>
      </Card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import { exportToExcel } from '@/utils/export';

const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

const projectStatusChartRef = ref<HTMLElement>();
const taskPriorityChartRef = ref<HTMLElement>();
const projectProgressChartRef = ref<HTMLElement>();
const teamPerformanceChartRef = ref<HTMLElement>();

// 从真实数据计算统计数据
const statistics = computed(() => {
  const totalProjects = projectStore.projects.length;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

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

// 延期统计
const delayStats = computed(() => {
  const allTasks = taskStore.tasks;
  const today = new Date();

  // 计算延期任务
  const delayedTasks = allTasks.filter(t => {
    if (t.status === 'done') return false;
    const endDate = new Date(t.endDate);
    return endDate < today;
  });

  // 计算累计延期天数
  const totalDelayedDays = allTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);

  // 计算严重延期任务（≥7天）
  const criticalDelayedTasks = allTasks.filter(t => (t.delayedDays || 0) >= 7).length;

  // 计算延期率
  const delayRate = allTasks.length > 0 ? (delayedTasks.length / allTasks.length) * 100 : 0;

  return {
    delayedTasks: delayedTasks.length,
    totalDelayedDays,
    delayRate,
    criticalDelayedTasks
  };
});

const initProjectStatusChart = () => {
  if (!projectStatusChartRef.value) return;

  const chart = echarts.init(projectStatusChartRef.value);
  const statusData = [
    { value: projectStore.projects.filter(p => p.status === 'planning').length, name: t('reports.statuses.planning'), itemStyle: { color: '#0891b2' } },
    { value: projectStore.projects.filter(p => p.status === 'active').length, name: t('reports.statuses.active'), itemStyle: { color: '#3b82f6' } },
    { value: projectStore.projects.filter(p => p.status === 'completed').length, name: t('reports.statuses.completed'), itemStyle: { color: '#10b981' } },
    { value: projectStore.projects.filter(p => p.status === 'on-hold').length, name: t('reports.statuses.onHold'), itemStyle: { color: '#f59e0b' } }
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

  const chart = echarts.init(taskPriorityChartRef.value);

  // 只统计叶子任务（没有子任务的任务）
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

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

  const chart = echarts.init(projectProgressChartRef.value);

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
      data: projectStore.projects.map(p => p.name.length > 6 ? p.name.substring(0, 6) + '...' : p.name),
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
        data: projectStore.projects.map(p => ({
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

  const chart = echarts.init(teamPerformanceChartRef.value);

  // 只统计叶子任务（没有子任务的任务）
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  const userData = userStore.users.map(user => ({
    name: user.name,
    completed: leafTasks.filter(t => t.assigneeId === user.id && t.status === 'done').length,
    inProgress: leafTasks.filter(t => t.assigneeId === user.id && t.status === 'in-progress').length
  }));

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

onMounted(async () => {
  // 加载数据
  await Promise.all([
    projectStore.loadProjects(),
    taskStore.loadTasks(),
    userStore.loadUsers()
  ]);

  // 初始化图表
  setTimeout(() => {
    initProjectStatusChart();
    initTaskPriorityChart();
    initProjectProgressChart();
    initTeamPerformanceChart();
  }, 100);
});

// Export functions
const exportProjectsExcel = () => {
  if (projectStore.projects.length === 0) {
    alert(t('reports.messages.noProjectData'));
    return;
  }
  try {
    exportToExcel.projects(projectStore.projects, `${t('reports.export.projectExcel')}_${getTimestamp()}.xlsx`);
    alert(`${t('reports.export.projectExcelDesc')}${t('reports.messages.exportSuccess')}`);
  } catch (error) {
    console.error('Export failed:', error);
    alert(t('reports.messages.exportFailed'));
  }
};

const exportStatisticsExcel = () => {
  try {
    exportToExcel.statistics(
      {
        stats: statistics.value,
        projects: projectStore.projects,
        tasks: taskStore.tasks,
        users: userStore.users
      },
      `${t('reports.export.statisticsExcel')}_${getTimestamp()}.xlsx`
    );
    alert(`${t('reports.messages.exportSuccess')}`);
  } catch (error) {
    console.error('Export failed:', error);
    alert(t('reports.messages.exportFailed'));
  }
};

const exportComprehensive = () => {
  if (projectStore.projects.length === 0) {
    alert(t('reports.messages.noData'));
    return;
  }

  try {
    // 导出综合 Excel 报表
    exportToExcel.comprehensive(
      {
        projects: projectStore.projects,
        tasks: taskStore.tasks,
        users: userStore.users,
        stats: statistics.value
      },
      `${t('reports.export.comprehensiveReport')}_${getTimestamp()}.xlsx`
    );
    alert(`${t('reports.export.comprehensiveReport')}${t('reports.messages.exportSuccess')}`);
  } catch (error) {
    console.error('Export failed:', error);
    alert(t('reports.messages.exportFailed'));
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
