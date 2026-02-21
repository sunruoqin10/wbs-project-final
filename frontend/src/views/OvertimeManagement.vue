<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">项目加班管理</h1>
          <p class="mt-1 text-sm text-secondary-600">记录和管理项目成员的加班情况</p>
        </div>
        <Button variant="primary" @click="handleAddOvertime">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新增加班
        </Button>
      </div>

      <!-- Statistics Cards -->
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
        <!-- 本月加班时长 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-blue-100 p-3">
              <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">本月加班时长</p>
              <p class="text-2xl font-semibold text-blue-600">{{ stats.thisMonthHours }} 小时</p>
            </div>
          </div>
        </Card>

        <!-- 加班人数 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-green-100 p-3">
              <svg class="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">加班人数</p>
              <p class="text-2xl font-semibold text-green-600">{{ stats.thisMonthPeople }} 人</p>
            </div>
          </div>
        </Card>

        <!-- 待审批数量 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-orange-100 p-3">
              <svg class="h-6 w-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">待审批数量</p>
              <p class="text-2xl font-semibold text-orange-600">{{ stats.pendingApprovals }}</p>
            </div>
          </div>
        </Card>

        <!-- 调休累计 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-purple-100 p-3">
              <svg class="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">调休累计</p>
              <p class="text-2xl font-semibold text-purple-600">{{ totalCompTimeoff }} 小时</p>
            </div>
          </div>
        </Card>

        <!-- 加班费累计 -->
        <Card>
          <div class="flex items-center">
            <div class="rounded-lg bg-red-100 p-3">
              <svg class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">加班费累计</p>
              <p class="text-2xl font-semibold text-red-600">{{ totalPayHours }} 小时</p>
            </div>
          </div>
        </Card>
      </div>

      <!-- Charts -->
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- 加班趋势图 -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">加班趋势（最近30天）</h3>
          </template>
          <div class="h-80" ref="trendChartRef"></div>
        </Card>

        <!-- 项目加班分布 -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">项目加班分布</h3>
          </template>
          <div class="h-80" ref="distributionChartRef"></div>
        </Card>
      </div>

      <!-- Overtime Records Table -->
      <Card>
        <template #header>
          <div class="flex flex-wrap items-center gap-4">
            <div class="flex items-center gap-2">
              <label class="text-sm text-secondary-600">项目：</label>
              <select v-model="filters.projectId" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                <option value="">全部项目</option>
                <option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
                  {{ project.name }}
                </option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <label class="text-sm text-secondary-600">人员：</label>
              <select v-model="filters.userId" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                <option value="">全部人员</option>
                <option v-for="user in userStore.users" :key="user.id" :value="user.id">
                  {{ user.name }}
                </option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <label class="text-sm text-secondary-600">状态：</label>
              <select v-model="filters.status" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                <option value="">全部状态</option>
                <option value="pending">待审批</option>
                <option value="approved">已通过</option>
                <option value="rejected">已拒绝</option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <label class="text-sm text-secondary-600">类型：</label>
              <select v-model="filters.overtimeType" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                <option value="">全部类型</option>
                <option value="weekday">工作日加班</option>
                <option value="weekend">周末加班</option>
                <option value="holiday">节假日加班</option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <label class="text-sm text-secondary-600">日期范围：</label>
              <input
                v-model="filters.startDate"
                type="date"
                class="rounded border border-secondary-300 px-3 py-1 text-sm"
              />
              <span class="text-secondary-500">至</span>
              <input
                v-model="filters.endDate"
                type="date"
                class="rounded border border-secondary-300 px-3 py-1 text-sm"
              />
            </div>
          </div>
        </template>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-secondary-200">
            <thead class="bg-secondary-50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">人员</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">项目</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">项目负责人</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">关联任务</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">日期</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">时间段</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">时长</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">类型</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">补偿方式</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-200 bg-white">
              <tr v-if="filteredRecords.length === 0">
                <td colspan="11" class="px-4 py-8 text-center text-sm text-secondary-500">
                  暂无加班记录
                </td>
              </tr>
              <tr v-for="record in filteredRecords" :key="record.id" class="hover:bg-secondary-50">
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">
                  <div class="flex items-center">
                    <img
                      :src="getUserAvatar(record.userId)"
                      :alt="getUserName(record.userId)"
                      class="mr-2 h-8 w-8 rounded-full"
                    />
                    {{ getUserName(record.userId) }}
                  </div>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
                  {{ getProjectName(record.projectId) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
                  {{ getProjectOwner(record.projectId) }}
                </td>
                <td class="px-4 py-3 text-sm text-secondary-600">
                  {{ getTaskName(record.taskId) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
                  {{ formatDate(record.overtimeDate) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
                  {{ record.startTime }} - {{ record.endTime }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold text-secondary-900">
                  {{ record.hours }} 小时
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getTypeBadgeClass(record.overtimeType)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getTypeLabel(record.overtimeType) }}
                  </span>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">
                  {{ getCompensationLabel(record.compensationType) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getStatusBadgeClass(record.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getStatusLabel(record.status) }}
                  </span>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <div class="flex items-center gap-2">
                    <button
                      v-if="record.status === 'pending' && canApprove(record.projectId)"
                      @click="handleApprove(record)"
                      class="text-primary-600 hover:text-primary-800"
                      title="审批"
                    >
                      审批
                    </button>
                    <button
                      @click="handleEdit(record)"
                      class="text-secondary-600 hover:text-secondary-800"
                      title="编辑"
                    >
                      编辑
                    </button>
                    <button
                      @click="handleDelete(record)"
                      class="text-danger-600 hover:text-danger-800"
                      title="删除"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </div>

    <!-- Overtime Modal -->
    <OvertimeModal
      :open="showOvertimeModal"
      :record="currentRecord"
      @close="handleOvertimeModalClose"
      @save="handleOvertimeSave"
    />

    <!-- Approval Modal -->
    <ApprovalModal
      :open="showApprovalModal"
      :record="currentRecord"
      @close="handleApprovalModalClose"
      @approve="handleApprovalSubmit"
      @reject="handleRejectSubmit"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import dayjs from 'dayjs';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import OvertimeModal from '@/components/overtime/OvertimeModal.vue';
import ApprovalModal from '@/components/overtime/ApprovalModal.vue';
import { useOvertimeStore } from '@/stores/overtime';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import type { OvertimeRecord } from '@/types';

const overtimeStore = useOvertimeStore();
const projectStore = useProjectStore();
const userStore = useUserStore();
const taskStore = useTaskStore();

// Chart refs
const trendChartRef = ref<HTMLElement>();
const distributionChartRef = ref<HTMLElement>();
// Chart instances
let trendChartInstance: echarts.ECharts | null = null;
let distributionChartInstance: echarts.ECharts | null = null;

// Modal state
const showOvertimeModal = ref(false);
const showApprovalModal = ref(false);
const currentRecord = ref<OvertimeRecord | null>(null);

// Filters
const filters = ref({
  projectId: '',
  userId: '',
  status: '',
  overtimeType: '',
  startDate: '',
  endDate: ''
});

// Statistics
const stats = computed(() => overtimeStore.stats || {
  totalRecords: 0,
  totalHours: 0,
  totalPeople: 0,
  pendingApprovals: 0,
  thisMonthHours: 0,
  thisMonthPeople: 0,
  byType: { weekday: 0, weekend: 0, holiday: 0 },
  byProject: []
});

// 计算调休累计（补偿方式为调休的已审批加班时长）
const totalCompTimeoff = computed(() => {
  return overtimeStore.approvedRecords
    .filter(r => r.compensationType === 'timeoff')
    .reduce((sum, r) => sum + r.hours, 0);
});

// 计算加班费累计（补偿方式为加班费的已审批加班时长）
const totalPayHours = computed(() => {
  return overtimeStore.approvedRecords
    .filter(r => r.compensationType === 'pay')
    .reduce((sum, r) => sum + r.hours, 0);
});

// Filtered records
const filteredRecords = computed(() => {
  let result = overtimeStore.overtimeRecords;

  if (filters.value.projectId) {
    result = result.filter(r => r.projectId === filters.value.projectId);
  }

  if (filters.value.userId) {
    result = result.filter(r => r.userId === filters.value.userId);
  }

  if (filters.value.status) {
    result = result.filter(r => r.status === filters.value.status);
  }

  if (filters.value.overtimeType) {
    result = result.filter(r => r.overtimeType === filters.value.overtimeType);
  }

  if (filters.value.startDate) {
    result = result.filter(r => r.overtimeDate >= filters.value.startDate);
  }

  if (filters.value.endDate) {
    result = result.filter(r => r.overtimeDate <= filters.value.endDate);
  }

  return result;
});

// Helper functions
const getUserName = (userId: string) => {
  const user = userStore.userById(userId);
  return user?.name || '未知';
};

const getUserAvatar = (userId: string) => {
  const user = userStore.userById(userId);
  return user?.avatar || 'https://ui-avatars.com/api/?name=User&background=6366f1&color=fff';
};

const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || '未知项目';
};

const getProjectOwner = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  if (!project?.ownerId) return '未设置';
  const owner = userStore.userById(project.ownerId);
  return owner?.name || '未知';
};

const getTaskName = (taskId?: string) => {
  if (!taskId) return '-';
  const task = taskStore.getTaskById(taskId);
  return task?.title || '未知任务';
};

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD');
};

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    weekday: '工作日',
    weekend: '周末',
    holiday: '节假日'
  };
  return labels[type] || type;
};

const getTypeBadgeClass = (type: string) => {
  const classes: Record<string, string> = {
    weekday: 'bg-blue-100 text-blue-800',
    weekend: 'bg-green-100 text-green-800',
    holiday: 'bg-purple-100 text-purple-800'
  };
  return classes[type] || 'bg-secondary-100 text-secondary-800';
};

const getCompensationLabel = (type?: string) => {
  if (!type) return '-';
  const labels: Record<string, string> = {
    pay: '加班费',
    timeoff: '调休'
  };
  return labels[type] || type;
};

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已拒绝'
  };
  return labels[status] || status;
};

const getStatusBadgeClass = (status: string) => {
  const classes: Record<string, string> = {
    pending: 'bg-orange-100 text-orange-800',
    approved: 'bg-green-100 text-green-800',
    rejected: 'bg-red-100 text-red-800'
  };
  return classes[status] || 'bg-secondary-100 text-secondary-800';
};

// 判断当前用户是否可以审批（项目负责人）
const canApprove = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  if (!project) return false;
  // 检查当前用户是否是项目负责人
  return project.ownerId === userStore.currentUserId;
};

// Initialize charts
const initTrendChart = () => {
  if (!trendChartRef.value) return;

  console.log('=== 初始化加班趋势图 ===');
  console.log('加班记录数量:', overtimeStore.overtimeRecords.length);

  // 销毁旧实例
  if (trendChartInstance) {
    trendChartInstance.dispose();
  }

  // 创建新实例
  trendChartInstance = echarts.init(trendChartRef.value);

  // 生成最近30天的日期
  const dates: string[] = [];
  const hours: number[] = [];
  for (let i = 29; i >= 0; i--) {
    const date = dayjs().subtract(i, 'day');
    dates.push(date.format('MM-DD'));

    // 计算该日期的加班时长（包含所有状态）
    const dateStr = date.format('YYYY-MM-DD');
    const dayHours = overtimeStore.overtimeRecords
      .filter(r => r.overtimeDate === dateStr)
      .reduce((sum, r) => sum + r.hours, 0);
    hours.push(dayHours);
  }

  console.log('趋势图数据 - 日期:', dates);
  console.log('趋势图数据 - 时长:', hours);

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
      data: dates,
      axisLabel: {
        interval: 4,
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '小时',
      minInterval: 1
    },
    series: [
      {
        name: '加班时长',
        type: 'line',
        data: hours,
        smooth: true,
        lineStyle: {
          color: '#3b82f6',
          width: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        },
        itemStyle: {
          color: '#3b82f6'
        }
      }
    ]
  };

  trendChartInstance.setOption(option);
};

const initDistributionChart = () => {
  if (!distributionChartRef.value) return;

  console.log('=== 初始化项目加班分布图 ===');
  console.log('统计数据:', stats.value);
  console.log('项目分布数据:', stats.value.byProject);

  // 销毁旧实例
  if (distributionChartInstance) {
    distributionChartInstance.dispose();
  }

  // 创建新实例
  distributionChartInstance = echarts.init(distributionChartRef.value);

  // 处理项目数据，确保字段名称正确
  const projectData = (stats.value.byProject || [])
    .map((item: any) => {
      // 处理 BigDecimal 类型的 totalHours，可能是字符串或数字
      const hours = Number(item.totalHours || item.hours || 0);
      return {
        projectId: item.projectId,
        projectName: item.projectName,
        hours: hours,
        count: Number(item.recordCount || item.count || 0)
      };
    })
    .filter((item: any) => item.hours > 0) // 只显示有加班时长的项目
    .sort((a: any, b: any) => b.hours - a.hours)
    .slice(0, 10);

  console.log('处理后的项目数据:', projectData);

  if (projectData.length === 0) {
    distributionChartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    });
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
        const data = projectData[param.dataIndex];
        return `${data.projectName}<br/>加班时长: ${data.hours} 小时<br/>加班次数: ${data.count} 次`;
      }
    },
    grid: {
      left: '20%',
      right: '10%',
      top: '10%',
      bottom: '10%'
    },
    xAxis: {
      type: 'value',
      name: '小时',
      nameLocation: 'middle',
      nameGap: 30
    },
    yAxis: {
      type: 'category',
      data: projectData.map(item => item.projectName),
      inverse: true,
      axisLabel: {
        width: 100,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        name: '加班时长',
        type: 'bar',
        data: projectData.map(item => item.hours),
        itemStyle: {
          color: (params: any) => {
            const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1'];
            return colors[params.dataIndex % colors.length];
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c} 小时'
        }
      }
    ]
  };

  distributionChartInstance.setOption(option);
};

// Event handlers
const handleAddOvertime = () => {
  currentRecord.value = null;
  showOvertimeModal.value = true;
};

const handleEdit = (record: OvertimeRecord) => {
  // 已审批通过的记录不能编辑
  if (record.status === 'approved') {
    alert('已审批通过的加班记录不能编辑');
    return;
  }
  currentRecord.value = record;
  showOvertimeModal.value = true;
};

const handleDelete = async (record: OvertimeRecord) => {
  // 已审批通过的记录不能删除
  if (record.status === 'approved') {
    alert('已审批通过的加班记录不能删除');
    return;
  }
  
  if (!confirm('确定要删除这条加班记录吗？')) return;

  try {
    await overtimeStore.deleteOvertimeRecord(record.id);
    // 重新加载统计数据和记录
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    // 手动刷新图表
    setTimeout(() => {
      initTrendChart();
      initDistributionChart();
    }, 100);
  } catch (error) {
    console.error('Failed to delete overtime record:', error);
    alert('删除失败，请重试');
  }
};

const handleApprove = (record: OvertimeRecord) => {
  currentRecord.value = record;
  showApprovalModal.value = true;
};

const handleOvertimeModalClose = () => {
  showOvertimeModal.value = false;
  currentRecord.value = null;
};

const handleOvertimeSave = async (data: Partial<OvertimeRecord>) => {
  try {
    if (currentRecord.value) {
      await overtimeStore.updateOvertimeRecord(currentRecord.value.id, data);
    } else {
      await overtimeStore.createOvertimeRecord(data);
    }
    showOvertimeModal.value = false;
    currentRecord.value = null;
    // 重新加载统计数据和记录
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    // 手动刷新图表
    setTimeout(() => {
      initTrendChart();
      initDistributionChart();
    }, 100);
  } catch (error) {
    console.error('Failed to save overtime record:', error);
    alert('保存失败，请重试');
  }
};

const handleApprovalModalClose = () => {
  showApprovalModal.value = false;
  currentRecord.value = null;
};

const handleApprovalSubmit = async (recordId: string) => {
  try {
    const approverId = userStore.currentUserId;
    if (!approverId) {
      alert('无法获取当前用户信息');
      return;
    }
    await overtimeStore.approveOvertimeRecord(recordId, approverId);
    showApprovalModal.value = false;
    currentRecord.value = null;
    // 重新加载统计数据和记录
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    // 手动刷新图表
    setTimeout(() => {
      initTrendChart();
      initDistributionChart();
    }, 100);
  } catch (error) {
    console.error('Failed to approve overtime record:', error);
    alert('审批失败，请重试');
  }
};

const handleRejectSubmit = async (recordId: string, rejectReason: string) => {
  try {
    const approverId = userStore.currentUserId;
    if (!approverId) {
      alert('无法获取当前用户信息');
      return;
    }
    await overtimeStore.rejectOvertimeRecord(recordId, approverId, rejectReason);
    showApprovalModal.value = false;
    currentRecord.value = null;
    // 重新加载统计数据和记录
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    // 手动刷新图表
    setTimeout(() => {
      initTrendChart();
      initDistributionChart();
    }, 100);
  } catch (error) {
    console.error('Failed to reject overtime record:', error);
    alert('拒绝失败，请重试');
  }
};

// Load data on mount
onMounted(async () => {
  console.log('=== 开始加载数据 ===');
  await Promise.all([
    projectStore.loadProjects(),
    userStore.loadUsers(),
    taskStore.loadTasks(),
    overtimeStore.loadOvertimeRecords(),
    overtimeStore.loadStats()
  ]);

  console.log('=== 数据加载完成 ===');
  console.log('加班记录:', overtimeStore.overtimeRecords);
  console.log('统计数据:', overtimeStore.stats);

  // Initialize charts after data is loaded
  setTimeout(() => {
    console.log('=== 初始化图表 ===');
    initTrendChart();
    initDistributionChart();
  }, 100);
});

// Watch for data changes to update charts
watch(() => overtimeStore.overtimeRecords, () => {
  console.log('=== overtimeRecords 变化，刷新图表 ===');
  setTimeout(() => {
    initTrendChart();
    initDistributionChart();
  }, 100);
}, { deep: true });

// Watch for stats changes to update distribution chart
watch(() => overtimeStore.stats, () => {
  console.log('=== stats 变化，刷新分布图 ===');
  setTimeout(() => {
    initDistributionChart();
  }, 100);
}, { deep: true });

// Cleanup on unmount
onUnmounted(() => {
  if (trendChartInstance) {
    trendChartInstance.dispose();
    trendChartInstance = null;
  }
  if (distributionChartInstance) {
    distributionChartInstance.dispose();
    distributionChartInstance = null;
  }
});
</script>
