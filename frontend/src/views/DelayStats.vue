<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">项目延期统计</h1>
        <p class="mt-1 text-sm text-secondary-600">查看项目的延期情况统计和分析</p>
      </div>

      <!-- 延期统计卡片（叶子任务） -->
      <div>
        <div class="mb-3 flex items-center gap-2">
          <h2 class="text-lg font-semibold text-secondary-900">延期统计（叶子任务）</h2>
          <span class="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
            叶子任务总数: {{ delayStats.totalLeafTasks }}
          </span>
        </div>
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
          <!-- 延期叶子任务数 -->
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

          <!-- 严重延期（≥7天） -->
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

          <!-- 中度延期（3-6天） -->
          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-orange-100 p-3">
                <svg class="h-6 w-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">中度延期（3-6天）</p>
                <p class="text-2xl font-semibold text-orange-600">{{ delayStats.warningDelayedTasks }}</p>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- Charts -->
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- 延期任务分布 -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">延期任务分布（叶子任务）</h3>
          </template>
          <div class="h-80" ref="delayDistributionChartRef"></div>
        </Card>

        <!-- 项目延期排名 -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">项目延期排名</h3>
          </template>
          <div class="h-80" ref="projectDelayRankingChartRef"></div>
        </Card>

        <!-- 延期任务列表 -->
        <Card class="lg:col-span-2">
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-secondary-900">延期任务详情</h3>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">筛选：</label>
                <select v-model="delayFilter" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="all">全部延期</option>
                  <option value="critical">严重延期（≥7天）</option>
                  <option value="warning">中度延期（3-6天）</option>
                  <option value="minor">轻微延期（<3天）</option>
                </select>
              </div>
            </div>
          </template>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-secondary-200">
              <thead class="bg-secondary-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">任务名称</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">所属项目</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">延期天数</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">严重程度</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">结束日期</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">负责人</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-secondary-200 bg-white">
                <tr v-if="filteredDelayedTasks.length === 0">
                  <td colspan="6" class="px-4 py-8 text-center text-sm text-secondary-500">
                    暂无延期任务
                  </td>
                </tr>
                <tr v-for="task in filteredDelayedTasks" :key="task.id" class="hover:bg-secondary-50">
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-secondary-900">{{ task.title }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getProjectName(task.projectId) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold" :class="getDelayTextClass(task.delayedDays || 0)">
                    {{ task.delayedDays || 0 }} 天
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span :class="getDelayBadgeClass(task.delayedDays || 0)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                      {{ getDelaySeverityLabel(task.delayedDays || 0) }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ formatDate(task.endDate) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getAssigneeName(task.assigneeId) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import type { Task } from '@/types';
import dayjs from 'dayjs';

const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

const delayDistributionChartRef = ref<HTMLElement>();
const projectDelayRankingChartRef = ref<HTMLElement>();
const delayFilter = ref<'all' | 'critical' | 'warning' | 'minor'>('all');

// 延期统计 - 只统计叶子任务（没有子任务的任务）
const delayStats = computed(() => {
  const allTasks = taskStore.tasks;

  // 获取所有叶子任务（没有子任务的任务）
  const allTaskIds = new Set(allTasks.map(t => t.id));
  const parentTaskIds = new Set(allTasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = allTasks.filter(t => leafTaskIds.has(t.id));

  const today = new Date();

  // 计算延期的叶子任务
  const delayedTasks = leafTasks.filter(t => {
    if (t.status === 'done') return false;
    const endDate = new Date(t.endDate);
    return endDate < today;
  });

  // 计算叶子任务的累计延期天数
  const totalDelayedDays = leafTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);

  // 计算严重延期的叶子任务（≥7天）
  const criticalDelayedTasks = leafTasks.filter(t => (t.delayedDays || 0) >= 7).length;

  // 计算中度延期的叶子任务（3-6天）
  const warningDelayedTasks = leafTasks.filter(t => {
    const days = t.delayedDays || 0;
    return days >= 3 && days < 7;
  }).length;

  // 计算轻微延期的叶子任务（<3天）
  const minorDelayedTasks = leafTasks.filter(t => {
    const days = t.delayedDays || 0;
    return days > 0 && days < 3;
  }).length;

  // 计算延期率（基于叶子任务总数）
  const delayRate = leafTasks.length > 0 ? (delayedTasks.length / leafTasks.length) * 100 : 0;

  return {
    delayedTasks: delayedTasks.length,
    totalDelayedDays,
    delayRate,
    criticalDelayedTasks,
    warningDelayedTasks,
    minorDelayedTasks,
    totalLeafTasks: leafTasks.length  // 叶子任务总数
  };
});

// 获取所有延期的叶子任务
const delayedLeafTasks = computed(() => {
  const allTasks = taskStore.tasks;

  // 获取所有叶子任务
  const allTaskIds = new Set(allTasks.map(t => t.id));
  const parentTaskIds = new Set(allTasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = allTasks.filter(t => leafTaskIds.has(t.id));

  // 返回延期的叶子任务
  return leafTasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
});

// 根据筛选条件过滤延期任务
const filteredDelayedTasks = computed(() => {
  const tasks = delayedLeafTasks.value;

  switch (delayFilter.value) {
    case 'critical':
      return tasks.filter(t => (t.delayedDays || 0) >= 7);
    case 'warning':
      return tasks.filter(t => {
        const days = t.delayedDays || 0;
        return days >= 3 && days < 7;
      });
    case 'minor':
      return tasks.filter(t => {
        const days = t.delayedDays || 0;
        return days > 0 && days < 3;
      });
    default:
      return tasks;
  }
});

// 获取项目名称
const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || '未知项目';
};

// 获取负责人名称
const getAssigneeName = (assigneeId?: string) => {
  if (!assigneeId) return '-';
  const user = userStore.userById(assigneeId);
  return user?.name || '未知';
};

// 格式化日期
const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD');
};

// 获取延期文本样式
const getDelayTextClass = (days: number) => {
  if (days >= 7) return 'text-danger-600';
  if (days >= 3) return 'text-warning-600';
  return 'text-info-600';
};

// 获取延期徽章样式
const getDelayBadgeClass = (days: number) => {
  if (days >= 7) return 'bg-danger-100 text-danger-800';
  if (days >= 3) return 'bg-warning-100 text-warning-800';
  return 'bg-info-100 text-info-800';
};

// 获取延期严重程度标签
const getDelaySeverityLabel = (days: number) => {
  if (days >= 7) return '严重延期';
  if (days >= 3) return '中度延期';
  return '轻微延期';
};

// 初始化延期分布图表
const initDelayDistributionChart = () => {
  if (!delayDistributionChartRef.value) return;

  const chart = echarts.init(delayDistributionChartRef.value);

  const delayData = [
    { value: delayStats.value.criticalDelayedTasks, name: '严重延期（≥7天）', itemStyle: { color: '#dc2626' } },
    { value: delayStats.value.warningDelayedTasks, name: '中度延期（3-6天）', itemStyle: { color: '#f97316' } },
    { value: delayStats.value.minorDelayedTasks, name: '轻微延期（<3天）', itemStyle: { color: '#eab308' } }
  ];

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '0%',
      left: 'center'
    },
    series: [
      {
        name: '延期任务',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c} 个'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: delayData
      }
    ]
  };

  chart.setOption(option);
};

// 初始化项目延期排名图表
const initProjectDelayRankingChart = () => {
  if (!projectDelayRankingChartRef.value) return;

  const chart = echarts.init(projectDelayRankingChartRef.value);

  // 按项目统计延期情况
  const projectDelayData = projectStore.projects.map(project => {
    const projectTasks = delayedLeafTasks.value.filter(t => t.projectId === project.id);
    const totalDelayedDays = projectTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
    return {
      name: project.name,
      value: totalDelayedDays,
      count: projectTasks.length
    };
  }).filter(item => item.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 10); // 只显示前10名

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = projectDelayData[param.dataIndex];
        return `${data.name}<br/>累计延期天数: ${data.value}<br/>延期任务数: ${data.count}`;
      }
    },
    xAxis: {
      type: 'value',
      name: '累计延期天数'
    },
    yAxis: {
      type: 'category',
      data: projectDelayData.map(item => item.name),
      inverse: true  // 让排名高的在上面
    },
    series: [
      {
        name: '累计延期天数',
        type: 'bar',
        data: projectDelayData.map(item => item.value),
        itemStyle: {
          color: (params: any) => {
            const colors = ['#dc2626', '#f97316', '#eab308', '#3b82f6', '#6366f1'];
            return colors[params.dataIndex % colors.length];
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c} 天'
        }
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
    initDelayDistributionChart();
    initProjectDelayRankingChart();
  }, 100);
});
</script>
