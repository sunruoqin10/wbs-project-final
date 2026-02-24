<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">项目延期统计</h1>
          <p class="mt-1 text-sm text-secondary-600">查看个人和团队的任务延期情况统计</p>
        </div>
        <div class="flex items-center gap-2">
          <Button variant="secondary" @click="handleRefresh" :loading="loading">
            <svg class="mr-2 h-5 w-5" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            刷新数据
          </Button>
        </div>
      </div>

      <div class="border-b border-secondary-200">
        <nav class="-mb-px flex space-x-8">
          <button
            @click="activeTab = 'personal'"
            :class="[
              activeTab === 'personal'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-secondary-500 hover:border-secondary-300 hover:text-secondary-700',
              'whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium'
            ]"
          >
            <div class="flex items-center gap-2">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              个人负责任务
            </div>
          </button>
          <button
            @click="activeTab = 'team'"
            :class="[
              activeTab === 'team'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-secondary-500 hover:border-secondary-300 hover:text-secondary-700',
              'whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium'
            ]"
          >
            <div class="flex items-center gap-2">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              团队成员任务
            </div>
          </button>
        </nav>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="text-center">
          <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          <p class="mt-2 text-secondary-500">正在加载数据...</p>
        </div>
      </div>

      <div v-else-if="activeTab === 'personal'" class="space-y-6">
        <div v-if="personalTasks.length === 0" class="flex items-center justify-center py-12">
          <div class="text-center">
            <svg class="mx-auto h-16 w-16 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3 class="mt-2 text-lg font-medium text-secondary-900">暂无负责任务</h3>
            <p class="mt-1 text-sm text-secondary-500">您当前没有负责任何任务</p>
          </div>
        </div>

        <div v-else class="space-y-6">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-blue-100 p-3">
                  <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">任务总数</p>
                  <p class="text-2xl font-semibold text-blue-600">{{ personalStats.totalTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-danger-100 p-3">
                  <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">延期任务数</p>
                  <p class="text-2xl font-semibold text-danger-600">{{ personalStats.delayedTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-info-100 p-3">
                  <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">延期率</p>
                  <p class="text-2xl font-semibold text-info-600">{{ Math.round(personalStats.delayRate) }}%</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-warning-100 p-3">
                  <svg class="h-6 w-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">累计延期天数</p>
                  <p class="text-2xl font-semibold text-warning-600">{{ personalStats.totalDelayedDays }}</p>
                </div>
              </div>
            </Card>
          </div>

          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">延期时长分布</h3>
              </template>
              <div class="h-80" ref="personalDelayDistributionChartRef"></div>
            </Card>

            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">项目延期分布</h3>
              </template>
              <div class="h-80" ref="personalProjectDelayChartRef"></div>
            </Card>
          </div>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">延期任务列表</h3>
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
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">状态</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-secondary-200 bg-white">
                  <tr v-if="personalDelayedTasks.length === 0">
                    <td colspan="6" class="px-4 py-8 text-center text-sm text-secondary-500">
                      暂无延期任务
                    </td>
                  </tr>
                  <tr v-for="task in personalDelayedTasks" :key="task.id" class="hover:bg-secondary-50">
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
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <span :class="getStatusBadgeClass(task.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                        {{ getStatusLabel(task.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      </div>

      <div v-else class="space-y-6">
        <div v-if="teamMembers.length === 0" class="flex items-center justify-center py-12">
          <div class="text-center">
            <svg class="mx-auto h-16 w-16 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <h3 class="mt-2 text-lg font-medium text-secondary-900">暂无团队成员</h3>
            <p class="mt-1 text-sm text-secondary-500">您当前没有管辖的团队成员</p>
          </div>
        </div>

        <div v-else class="space-y-6">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-blue-100 p-3">
                  <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">团队任务总数</p>
                  <p class="text-2xl font-semibold text-blue-600">{{ teamStats.totalTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-danger-100 p-3">
                  <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">团队延期任务数</p>
                  <p class="text-2xl font-semibold text-danger-600">{{ teamStats.delayedTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-info-100 p-3">
                  <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">团队延期率</p>
                  <p class="text-2xl font-semibold text-info-600">{{ Math.round(teamStats.delayRate) }}%</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-purple-100 p-3">
                  <svg class="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">团队成员数</p>
                  <p class="text-2xl font-semibold text-purple-600">{{ teamMembers.length }}</p>
                </div>
              </div>
            </Card>
          </div>

          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">成员延期率对比</h3>
              </template>
              <div class="h-80" ref="teamMemberDelayRateChartRef"></div>
            </Card>

            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">成员延期任务数对比</h3>
              </template>
              <div class="h-80" ref="teamMemberDelayCountChartRef"></div>
            </Card>
          </div>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">团队成员延期详情</h3>
            </template>
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-secondary-200">
                <thead class="bg-secondary-50">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">成员</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">任务总数</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">延期任务数</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">延期率</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">累计延期天数</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">操作</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-secondary-200 bg-white">
                  <tr v-for="member in teamMemberStats" :key="member.userId" class="hover:bg-secondary-50">
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <div class="flex items-center">
                        <img
                          :src="getUserAvatar(member.userId)"
                          :alt="getUserName(member.userId)"
                          class="mr-2 h-8 w-8 rounded-full"
                        />
                        {{ getUserName(member.userId) }}
                      </div>
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">{{ member.totalTasks }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold text-danger-600">{{ member.delayedTasks }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <span :class="getDelayRateBadgeClass(member.delayRate)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                        {{ Math.round(member.delayRate) }}%
                      </span>
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-warning-600 font-semibold">{{ member.totalDelayedDays }} 天</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <button
                        @click="showMemberDetail(member.userId)"
                        class="text-primary-600 hover:text-primary-800"
                      >
                        查看详情
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      </div>
    </div>

    <Modal
      :open="showMemberDetailModal"
      @close="showMemberDetailModal = false"
      title="成员延期详情"
    >
      <div v-if="selectedMemberTasks.length > 0" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-lg bg-secondary-50 p-4">
            <p class="text-sm font-medium text-secondary-600">任务总数</p>
            <p class="text-2xl font-semibold text-secondary-900">{{ selectedMemberStats.totalTasks }}</p>
          </div>
          <div class="rounded-lg bg-secondary-50 p-4">
            <p class="text-sm font-medium text-secondary-600">延期任务数</p>
            <p class="text-2xl font-semibold text-danger-600">{{ selectedMemberStats.delayedTasks }}</p>
          </div>
        </div>
        <div class="overflow-x-auto max-h-96">
          <table class="min-w-full divide-y divide-secondary-200">
            <thead class="bg-secondary-50 sticky top-0">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">任务名称</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">延期天数</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">严重程度</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">状态</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-200 bg-white">
              <tr v-for="task in selectedMemberTasks" :key="task.id" class="hover:bg-secondary-50">
                <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-secondary-900">{{ task.title }}</td>
                <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold" :class="getDelayTextClass(task.delayedDays || 0)">
                  {{ task.delayedDays || 0 }} 天
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getDelayBadgeClass(task.delayedDays || 0)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getDelaySeverityLabel(task.delayedDays || 0) }}
                  </span>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getStatusBadgeClass(task.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getStatusLabel(task.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import dayjs from 'dayjs';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Modal from '@/components/common/Modal.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import type { Task } from '@/types';

const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const activeTab = ref<'personal' | 'team'>('personal');
const loading = ref(false);
const showMemberDetailModal = ref(false);
const selectedMemberId = ref<string>('');

const personalDelayDistributionChartRef = ref<HTMLElement>();
const personalProjectDelayChartRef = ref<HTMLElement>();
const teamMemberDelayRateChartRef = ref<HTMLElement>();
const teamMemberDelayCountChartRef = ref<HTMLElement>();

let personalDelayDistributionChartInstance: echarts.ECharts | null = null;
let personalProjectDelayChartInstance: echarts.ECharts | null = null;
let teamMemberDelayRateChartInstance: echarts.ECharts | null = null;
let teamMemberDelayCountChartInstance: echarts.ECharts | null = null;

const accessibleProjects = computed(() => {
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return projectStore.projects;
  }

  const currentUserId = userStore.currentUserId;
  if (!currentUserId) {
    return [];
  }

  return projectStore.projects.filter(project => {
    const isOwner = project.ownerId === currentUserId;
    const isMember = project.memberIds?.includes(currentUserId) || false;
    return isOwner || isMember;
  });
});

const accessibleProjectIds = computed(() => {
  return accessibleProjects.value.map(p => p.id);
});

const accessibleTasks = computed(() => {
  return taskStore.tasks.filter(t => accessibleProjectIds.value.includes(t.projectId));
});

const getTasksForUser = (userId: string): Task[] => {
  const userTasks = accessibleTasks.value.filter(t => t.assigneeId === userId);
  
  const allTaskIds = new Set(userTasks.map(t => t.id));
  const parentTaskIds = new Set(userTasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = userTasks.filter(t => leafTaskIds.has(t.id));

  if (leafTasks.length > 0) {
    return leafTasks;
  }

  const rootTasks = userTasks.filter(t => !t.parentTaskId);
  if (rootTasks.length === 1) {
    return rootTasks;
  }

  return userTasks;
};

const personalTasks = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return getTasksForUser(currentUserId);
});

const personalDelayedTasks = computed(() => {
  return personalTasks.value.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
});

const personalStats = computed(() => {
  const tasks = personalTasks.value;
  const delayedTasks = personalDelayedTasks.value;

  const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
  const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

  return {
    totalTasks: tasks.length,
    delayedTasks: delayedTasks.length,
    delayRate,
    totalDelayedDays
  };
});

const teamMembers = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];

  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return userStore.users.filter(u => u.id !== currentUserId);
  }

  if (permissionStore.currentRole === 'member') {
    const managedProjectIds = accessibleProjects.value
      .filter(p => p.ownerId === currentUserId)
      .map(p => p.id);
    
    const teamMemberIds = new Set<string>();
    accessibleTasks.value
      .filter(t => managedProjectIds.includes(t.projectId) && t.assigneeId)
      .forEach(t => teamMemberIds.add(t.assigneeId!));
    
    return userStore.users.filter(u => teamMemberIds.has(u.id) && u.id !== currentUserId);
  }

  return [];
});

const teamMemberStats = computed(() => {
  return teamMembers.value.map(member => {
    const tasks = getTasksForUser(member.id);
    const delayedTasks = tasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
    const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
    const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

    return {
      userId: member.id,
      totalTasks: tasks.length,
      delayedTasks: delayedTasks.length,
      delayRate,
      totalDelayedDays
    };
  });
});

const teamStats = computed(() => {
  let totalTasks = 0;
  let delayedTasks = 0;
  let totalDelayedDays = 0;

  teamMemberStats.value.forEach(stats => {
    totalTasks += stats.totalTasks;
    delayedTasks += stats.delayedTasks;
    totalDelayedDays += stats.totalDelayedDays;
  });

  const delayRate = totalTasks > 0 ? (delayedTasks / totalTasks) * 100 : 0;

  return {
    totalTasks,
    delayedTasks,
    delayRate,
    totalDelayedDays
  };
});

const selectedMemberTasks = computed(() => {
  if (!selectedMemberId.value) return [];
  const tasks = getTasksForUser(selectedMemberId.value);
  return tasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
});

const selectedMemberStats = computed(() => {
  if (!selectedMemberId.value) return { totalTasks: 0, delayedTasks: 0, delayRate: 0, totalDelayedDays: 0 };
  const tasks = getTasksForUser(selectedMemberId.value);
  const delayedTasks = tasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
  const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
  const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

  return {
    totalTasks: tasks.length,
    delayedTasks: delayedTasks.length,
    delayRate,
    totalDelayedDays
  };
});

const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || '未知项目';
};

const getUserName = (userId?: string) => {
  if (!userId) return '-';
  const user = userStore.userById(userId);
  return user?.name || '未知';
};

const getUserAvatar = (userId?: string) => {
  if (!userId) return 'https://via.placeholder.com/32';
  const user = userStore.userById(userId);
  return user?.avatar || 'https://via.placeholder.com/32';
};

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD');
};

const getDelayTextClass = (days: number) => {
  if (days >= 7) return 'text-danger-600';
  if (days >= 3) return 'text-warning-600';
  return 'text-info-600';
};

const getDelayBadgeClass = (days: number) => {
  if (days >= 7) return 'bg-danger-100 text-danger-800';
  if (days >= 3) return 'bg-warning-100 text-warning-800';
  return 'bg-info-100 text-info-800';
};

const getDelaySeverityLabel = (days: number) => {
  if (days >= 7) return '严重延期';
  if (days >= 3) return '中度延期';
  return '轻微延期';
};

const getDelayRateBadgeClass = (rate: number) => {
  if (rate >= 50) return 'bg-danger-100 text-danger-800';
  if (rate >= 20) return 'bg-warning-100 text-warning-800';
  if (rate > 0) return 'bg-info-100 text-info-800';
  return 'bg-green-100 text-green-800';
};

const getStatusBadgeClass = (status: string) => {
  switch (status) {
    case 'todo':
      return 'bg-secondary-100 text-secondary-800';
    case 'in-progress':
      return 'bg-blue-100 text-blue-800';
    case 'done':
      return 'bg-green-100 text-green-800';
    default:
      return 'bg-secondary-100 text-secondary-800';
  }
};

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'todo':
      return '待办';
    case 'in-progress':
      return '进行中';
    case 'done':
      return '已完成';
    default:
      return status;
  }
};

const initPersonalDelayDistributionChart = () => {
  if (!personalDelayDistributionChartRef.value) return;

  const chart = echarts.init(personalDelayDistributionChartRef.value);

  const critical = personalDelayedTasks.value.filter(t => (t.delayedDays || 0) >= 7).length;
  const warning = personalDelayedTasks.value.filter(t => {
    const days = t.delayedDays || 0;
    return days >= 3 && days < 7;
  }).length;
  const minor = personalDelayedTasks.value.filter(t => {
    const days = t.delayedDays || 0;
    return days > 0 && days < 3;
  }).length;

  const delayData = [
    { value: critical, name: '严重延期（≥7天）', itemStyle: { color: '#dc2626' } },
    { value: warning, name: '中度延期（3-6天）', itemStyle: { color: '#f97316' } },
    { value: minor, name: '轻微延期（<3天）', itemStyle: { color: '#eab308' } }
  ].filter(item => item.value > 0);

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
        data: delayData.length > 0 ? delayData : [{ value: 1, name: '暂无数据', itemStyle: { color: '#d1d5db' } }]
      }
    ]
  };

  chart.setOption(option);
  personalDelayDistributionChartInstance = chart;
};

const initPersonalProjectDelayChart = () => {
  if (!personalProjectDelayChartRef.value) return;

  const chart = echarts.init(personalProjectDelayChartRef.value);

  const projectDelayMap = new Map<string, { name: string; count: number; days: number }>();

  personalDelayedTasks.value.forEach(task => {
    const projectName = getProjectName(task.projectId);
    const existing = projectDelayMap.get(task.projectId);
    if (existing) {
      existing.count += 1;
      existing.days += task.delayedDays || 0;
    } else {
      projectDelayMap.set(task.projectId, {
        name: projectName,
        count: 1,
        days: task.delayedDays || 0
      });
    }
  });

  const projectDelayData = Array.from(projectDelayMap.values())
    .sort((a, b) => b.days - a.days)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = projectDelayData[param.dataIndex];
        return `${data.name}<br/>延期任务数: ${data.count}<br/>累计延期天数: ${data.days}`;
      }
    },
    grid: {
      left: '20%',
      right: '15%',
      top: '10%',
      bottom: '10%'
    },
    xAxis: {
      type: 'value',
      name: '累计延期天数',
      nameLocation: 'middle',
      nameGap: 30
    },
    yAxis: {
      type: 'category',
      data: projectDelayData.map(item => item.name),
      inverse: true,
      axisLabel: {
        width: 100,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        name: '累计延期天数',
        type: 'bar',
        data: projectDelayData.map(item => item.days),
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

  if (projectDelayData.length === 0) {
    option.series[0].data = [0];
    option.yAxis.data = ['暂无数据'];
  }

  chart.setOption(option);
  personalProjectDelayChartInstance = chart;
};

const initTeamMemberDelayRateChart = () => {
  if (!teamMemberDelayRateChartRef.value) return;

  const chart = echarts.init(teamMemberDelayRateChartRef.value);

  const memberData = teamMemberStats.value
    .filter(m => m.totalTasks > 0)
    .sort((a, b) => b.delayRate - a.delayRate)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = memberData[param.dataIndex];
        return `${getUserName(data.userId)}<br/>延期率: ${Math.round(data.delayRate)}%<br/>任务总数: ${data.totalTasks}<br/>延期任务: ${data.delayedTasks}`;
      }
    },
    grid: {
      left: '20%',
      right: '15%',
      top: '10%',
      bottom: '10%'
    },
    xAxis: {
      type: 'value',
      name: '延期率 (%)',
      nameLocation: 'middle',
      nameGap: 30,
      max: 100
    },
    yAxis: {
      type: 'category',
      data: memberData.map(m => getUserName(m.userId)),
      inverse: true,
      axisLabel: {
        width: 80,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        name: '延期率',
        type: 'bar',
        data: memberData.map(m => m.delayRate),
        itemStyle: {
          color: (params: any) => {
            const value = params.value;
            if (value >= 50) return '#dc2626';
            if (value >= 20) return '#f97316';
            if (value > 0) return '#eab308';
            return '#22c55e';
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => `${Math.round(params.value)}%`
        }
      }
    ]
  };

  if (memberData.length === 0) {
    option.series[0].data = [0];
    option.yAxis.data = ['暂无数据'];
  }

  chart.setOption(option);
  teamMemberDelayRateChartInstance = chart;
};

const initTeamMemberDelayCountChart = () => {
  if (!teamMemberDelayCountChartRef.value) return;

  const chart = echarts.init(teamMemberDelayCountChartRef.value);

  const memberData = teamMemberStats.value
    .filter(m => m.totalTasks > 0)
    .sort((a, b) => b.delayedTasks - a.delayedTasks)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['任务总数', '延期任务数'],
      bottom: '0%'
    },
    grid: {
      left: '15%',
      right: '10%',
      top: '10%',
      bottom: '15%'
    },
    xAxis: {
      type: 'category',
      data: memberData.map(m => getUserName(m.userId)),
      axisLabel: {
        width: 60,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '任务总数',
        type: 'bar',
        data: memberData.map(m => m.totalTasks),
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '延期任务数',
        type: 'bar',
        data: memberData.map(m => m.delayedTasks),
        itemStyle: { color: '#dc2626' }
      }
    ]
  };

  if (memberData.length === 0) {
    option.series[0].data = [0];
    option.series[1].data = [0];
    option.xAxis.data = ['暂无数据'];
  }

  chart.setOption(option);
  teamMemberDelayCountChartInstance = chart;
};

const handleRefresh = async () => {
  loading.value = true;
  try {
    await Promise.all([
      projectStore.loadProjects(),
      taskStore.loadTasks(),
      userStore.loadUsers()
    ]);
    initCharts();
  } finally {
    loading.value = false;
  }
};

const showMemberDetail = (userId: string) => {
  selectedMemberId.value = userId;
  showMemberDetailModal.value = true;
};

const initCharts = () => {
  setTimeout(() => {
    if (activeTab.value === 'personal') {
      initPersonalDelayDistributionChart();
      initPersonalProjectDelayChart();
    } else {
      initTeamMemberDelayRateChart();
      initTeamMemberDelayCountChart();
    }
  }, 100);
};

watch(activeTab, () => {
  initCharts();
});

onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([
      projectStore.loadProjects(),
      taskStore.loadTasks(),
      userStore.loadUsers()
    ]);
    initCharts();
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  personalDelayDistributionChartInstance?.dispose();
  personalProjectDelayChartInstance?.dispose();
  teamMemberDelayRateChartInstance?.dispose();
  teamMemberDelayCountChartInstance?.dispose();
});
</script>
