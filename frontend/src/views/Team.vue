<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('team.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('team.subtitle') }}</p>
        </div>
        <Button v-permission="'user:create'" variant="primary" @click="openAddMemberModal">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('team.addMember') }}
        </Button>
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
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.member') }}</p>
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
            <!-- Team Members List -->
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.allMembers') }}</h3>
              </template>
              <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-secondary-200">
                  <thead class="bg-secondary-50">
                    <tr>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.name') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.userId') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.role') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.department') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.skills') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.joinedAt') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.actions') }}
                      </th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-secondary-200 bg-white">
                    <tr v-for="user in users" :key="user.id" class="hover:bg-secondary-50">
                      <td class="whitespace-nowrap px-6 py-4">
                        <div class="flex items-center">
                          <UserAvatar :name="user.name" size="xl" />
                          <div class="ml-4">
                            <div class="text-sm font-medium text-secondary-900">{{ user.name }}</div>
                            <div class="text-sm text-secondary-600">{{ user.email }}</div>
                          </div>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm font-mono text-secondary-900">
                        {{ user.id }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <Badge :variant="getRoleBadgeVariant(user.role)">
                          {{ getRoleLabel(user.role) }}
                        </Badge>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-900">
                        {{ user.department }}
                      </td>
                      <td class="px-6 py-4">
                        <div class="flex flex-wrap gap-1">
                          <template v-if="user.skills && user.skills.length > 0">
                            <span
                              v-for="skill in user.skills.slice(0, 2)"
                              :key="skill"
                              class="rounded bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
                            >
                              {{ skill }}
                            </span>
                            <span
                              v-if="user.skills.length > 2"
                              class="rounded bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
                            >
                              +{{ user.skills.length - 2 }}
                            </span>
                          </template>
                          <span v-else class="text-xs text-secondary-400">-</span>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-600">
                        {{ formattedDate(user.joinedAt) }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm">
                        <button
                          v-if="permissionStore.canEditUser(user.id)"
                          @click="openEditModal(user)"
                          class="text-primary-600 hover:text-primary-900 mr-3"
                        >
                          {{ $t('team.edit') }}
                        </button>
                        <button
                          v-if="permissionStore.canDeleteUser()"
                          @click="handleDeleteMember(user.id)"
                          class="text-red-600 hover:text-red-900"
                        >
                          {{ $t('team.delete') }}
                        </button>
                        <span v-if="!permissionStore.canEditUser(user.id) && !permissionStore.canDeleteUser()" class="text-secondary-400">
                          -
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
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
                          <UserAvatar :name="item.userName" size="md" />
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

    <!-- Add/Edit Member Modal -->
    <Modal
      :open="showAddMemberModal"
      :title="isEditMode ? $t('team.editMember') : $t('team.addNewMember')"
      size="lg"
      @close="closeModal"
    >
      <form @submit.prevent="handleSaveMember" class="space-y-4">
        <div
          v-if="formError"
          class="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
          role="alert"
        >
          <svg class="mt-0.5 h-5 w-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
          </svg>
          <span class="flex-1">{{ formError }}</span>
          <button
            type="button"
            class="rounded p-0.5 text-red-500 transition-colors hover:bg-red-100 hover:text-red-700"
            :aria-label="$t('common.close')"
            @click="formError = ''"
          >
            <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.nameLabel') }} *</label>
          <input
            v-model="newMember.name"
            type="text"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.namePlaceholder')"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.emailLabel') }} *</label>
          <input
            v-model="newMember.email"
            type="email"
            required
            :class="[
              'w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-1',
              isEmailError
                ? 'border-red-400 bg-red-50 text-red-900 focus:border-red-500 focus:ring-red-500'
                : 'border-secondary-300 focus:border-primary-500 focus:ring-primary-500'
            ]"
            :placeholder="$t('team.form.emailPlaceholder')"
            :aria-invalid="isEmailError"
            @input="onEmailInput"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.roleLabel') }} *</label>
          <select
            v-model="newMember.role"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
          >
            <option value="">{{ $t('team.form.rolePlaceholder') }}</option>
            <option value="member">{{ $t('roles.member') }}</option>
            <option value="project-manager">{{ $t('roles.projectManager') }}</option>
            <option value="admin">{{ $t('roles.admin') }}</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.departmentLabel') }} *</label>
          <input
            v-model="newMember.department"
            type="text"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.departmentPlaceholder')"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.skillsLabel') }}</label>
          <input
            v-model="skillsInput"
            type="text"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.skillsPlaceholder')"
          />
          <p class="mt-1 text-xs text-secondary-500">{{ $t('team.form.skillsHint') }}</p>
        </div>
      </form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <Button variant="secondary" :disabled="submitting" @click="closeModal">{{ $t('common.cancel') }}</Button>
          <Button variant="primary" :loading="submitting" :disabled="submitting" @click="submitForm">
            {{ isEditMode ? $t('team.buttons.saveChanges') : $t('team.buttons.addMember') }}
          </Button>
        </div>
      </template>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import { createAvatar } from '@dicebear/core';
import { avataaars } from '@dicebear/collection';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Modal from '@/components/common/Modal.vue';
import Tabs from '@/components/common/Tabs.vue';
import type { Tab } from '@/components/common/Tabs.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { useProjectStore } from '@/stores/project';
import { usePermissionStore } from '@/stores/permission';
import { ApiError } from '@/services/api';
import type { User, Task, Project } from '@/types';
import dayjs from 'dayjs';

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
const permissionStore = usePermissionStore();

const users = computed(() => userStore.users);
const workloadChartRef = ref<HTMLElement>();
const activeTab = ref(0);

const tabs = computed<Tab[]>(() => [
  {
    label: t('team.allMembers'),
    badge: users.value.length,
    value: 'members'
  },
  {
    label: t('team.taskAssignment.title'),
    badge: taskAssignments.value.length,
    value: 'tasks'
  }
]);

// Add Member Modal
const showAddMemberModal = ref(false);
const isEditMode = ref(false);
const editingUserId = ref<string | null>(null);
const skillsInput = ref('');
const formError = ref<string>('');
const submitting = ref(false);

// 当前错误是否与邮箱相关，用于在邮箱输入框上呈现红色高亮
const isEmailError = computed(() => {
  const msg = formError.value;
  if (!msg) return false;
  return (
    msg.includes('邮箱') ||
    /email/i.test(msg) ||
    msg === t('team.messages.emailAlreadyRegistered') ||
    msg === t('team.messages.emailInvalid')
  );
});

// 用户在邮箱输入框中输入时，自动清除已有的邮箱相关错误
const onEmailInput = () => {
  if (isEmailError.value) {
    formError.value = '';
  }
};

const newMember = reactive<Omit<User, 'id' | 'avatar' | 'joinedAt'>>({
  name: '',
  email: '',
  role: 'member',
  department: '',
  skills: []
});

const resetNewMember = () => {
  newMember.name = '';
  newMember.email = '';
  newMember.role = 'member';
  newMember.department = '';
  newMember.skills = [];
  skillsInput.value = '';
};

const openAddMemberModal = () => {
  isEditMode.value = false;
  editingUserId.value = null;
  resetNewMember();
  formError.value = '';
  submitting.value = false;
  showAddMemberModal.value = true;
};

const closeModal = () => {
  showAddMemberModal.value = false;
  isEditMode.value = false;
  editingUserId.value = null;
  resetNewMember();
  formError.value = '';
  submitting.value = false;
};

const openEditModal = (user: User) => {
  isEditMode.value = true;
  editingUserId.value = user.id;
  newMember.name = user.name;
  newMember.email = user.email;
  // 标准化角色：将下划线转换为连字符，确保与表单选项匹配
  const normalizedRole = user.role?.replace(/_/g, '-') || 'member';
  newMember.role = normalizedRole as User['role'];
  newMember.department = user.department;
  // 确保 skills 是数组
  newMember.skills = Array.isArray(user.skills) ? [...user.skills] : [];
  skillsInput.value = Array.isArray(user.skills) ? user.skills.join(', ') : '';
  formError.value = '';
  submitting.value = false;
  showAddMemberModal.value = true;
};

// 将后端错误信息映射为友好提示
const mapSaveMemberError = (error: unknown): string => {
  const isApiError = error instanceof ApiError;
  const rawMessage = isApiError
    ? error.message
    : (error instanceof Error ? error.message : String(error || ''));
  const message = (rawMessage || '').trim();
  const status = isApiError ? error.status : undefined;

  // 邮箱重复（中英文关键词都覆盖）
  if (
    message.includes('邮箱已被注册') ||
    message.includes('邮箱已被使用') ||
    message.includes('邮箱已存在') ||
    /email.{0,10}(already|exists|registered|been used)/i.test(message)
  ) {
    return t('team.messages.emailAlreadyRegistered');
  }

  // 邮箱格式无效
  if (
    message.includes('邮箱格式') ||
    message.includes('合法邮箱') ||
    /invalid email/i.test(message)
  ) {
    return t('team.messages.emailInvalid');
  }

  // 用户不存在
  if (message.includes('用户不存在') || /user not found/i.test(message)) {
    return t('team.messages.userNotFound');
  }

  // 网络异常
  if (
    message.includes('Failed to fetch') ||
    message.includes('NetworkError') ||
    message.includes('Network request failed') ||
    message.includes('网络')
  ) {
    return t('team.messages.networkError');
  }

  // 仅当后端没有返回可读消息时，再根据状态码做兜底
  const isGenericHttpError = /^HTTP error! status:\s*\d+$/.test(message) || message === '';
  if (isGenericHttpError) {
    if (status === 400) {
      // 400 但没有可读消息：通常是参数错误或唯一性冲突，按最常见情况提示
      return t('team.messages.emailAlreadyRegistered');
    }
    if (status === 401 || status === 403) {
      return t('team.messages.userNotFound');
    }
    if (typeof status === 'number' && status >= 500) {
      return t('team.messages.serverError');
    }
    return t('team.messages.operationFailed');
  }

  // 如果后端返回了中文消息，直接使用
  if (/[\u4e00-\u9fa5]/.test(message)) {
    return message;
  }

  return t('team.messages.operationFailed');
};

const handleSaveMember = async () => {
  // 重置错误信息
  formError.value = '';

  // 验证必填字段
  if (!newMember.name || !newMember.email || !newMember.department) {
    formError.value = t('team.messages.requiredFields');
    return;
  }

  // 验证角色已选择
  if (!newMember.role) {
    formError.value = t('team.form.rolePlaceholder');
    return;
  }

  // 防止重复提交
  if (submitting.value) return;
  submitting.value = true;

  // Parse skills from comma-separated input
  const skillsArray = skillsInput.value
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0);

  try {
    if (isEditMode.value && editingUserId.value) {
      // Update existing user
      await userStore.updateUser(editingUserId.value, {
        name: newMember.name,
        email: newMember.email,
        role: newMember.role,
        department: newMember.department,
        skills: skillsArray
      });
      alert(t('team.messages.updateSuccess'));
    } else {
      // Add new user
      const avatar = createAvatar(avataaars, { seed: newMember.name }).toDataUri();
      await userStore.addUser({
        ...newMember,
        skills: skillsArray,
        avatar,
        joinedAt: new Date().toISOString()
      });
      alert(t('team.messages.createSuccess'));
    }

    // Close modal and reset form
    closeModal();

    // Refresh chart after a short delay
    setTimeout(() => {
      initWorkloadChart();
    }, 200);
  } catch (error) {
    console.error('Failed to save member:', error);
    formError.value = mapSaveMemberError(error);
  } finally {
    submitting.value = false;
  }
};

// 提交表单方法（通过按钮点击触发）
const submitForm = () => {
  handleSaveMember();
};

const handleDeleteMember = async (userId: string) => {
  const user = users.value.find(u => u.id === userId);
  if (!user) return;

  if (confirm(t('team.messages.deleteConfirm', { name: user.name }))) {
    try {
      await userStore.deleteUser(userId);
      alert(t('team.messages.deleteSuccess'));

      // Refresh chart after a short delay
      setTimeout(() => {
        initWorkloadChart();
      }, 200);
    } catch (error) {
      console.error('Failed to delete member:', error);
      alert(t('team.messages.deleteFailed'));
    }
  }
};

const adminCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'admin').length);
const pmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'project-manager').length);
const memberCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'member').length);

const getRoleLabel = (role: string) => {
  // 标准化角色名称：处理下划线和连字符的兼容性
  const normalizedRole = role?.replace(/_/g, '-');

  const labels: Record<string, string> = {
    admin: 'roles.admin',
    'project-manager': 'roles.projectManager',
    member: 'roles.member'
  };
  const key = labels[normalizedRole];
  return key ? t(key) : role;
};

const getRoleBadgeVariant = (role: string) => {
  // 标准化角色名称：处理下划线和连字符的兼容性
  const normalizedRole = role?.replace(/_/g, '-');

  const variants: Record<string, 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info'> = {
    admin: 'danger',
    'project-manager': 'warning',
    member: 'primary'
  };
  return variants[normalizedRole] || 'default';
};

const formattedDate = (date: string) => {
  // Use localized date format based on current locale
  const locale = t('common.locale') || 'zh';
  if (locale === 'ko') {
    return dayjs(date).format('YYYY년 MM월');
  } else {
    return dayjs(date).format('YYYY年MM月');
  }
};

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return;

  const chart = echarts.init(workloadChartRef.value);

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  const userData = users.value.slice(0, 8).map(user => {
    const taskCount = leafTasks.filter(t => t.assigneeId === user.id).length;
    return {
      name: user.name,
      value: taskCount
    };
  });

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

onMounted(async () => {
  // 确保用户数据已加载
  await userStore.loadUsers();

  // 确保项目数据已加载（任务分配表格需要）
  try {
    await projectStore.loadProjects();
  } catch (error) {
    console.warn('加载项目数据失败:', error);
  }

  // 确保任务数据已加载（工作负载分布图表和任务分配表格需要）
  try {
    await taskStore.loadTasks();
  } catch (error) {
    console.warn('加载任务数据失败:', error);
  }

  // 初始化图表
  setTimeout(() => {
    initWorkloadChart();
  }, 100);
});
</script>
