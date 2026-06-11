<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('team.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('team.subtitle') }}</p>
        </div>
        <!-- 成员由 HR 同步管理,无手动添加入口 -->
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('common.members') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ users.length }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.admin') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ adminCount }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.projectManager') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ pmCount }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.totalMembers') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ memberCount }}</p>
          </div>
        </Card>
      </div>

      <!-- Workload Distribution -->
      <Card>
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.workloadDistribution') }}</h3>
        </template>
        <div class="h-80" ref="workloadChartRef"></div>
      </Card>

      <!-- Tabs -->
      <Tabs v-model="activeTab" :tabs="tabs">
        <template #default="{ activeTab: currentTab }">
          <div v-if="currentTab === 0" class="space-y-6">
            <!-- 顶部全局筛选 -->
            <Card>
              <template #header>
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <h3 class="text-lg font-semibold text-secondary-900">按组织架构查看</h3>
                  <div class="flex items-center gap-3">
                    <input
                      v-model="memberSearch"
                      type="text"
                      placeholder="搜索姓名/工号/邮箱"
                      class="w-48 rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    />
                    <select
                      v-model="memberRoleFilter"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="">{{ $t('team.allMembers.filterRole') }}</option>
                      <option value="admin">{{ $t('team.allMembers.roleAdmin') }}</option>
                      <option value="dept-project-manager">{{ $t('team.allMembers.roleDeptProjectManager') }}</option>
                      <option value="project-manager">{{ $t('team.allMembers.roleProjectManager') }}</option>
                      <option value="member">{{ $t('team.allMembers.roleMember') }}</option>
                      <option value="viewer">{{ $t('team.allMembers.roleViewer') }}</option>
                    </select>
                    <select
                      v-model="companyFilter"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="">所有公司</option>
                      <option v-for="cd in availableCompanyCodes" :key="cd" :value="cd">
                        {{ cd }} {{ companyName(cd) }}
                      </option>
                    </select>
                  </div>
                </div>
              </template>

              <div v-if="!orgStore.tree" class="py-8 text-center text-sm text-secondary-500">
                正在加载组织架构…
              </div>

              <div v-else-if="filteredUsers.length === 0" class="py-8 text-center text-sm text-secondary-500">
                没有匹配的用户
              </div>

              <!-- 搜索模式：扁平结果 -->
              <div v-else-if="isSearchMode" class="space-y-4">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-medium text-secondary-700">
                    搜索结果 ({{ filteredUsers.length }} 人)
                  </h4>
                  <button
                    type="button"
                    @click="memberSearch = ''"
                    class="text-xs text-primary-600 hover:text-primary-700"
                  >
                    清除搜索
                  </button>
                </div>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                  <div
                    v-for="user in filteredUsers"
                    :key="user.id"
                    class="flex items-start gap-3 rounded-lg border border-secondary-200 bg-white p-3 transition-shadow hover:shadow-sm"
                  >
                    <UserAvatar :name="displayName(user)" :seed="user.avatar" size="md" />
                    <div class="min-w-0 flex-1">
                      <div class="truncate text-sm font-medium text-secondary-900">
                        {{ displayName(user) }}
                      </div>
                      <div v-if="user.chineseNam && user.chineseNam !== user.name" class="truncate text-xs text-secondary-500">
                        {{ user.name }}
                      </div>
                      <div class="mt-0.5 truncate font-mono text-xs text-secondary-500">
                        {{ user.id }}
                      </div>
                      <div class="mt-0.5 truncate text-xs text-secondary-500">
                        {{ user.email }}
                      </div>
                      <div class="mt-0.5 truncate text-xs text-secondary-500">
                        {{ user.companyCd }} · {{ user.department }}
                      </div>
                      <div class="mt-1 flex items-center justify-between gap-2">
                        <Badge :variant="roleBadgeVariant(user.role)">
                          {{ roleLabel(user.role) }}
                        </Badge>
                        <!-- 修改角色按钮:永远显示,仅 admin 可点;非 admin 显示灰色 + tooltip 提示 -->
                        <button
                          type="button"
                          :class="[
                            'rounded px-2 py-0.5 text-xs',
                            permissionStore.isAdmin()
                              ? 'text-primary-600 hover:bg-primary-50 hover:text-primary-700'
                              : 'cursor-not-allowed text-secondary-400'
                          ]"
                          :disabled="!permissionStore.isAdmin()"
                          :title="permissionStore.isAdmin()
                            ? $t('team.roleChange.title')
                            : $t('team.roleChange.adminOnly')"
                          @click="permissionStore.isAdmin() && openRoleChangeDialog(user)"
                        >
                          {{ $t('team.roleChange.title') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 浏览模式：按公司 + org 树分组 -->
              <div v-else class="space-y-3">
                <div
                  v-for="company in displayedCompanyNodes"
                  :key="company.code"
                  class="rounded-lg border-2 border-primary-200 bg-primary-50/30 p-3"
                >
                  <div class="mb-2 flex items-center gap-2 px-1">
                    <span class="inline-flex h-6 w-6 items-center justify-center rounded bg-primary-600 text-xs font-bold text-white">
                      {{ company.code }}
                    </span>
                    <span class="text-sm font-semibold text-secondary-900">{{ company.name }}</span>
                    <span class="text-xs text-secondary-500">({{ countCompanyMembers(company) }} 人)</span>
                  </div>
                  <div v-if="company.children.length === 0" class="px-1 py-2 text-xs text-secondary-400">
                    本公司在组织架构中暂无数据
                  </div>
                  <div v-else class="space-y-2">
                    <OrgGroup
                      v-for="top in company.children"
                      :key="top.code"
                      :node="top"
                      :members="filteredUsers"
                      @open-role-change="openRoleChangeDialog"
                    />
                  </div>
                </div>

                <!-- 未分配部门用户 -->
                <div
                  v-if="unassignedUsers.length > 0"
                  class="rounded-lg border-2 border-dashed border-secondary-300 bg-secondary-50"
                >
                  <button
                    type="button"
                    @click="unassignedExpanded = !unassignedExpanded"
                    class="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-secondary-100"
                    :aria-expanded="unassignedExpanded"
                  >
                    <div class="flex items-center gap-2">
                      <span
                        class="inline-flex h-5 w-5 items-center justify-center text-secondary-400 transition-transform"
                        :class="{ 'rotate-90': unassignedExpanded }"
                      >
                        <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4">
                          <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L10.94 10 7.23 6.29a.75.75 0 111.04-1.08l4.25 4.25a.75.75 0 010 1.08l-4.25 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd" />
                        </svg>
                      </span>
                      <span class="inline-flex h-6 w-6 items-center justify-center rounded bg-secondary-400 text-xs font-bold text-white">
                        ?
                      </span>
                      <span class="text-sm font-semibold text-secondary-900">未分配部门</span>
                      <span class="text-xs text-secondary-500">({{ unassignedUsers.length }} 人 · deptCode 为空)</span>
                    </div>
                  </button>

                  <div v-if="unassignedExpanded" class="border-t border-secondary-200 p-3">
                    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                      <div
                        v-for="user in visibleUnassignedUsers"
                        :key="user.id"
                        class="flex items-start gap-3 rounded-lg border border-secondary-200 bg-white p-3 transition-shadow hover:shadow-sm"
                      >
                        <UserAvatar :name="displayName(user)" :seed="user.avatar" size="md" />
                        <div class="min-w-0 flex-1">
                          <div class="truncate text-sm font-medium text-secondary-900">{{ displayName(user) }}</div>
                          <div v-if="user.chineseNam && user.chineseNam !== user.name" class="truncate text-xs text-secondary-500">
                            {{ user.name }}
                          </div>
                          <div class="mt-0.5 truncate font-mono text-xs text-secondary-500">{{ user.id }}</div>
                          <div v-if="user.email" class="mt-0.5 truncate text-xs text-secondary-500">{{ user.email }}</div>
                          <div class="mt-0.5 truncate text-xs text-secondary-500">
                            {{ user.companyCd || '-' }} · {{ user.department || '未分配部门' }}
                          </div>
                          <div class="mt-1 flex items-center justify-between gap-2">
                            <Badge :variant="roleBadgeVariant(user.role)">
                              {{ roleLabel(user.role) }}
                            </Badge>
                            <button
                              type="button"
                              :class="[
                                'rounded px-2 py-0.5 text-xs',
                                permissionStore.isAdmin()
                                  ? 'text-primary-600 hover:bg-primary-50 hover:text-primary-700'
                                  : 'cursor-not-allowed text-secondary-400'
                              ]"
                              :disabled="!permissionStore.isAdmin()"
                              :title="permissionStore.isAdmin()
                                ? $t('team.roleChange.title')
                                : $t('team.roleChange.adminOnly')"
                              @click="permissionStore.isAdmin() && openRoleChangeDialog(user)"
                            >
                              {{ $t('team.roleChange.title') }}
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div v-if="unassignedUsers.length > unassignedPageSize" class="mt-3 text-center">
                      <button
                        type="button"
                        @click="unassignedVisibleCount += unassignedPageSize"
                        class="text-xs font-medium text-primary-600 hover:text-primary-700"
                      >
                        加载更多 ({{ unassignedUsers.length - unassignedVisibleCount }} 人未显示)
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <div v-else-if="currentTab === 1" class="space-y-6">
            <!-- Task Assignment Table -->
            <Card>
              <template #header>
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.taskAssignment.title') }}</h3>
                  <div class="flex items-center gap-2">
                    <select 
                      v-model="sortBy" 
                      @change="handleSortChange"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="memberName">{{ $t('team.taskAssignment.memberName') }}</option>
                      <option value="taskName">{{ $t('team.taskAssignment.taskName') }}</option>
                      <option value="projectName">{{ $t('team.taskAssignment.projectName') }}</option>
                      <option value="status">{{ $t('team.taskAssignment.status') }}</option>
                      <option value="priority">{{ $t('team.taskAssignment.priority') }}</option>
                    </select>
                    <select 
                      v-model="sortOrder" 
                      @change="handleSortChange"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="asc">↑</option>
                      <option value="desc">↓</option>
                    </select>
                  </div>
                </div>
              </template>
              <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-secondary-200">
                  <thead class="bg-secondary-50">
                    <tr>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.memberName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.userId') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.taskName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.projectName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.status') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.priority') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.progress') }}
                      </th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-secondary-200 bg-white">
                    <tr v-for="item in paginatedTaskAssignments" :key="item.id" class="hover:bg-secondary-50">
                      <td class="whitespace-nowrap px-6 py-4">
                        <div class="flex items-center">
                          <UserAvatar :name="item.userName" :seed="item.userAvatar" size="md" />
                          <div class="ml-3">
                            <div class="text-sm font-medium text-secondary-900">{{ item.userName }}</div>
                          </div>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm font-mono text-secondary-900">
                        {{ item.userId }}
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-900">
                        {{ item.taskName }}
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-900">
                        {{ item.projectName }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <Badge :variant="getStatusBadgeVariant(item.status)">
                          {{ getStatusLabel(item.status) }}
                        </Badge>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <Badge :variant="getPriorityBadgeVariant(item.priority)">
                          {{ getPriorityLabel(item.priority) }}
                        </Badge>
                      </td>
                      <td class="px-6 py-4">
                        <div class="w-32">
                          <div class="flex items-center">
                            <div class="flex-1 bg-secondary-200 rounded-full h-2">
                              <div 
                                class="bg-primary-600 h-2 rounded-full transition-all duration-300" 
                                :style="{ width: `${item.progress}%` }"
                              ></div>
                            </div>
                            <span class="ml-2 text-sm text-secondary-600">{{ item.progress }}%</span>
                          </div>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="sortedTaskAssignments.length === 0">
                      <td colspan="7" class="px-6 py-12 text-center text-sm text-secondary-500">
                        {{ $t('team.taskAssignment.noData') }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <!-- Pagination -->
              <div v-if="sortedTaskAssignments.length > 0" class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-t border-secondary-200 px-6 py-4">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-secondary-600">{{ $t('team.taskAssignment.itemsPerPage') }}</span>
                  <select 
                    v-model="itemsPerPage" 
                    @change="handleItemsPerPageChange"
                    class="rounded-lg border border-secondary-300 px-2 py-1 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                  >
                    <option :value="5">5</option>
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                  </select>
                </div>
                <div class="flex items-center gap-4">
                  <span class="text-sm text-secondary-600">
                    {{ $t('team.taskAssignment.page', { current: currentPage, total: totalPages }) }}
                  </span>
                  <div class="flex gap-2">
                    <Button 
                      variant="secondary" 
                      size="sm" 
                      :disabled="currentPage === 1"
                      @click="currentPage--"
                    >
                      {{ $t('team.taskAssignment.previous') }}
                    </Button>
                    <Button 
                      variant="secondary" 
                      size="sm" 
                      :disabled="currentPage === totalPages"
                      @click="currentPage++"
                    >
                      {{ $t('team.taskAssignment.next') }}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </template>
      </Tabs>
    </div>

    <!-- Add Member Modal 已移除:成员由 HR 同步管理 -->

    <!-- 角色变更弹窗（仅 admin 可触发） -->
    <RoleChangeDialog
      v-if="roleChangeTarget"
      v-model:visible="roleChangeDialogVisible"
      :user-id="roleChangeTarget.id"
      :current-role="roleChangeTarget.role"
      :current-managed-dept-codes="roleChangeTarget.managedDeptCodes || []"
      :current-managed-company-cd="roleChangeTarget.managedCompanyCd || ''"
      :user-company-cd="roleChangeTarget.companyCd || ''"
      :user-dept-code="roleChangeTarget.deptCode || ''"
      @success="onRoleChangeSuccess"
    />
  </MainLayout>
</template>

  <script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Tabs from '@/components/common/Tabs.vue';
import type { Tab } from '@/components/common/Tabs.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import OrgGroup from '@/components/team/OrgGroup.vue';
import RoleChangeDialog from '@/components/team/RoleChangeDialog.vue';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { useProjectStore } from '@/stores/project';
import { useOrgStore } from '@/stores/org';
import { usePermissionStore } from '@/stores/permission';
// Modal 与 ApiError 已不再使用（Add Member 表单移除）
import type { User, Task, OrgNode } from '@/types';

interface TaskAssignment {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  taskId: string;
  taskName: string;
  projectId: string;
  projectName: string;
  status: Task['status'];
  priority: Task['priority'];
  progress: number;
}

const { t } = useI18n();
const userStore = useUserStore();
const taskStore = useTaskStore();
const projectStore = useProjectStore();
const orgStore = useOrgStore();
const permissionStore = usePermissionStore();

const users = computed(() => userStore.users);
const workloadChartRef = ref<HTMLElement>();
let workloadChartInstance: echarts.ECharts | null = null;
const activeTab = ref(0);

const tabs = computed<Tab[]>(() => [
  {
    label: t('team.allMembers.title'),
    badge: users.value.length,
    value: 'members'
  },
  {
    label: t('team.taskAssignment.title'),
    badge: taskAssignments.value.length,
    value: 'tasks'
  }
]);

// (Add Member Modal 相关 state/functions 已移除:成员由 HR 同步管理,前端不再暴露手动 add/edit/delete 入口)


const adminCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'admin').length);
const pmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'project-manager').length);
const deptPmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'dept-project-manager').length);
void deptPmCount; // 暂未在模板中使用,保留供后续"按角色统计"卡
// 统计参与项目的总成员数（与 Dashboard 保持一致）
const memberCount = computed(() => {
  const projects = projectStore.projects;
  const memberSet = new Set<string>();
  projects.forEach(p => {
    memberSet.add(p.ownerId);
    if (p.memberIds) {
      p.memberIds.forEach(id => memberSet.add(id));
    }
  });
  return memberSet.size;
});

// (getRoleLabel / getRoleBadgeVariant / formattedDate removed: org
// group view uses inline role/date formatting in OrgGroup.vue)

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return;

  // 销毁旧实例
  if (workloadChartInstance) {
    workloadChartInstance.dispose();
    workloadChartInstance = null;
  }

  const chart = echarts.init(workloadChartRef.value);
  workloadChartInstance = chart;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  // 按任务数量排序，取前8名
  const userData = users.value
    .map(user => {
      const taskCount = leafTasks.filter(t => t.assigneeId === user.id).length;
      return { name: user.name, value: taskCount };
    })
    .sort((a, b) => b.value - a.value)
    .slice(0, 8)
    .filter(u => u.value > 0);

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
      data: userData.map(u => u.name),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: t('team.taskCount')
    },
    series: [
      {
        name: '任务数',
        type: 'bar',
        data: userData.map(u => u.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3b82f6' },
            { offset: 1, color: '#1d4ed8' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '60%'
      }
    ]
  };

  chart.setOption(option);
};

// 筛选
const memberSearch = ref<string>('');
const memberRoleFilter = ref<string>('');
const companyFilter = ref<string>('');

// 角色变更弹窗状态
const roleChangeDialogVisible = ref(false);
const roleChangeTarget = ref<User | null>(null);
const openRoleChangeDialog = (user: User) => {
  roleChangeTarget.value = user;
  roleChangeDialogVisible.value = true;
};
const onRoleChangeSuccess = async () => {
  // 角色变更后,后端 tokenVersion + 1 → 目标用户旧 token 失效
  // 当前 admin 自己的 users 列表也要刷新
  try {
    await userStore.refreshUsers();
  } catch (e) {
    console.error('角色变更后刷新用户列表失败', e);
  }
};

/**
 * 按顶部筛选条件过滤用户
 * 用于驱动 org 分组视图
 */
const filteredUsers = computed<User[]>(() => {
  const search = memberSearch.value.trim().toLowerCase();
  return users.value.filter(user => {
    if (search) {
      const name = (user.chineseNam || user.name || '').toLowerCase();
      const id = (user.id || '').toLowerCase();
      const email = (user.email || '').toLowerCase();
      if (!name.includes(search) && !id.includes(search) && !email.includes(search)) return false;
    }
    const normalizedRole = user.role?.replace(/_/g, '-');
    if (memberRoleFilter.value && normalizedRole !== memberRoleFilter.value) return false;
    if (companyFilter.value && user.companyCd !== companyFilter.value) return false;
    return true;
  });
});

/**
 * 顶层节点 = 4 个公司（2700 / 8400 / 2710 / 9000）
 * 按 companyFilter 过滤
 */
const displayedCompanyNodes = computed<OrgNode[]>(() => {
  if (!orgStore.tree) return [];
  const all = orgStore.tree.children || [];
  if (!companyFilter.value) return all;
  return all.filter(n => n.code === companyFilter.value);
});

/**
 * 用户所有出现过的 company_cd（驱动公司下拉框）
 */
const availableCompanyCodes = computed<string[]>(() => {
  if (!orgStore.tree) return [];
  return (orgStore.tree.children || []).map(n => n.code).filter(Boolean);
});

function companyName(cd: string): string {
  if (!orgStore.tree) return cd;
  const node = (orgStore.tree.children || []).find(n => n.code === cd);
  return node?.name || cd;
}

/**
 * 搜索模式：搜索框有内容 → 扁平结果
 * 浏览模式：搜索框为空 → org 树分组
 */
const isSearchMode = computed(() => memberSearch.value.trim().length > 0);

/**
 * 收集 org tree 中所有 code（用于判断用户的 deptCode 是否在树里）
 */
const orgCodes = computed<Set<string>>(() => {
  const set = new Set<string>();
  if (!orgStore.tree) return set;
  const walk = (n: OrgNode) => {
    set.add(n.code);
    for (const c of n.children || []) walk(c);
  };
  walk(orgStore.tree);
  return set;
});

/**
 * deptCode 为空 或 deptCode 在 org tree 里找不到的用户
 */
const unassignedUsers = computed<User[]>(() => {
  return filteredUsers.value.filter(u => !u.deptCode || !orgCodes.value.has(u.deptCode));
});

/**
 * 未分配部门折叠/分页状态
 */
const unassignedExpanded = ref(false);
const unassignedVisibleCount = ref(12);
const unassignedPageSize = 12;
const visibleUnassignedUsers = computed<User[]>(() =>
  unassignedUsers.value.slice(0, unassignedVisibleCount.value)
);

/**
 * 统计某公司节点下所有成员数（含子组织）
 */
function countCompanyMembers(root: OrgNode): number {
  const codes = new Set<string>();
  const walk = (n: OrgNode) => {
    codes.add(n.code);
    for (const c of n.children || []) walk(c);
  };
  walk(root);
  return filteredUsers.value.filter(u => u.deptCode && codes.has(u.deptCode)).length;
}

function displayName(user: User): string {
  return user.chineseNam && user.chineseNam.trim() ? user.chineseNam : user.name;
}

function roleLabel(role: string): string {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, string> = {
    'admin': '管理员',
    'dept-project-manager': '部门项目负责人',
    'project-manager': '项目经理',
    'member': '项目人员',
    'viewer': '观察者',
  };
  return map[normalized] || role;
}

function roleBadgeVariant(role: string): 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info' {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, 'danger' | 'warning' | 'primary' | 'default'> = {
    'admin': 'danger',
    'dept-project-manager': 'warning',
    'project-manager': 'warning',
    'member': 'primary',
    'viewer': 'default',
  };
  return map[normalized] || 'default';
}

// Task Assignment Table
const sortBy = ref<string>('memberName');
const sortOrder = ref<'asc' | 'desc'>('asc');
const currentPage = ref<number>(1);
const itemsPerPage = ref<number>(10);

// 计算任务分配数据
const taskAssignments = computed<TaskAssignment[]>(() => {
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));

  const assignments: TaskAssignment[] = [];
  const userMap = new Map(users.value.map(u => [u.id, u]));
  const projectMap = new Map(projectStore.projects.map(p => [p.id, p]));

  // 处理有负责人的任务
  const assignedTasks = taskStore.tasks.filter(t => t.assigneeId);

  assignedTasks.forEach(task => {
    const user = userMap.get(task.assigneeId!);
    const project = projectMap.get(task.projectId);
    if (!user || !project) return;

    // 判断是否应该显示此任务
    const shouldShow = () => {
      // 如果是叶子任务，始终显示
      if (leafTaskIds.has(task.id)) return true;

      // 如果是根任务（没有父任务）且没有叶子子任务分配给同一人，则显示
      if (!task.parentTaskId) {
        const descendants = getAllDescendantTasks(task.id);
        const hasLeafAssignedToSameUser = descendants.some(d => 
          leafTaskIds.has(d.id) && d.assigneeId === user.id
        );
        if (!hasLeafAssignedToSameUser) return true;
      }

      return false;
    };

    if (shouldShow()) {
      assignments.push({
        id: `${user.id}-${task.id}`,
        userId: user.id,
        userName: user.name,
        userAvatar: user.avatar,
        taskId: task.id,
        taskName: task.title,
        projectId: project.id,
        projectName: project.name,
        status: task.status,
        priority: task.priority,
        progress: task.progress
      });
    }
  });

  return assignments;
});

// 获取所有子孙任务
const getAllDescendantTasks = (taskId: string): Task[] => {
  const directSubtasks = taskStore.tasks.filter(t => t.parentTaskId === taskId);
  let allDescendants = [...directSubtasks];
  
  directSubtasks.forEach(subtask => {
    const subDescendants = getAllDescendantTasks(subtask.id);
    allDescendants = [...allDescendants, ...subDescendants];
  });
  
  return allDescendants;
};

// 排序后的任务分配数据
const sortedTaskAssignments = computed<TaskAssignment[]>(() => {
  const sorted = [...taskAssignments.value];
  sorted.sort((a, b) => {
    let aVal: string | number = '';
    let bVal: string | number = '';

    switch (sortBy.value) {
      case 'memberName':
        aVal = a.userName;
        bVal = b.userName;
        break;
      case 'taskName':
        aVal = a.taskName;
        bVal = b.taskName;
        break;
      case 'projectName':
        aVal = a.projectName;
        bVal = b.projectName;
        break;
      case 'status':
        aVal = a.status;
        bVal = b.status;
        break;
      case 'priority':
        const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
        aVal = priorityOrder[a.priority];
        bVal = priorityOrder[b.priority];
        break;
      default:
        aVal = a.userName;
        bVal = b.userName;
    }

    if (typeof aVal === 'string' && typeof bVal === 'string') {
      return sortOrder.value === 'asc' 
        ? aVal.localeCompare(bVal, 'zh-CN')
        : bVal.localeCompare(aVal, 'zh-CN');
    }

    return sortOrder.value === 'asc' 
      ? (aVal as number) - (bVal as number)
      : (bVal as number) - (aVal as number);
  });

  return sorted;
});

// 分页后的任务分配数据
const paginatedTaskAssignments = computed<TaskAssignment[]>(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return sortedTaskAssignments.value.slice(start, end);
});

// 总页数
const totalPages = computed(() => {
  return Math.ceil(sortedTaskAssignments.value.length / itemsPerPage.value) || 1;
});

// 处理排序变化
const handleSortChange = () => {
  currentPage.value = 1;
};

// 处理每页条数变化
const handleItemsPerPageChange = () => {
  currentPage.value = 1;
};

// 状态标签样式
const getStatusBadgeVariant = (status: Task['status']) => {
  const variants: Record<Task['status'], 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info'> = {
    'todo': 'default',
    'in-progress': 'primary',
    'done': 'success'
  };
  return variants[status] || 'default';
};

// 状态标签文本
const getStatusLabel = (status: Task['status']) => {
  const statusMap: Record<string, string> = {
    'todo': t('taskStatus.todo'),
    'in-progress': t('taskStatus.inProgress'),
    'done': t('taskStatus.done')
  };
  return statusMap[status] || status;
};

// 优先级标签样式
const getPriorityBadgeVariant = (priority: Task['priority']) => {
  const variants: Record<Task['priority'], 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info'> = {
    'low': 'default',
    'medium': 'info',
    'high': 'warning',
    'urgent': 'danger'
  };
  return variants[priority] || 'default';
};

// 优先级标签文本
const getPriorityLabel = (priority: Task['priority']) => {
  return t(`priorities.${priority}`);
};

// 监听任务、用户、项目数据变化，重置当前页
watch([() => taskStore.tasks, () => userStore.users, () => projectStore.projects], () => {
  currentPage.value = 1;
});

// 监听数据加载完成，重新渲染图表
watch(() => taskStore.loaded, async (loaded) => {
  if (loaded && userStore.users.length > 0) {
    await nextTick();
    initWorkloadChart();
  }
}, { immediate: false });

// 窗口大小变化处理
const handleResize = () => {
  if (workloadChartInstance) {
    workloadChartInstance.resize();
  }
};

onMounted(async () => {
  // 强制刷新用户数据，确保获取最新数据
  await userStore.refreshUsers();

  // 加载组织架构树
  try {
    await orgStore.loadTree();
  } catch (error) {
    console.warn('加载组织架构失败:', error);
  }

  // 强制刷新项目数据（任务分配表格需要）
  try {
    await projectStore.refreshProjects();
  } catch (error) {
    console.warn('加载项目数据失败:', error);
  }

  // 强制刷新任务数据（工作负载分布图表和任务分配表格需要）
  try {
    await taskStore.refreshTasks();
  } catch (error) {
    console.warn('加载任务数据失败:', error);
  }

  // 初始化图表
  await nextTick();
  initWorkloadChart();

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (workloadChartInstance) {
    workloadChartInstance.dispose();
    workloadChartInstance = null;
  }
});
</script>
