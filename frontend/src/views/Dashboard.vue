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

      <!-- Stats Cards -->
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
import dayjs from 'dayjs';

const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

// 存储图表实例用于 resize
let taskChart: echarts.ECharts | null = null;
let projectChart: echarts.ECharts | null = null;

const taskChartRef = ref<HTMLElement>();
const projectChartRef = ref<HTMLElement>();

const currentUser = computed(() => userStore.currentUser);
const recentProjects = computed(() => projectStore.projects.slice(0, 5));
const upcomingTasks = computed(() => {
  // 只统计叶子任务（没有子任务的任务）
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  return leafTasks
    .filter(t => t.status !== 'done')
    .sort((a, b) => dayjs(a.endDate).valueOf() - dayjs(b.endDate).valueOf())
    .slice(0, 5);
});

// 从真实数据计算统计数据
const statistics = computed(() => {
  const totalProjects = projectStore.projects.length;
  const activeProjects = projectStore.projects.filter(p => p.status === 'active').length;
  const completedProjects = projectStore.projects.filter(p => p.status === 'completed').length;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  // 一个任务是叶子任务的条件是：没有任何任务的 parentTaskId 指向它
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));

  // 叶子任务 = 所有的任务ID - 有子任务的父任务ID
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));

  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  // 调试日志
  console.log('===== Dashboard 统计调试 =====');
  console.log('所有任务数量:', taskStore.tasks.length);
  console.log('所有任务:', taskStore.tasks.map(t => ({ id: t.id, title: t.title, status: t.status, parentTaskId: t.parentTaskId })));
  console.log('父任务ID列表:', Array.from(parentTaskIds));
  console.log('叶子任务ID列表:', Array.from(leafTaskIds));
  console.log('叶子任务:', leafTasks.map(t => ({ id: t.id, title: t.title, status: t.status })));
  console.log('待办任务数:', leafTasks.filter(t => t.status === 'todo').length);
  console.log('进行中任务数:', leafTasks.filter(t => t.status === 'in-progress').length);
  console.log('已完成任务数:', leafTasks.filter(t => t.status === 'done').length);
  console.log('===== 调试结束 =====');
  const totalTasks = leafTasks.length;
  const completedTasks = leafTasks.filter(t => t.status === 'done').length;
  const inProgressTasks = leafTasks.filter(t => t.status === 'in-progress').length;
  const todoTasks = leafTasks.filter(t => t.status === 'todo').length;
  const totalMembers = userStore.users.length;

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

  // 检查是否有项目数据
  if (recentProjects.value.length === 0) {
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
        const project = recentProjects.value[param.dataIndex];
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
      data: recentProjects.value.map(p => p.name.length > 6 ? p.name.substring(0, 6) + '...' : p.name),
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
        data: recentProjects.value.map(p => ({
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
</script>
